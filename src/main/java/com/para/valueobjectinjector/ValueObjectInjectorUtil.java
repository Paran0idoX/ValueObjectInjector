package com.para.valueobjectinjector;

import com.para.valueobjectinjector.annotation.InjectValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.para.valueobjectinjector.annotation.InjectIgnore;
import com.para.valueobjectinjector.annotation.InjectInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ValueObjectInjectorUtil {

    public static Field getInjectValueProviderById(Class valueObjectClass, int injectValueId) throws NoSuchFieldException {
        Field[] declaredFields = FieldUtils.getFieldsWithAnnotation(valueObjectClass, InjectValue.class);
        Method[] declaredMethods = MethodUtils.getMethodsWithAnnotation(valueObjectClass, InjectValue.class, true, true);
        for (Field declaredField : declaredFields) {
            int modelInjectValueId = declaredField.getAnnotation(InjectValue.class).value();
            if (modelInjectValueId == injectValueId){
                return declaredField;
            }
        }
        throw new NoSuchFieldException("modelFieldValueObject doesn't have such injectValue");
    }

    public static void valueInject(Field injectedField, Field valueField, Object injectedObject, Object valueObject){
        injectedField.setAccessible(true);
        valueField.setAccessible(true);
        try {
            Object value = injectedField.get(valueObject);
            valueField.set(injectedObject, value);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
        }
    }

    private static boolean directInject(Field modelField, List<Field> fieldList){
        return fieldList.size() == 1 && modelField.getDeclaringClass().equals(fieldList.get(0).getDeclaringClass());
    }

    public static List<FieldConnection> getFieldConnectionList(Class<? extends ValueObjectInjector> injectorClass, Class<? extends Model> modelClass){
        List<FieldConnection> fieldConnectionList;
        if (FieldConnectionCache.alreadyCached(injectorClass)){
            fieldConnectionList = FieldConnectionCache.get(injectorClass);
        } else {
            List<Field> valueObjectInjectFieldList = FieldUtils.getAllFieldsList(injectorClass).stream().filter(field -> !AnnotatedElementUtils.isAnnotated(field, InjectIgnore.class)).collect(Collectors.toList());
            Map<Field, List<Field>> fieldMap = Maps.newHashMapWithExpectedSize(valueObjectInjectFieldList.size());
            for (Field field : valueObjectInjectFieldList) {
                //Model中需注入的Field名称
                String modelFieldName = AnnotatedElementUtils.isAnnotated(field, InjectInfo.class) ?
                        field.getName() : field.getAnnotation(InjectInfo.class).modelFieldName();
                Field modelField = null;
                try {
                    modelField = modelClass.getDeclaredField(modelFieldName);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                if (fieldMap.containsKey(modelField)){
                    fieldMap.get(modelField).add(field);
                } else {
                    fieldMap.put(modelField, Lists.newArrayList(field));
                }
            }
            fieldConnectionList = new ArrayList<>();
            for (Map.Entry<Field, List<Field>> entry : fieldMap.entrySet()) {
                Field modelField = entry.getKey();
                List<Field> fieldList = entry.getValue();
                try {
                    fieldConnectionList.add(new FieldConnection(fieldList, modelField, directInject(modelField, fieldList), modelClass, injectorClass));
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
            FieldConnectionCache.put(injectorClass, fieldConnectionList);
        }
        return fieldConnectionList;
    }

    public static Enum getEnumObjectByValue(Class<? extends Enum> enumClass, Object value) throws NoSuchFieldException, IllegalAccessException {
        Enum[] enumConstants = enumClass.getEnumConstants();
        for (Enum enumConstant : enumConstants) {
            Field valueField = getInjectValueProviderById(enumClass, InjectValue.DEFAULT_VALUE_ID);
            valueField.setAccessible(true);
            if (valueField.get(enumConstant).equals(value)){
                return enumConstant;
            }
        }
        return null;
    }
}
