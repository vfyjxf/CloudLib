package dev.vfyjxf.cloudlib.data.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface for language providers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LangProvider {

    /**
     * @return the modid of the mod that provides the language keys.
     */
    String value() default "";

}
