package com.para.valueobjectinjector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectValue {
    int DEFAULT_VALUE_ID = -1;

    int value() default DEFAULT_VALUE_ID;
}
