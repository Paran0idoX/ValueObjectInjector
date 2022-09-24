package com.para.valueobjectinjector;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FieldConnectionCache {
    private static final ConcurrentHashMap<Class<? extends ValueObjectInjector<? extends Model>>, List<FieldConnection>> fieldConnectionMap = new ConcurrentHashMap<>();

    public static <I extends ValueObjectInjector<M>, M extends Model> void put(Class<I> clazz, List<FieldConnection> fieldConnectionList){
        fieldConnectionMap.put(clazz, fieldConnectionList);
    }

    public static boolean alreadyCached(Class<? extends ValueObjectInjector> clazz){
        return fieldConnectionMap.contains(clazz);
    }

    public static List<FieldConnection> get(Class<? extends ValueObjectInjector> clazz){
        return fieldConnectionMap.get(clazz);
    }
}
