package dev.vfyjxf.cloudlib.api.data.serialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD
)
@Retention(RetentionPolicy.RUNTIME)
public @interface Save {
    /**
     * @return the serialized name of the field,default is the field name
     */
    String value() default "";
}
