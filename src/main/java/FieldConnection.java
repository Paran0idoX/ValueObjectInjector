import annotation.InjectInfo;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class FieldConnection<I extends ValueObjectInjector<M>, M extends Model> {
    private List<Field> injectFieldList;
    private Field modelField;
    private boolean directInject;
    private Class<M> modelClass;
    private Class<I> injectorClass;
    private BiMap<Field, Field> modelToInjectorFieldMap;
    private Lock biMapInverseLock;

    public void inject(ValueObjectInjector<Model> injector, Model model){
        if (injector == null || model == null){
            return;
        }
        injectFieldList.forEach(field -> field.setAccessible(true));
        if (directInject){
            ValueObjectInjectorUtil.valueInject(injectFieldList.get(0), modelField, injector, model);
        } else {
            modelField.setAccessible(true);
            try {
                Object modelFieldValue = modelField.get(model);
                if (modelFieldValue == null){
                    return;
                }
                biMapInverseLock.lock();
                for (Map.Entry<Field, Field> entry : modelToInjectorFieldMap.entrySet()) {
                    entry.getValue().set(injector, entry.getKey().get(modelFieldValue));
                }
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            } finally {
                biMapInverseLock.unlock();
            }
        }
    }

    public void reverseInject(ValueObjectInjector<? extends Model> injector, Model model){
        if (injector == null || model == null){
            return;
        }
        injectFieldList.forEach(field -> field.setAccessible(true));
        if (directInject){
            ValueObjectInjectorUtil.valueInject(modelField, injectFieldList.get(0), model, injector);
        } else {
            try {
                biMapInverseLock.lock();
                modelToInjectorFieldMap.inverse();
                if (modelField.isEnumConstant()){
                    try {
                        Enum enumValue = ValueObjectInjectorUtil.getEnumObjectByValue((Class<? extends Enum>) modelField.getDeclaringClass(), injectFieldList.get(0).get(injector));
                        modelField.set(model, enumValue);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    Constructor<?> constructor = modelField.getDeclaringClass().getConstructor();
                    constructor.setAccessible(true);
                    try {
                        Object valueObject = constructor.newInstance();
                        for (Map.Entry<Field, Field> entry : modelToInjectorFieldMap.entrySet()) {
                            entry.getKey().setAccessible(true);
                            entry.getValue().set(valueObject, entry.getKey().get(injector));
                        }
                        modelField.set(model, valueObject);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            } catch (NoSuchMethodException e) {
                log.error("Class {} has no NoArgsConstructor", modelField.getDeclaringClass(), e);
            } finally {
                modelToInjectorFieldMap.inverse();
                biMapInverseLock.unlock();
            }
        }
    }

    public FieldConnection(List<Field> injectFieldList, Field modelField, boolean directInject, Class<M> modelClass, Class<I> injectorClass) throws NoSuchFieldException {
        this.injectFieldList = injectFieldList;
        this.modelField = modelField;
        this.directInject = directInject;
        this.modelClass = modelClass;
        this.injectorClass = injectorClass;
        if (!directInject){
            modelToInjectorFieldMap = HashBiMap.create(injectFieldList.size());
            biMapInverseLock = new ReentrantLock();
            for (Field field : injectFieldList) {
                Field valueObjectField = ValueObjectInjectorUtil.getInjectValueProviderById(modelField.getDeclaringClass(), field.getAnnotation(InjectInfo.class).injectValueId());
                modelToInjectorFieldMap.put(valueObjectField, field);
            }
        }
    }
}
