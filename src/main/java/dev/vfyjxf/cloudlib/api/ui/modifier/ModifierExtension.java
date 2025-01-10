package dev.vfyjxf.cloudlib.api.ui.modifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO:make modifier like android R file
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModifierExtension {
    Class<? extends Modifier> value();
}
