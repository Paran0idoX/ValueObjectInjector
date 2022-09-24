package com.para.valueobjectinjector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectInfo {
    String modelFieldName() default "";
    int injectValueId() default InjectValue.DEFAULT_VALUE_ID;
    boolean defaultNull() default true;
}
