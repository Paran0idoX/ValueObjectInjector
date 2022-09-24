package com.para.valueobjectinjector;

import java.lang.reflect.Constructor;

public interface ValueObjectInjector<T extends Model> {
    default void injectFromModel(T model){
        ValueObjectInjectorUtil.getFieldConnectionList(this.getClass(), model.getClass()).forEach(fieldConnection -> fieldConnection.inject(this, model));
    }

    default T convertToModel(Class<T> modelClass){
        T model = null;
        try {
            Constructor<T> constructor = modelClass.getConstructor();
            model = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        T finalModel = model;
        ValueObjectInjectorUtil.getFieldConnectionList(this.getClass(), modelClass).forEach(fieldConnection -> fieldConnection.reverseInject(this, finalModel));

        return finalModel;
    }
}
