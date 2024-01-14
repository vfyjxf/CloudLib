package dev.vfyjxf.cloudlib.api.ui.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface UIWidget {

    /**
     * @return the identifier of this type of widget.
     */
    String value();
}
