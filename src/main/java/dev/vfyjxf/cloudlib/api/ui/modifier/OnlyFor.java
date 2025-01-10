package dev.vfyjxf.cloudlib.api.ui.modifier;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnlyFor {

    Class<? extends Widget> value() default Widget.class;

}
