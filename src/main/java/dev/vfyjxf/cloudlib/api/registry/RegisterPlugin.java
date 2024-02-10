package dev.vfyjxf.cloudlib.api.registry;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Mark a class as a plugin.
 * must have a no-arg constructor.
 */
@Retention(RetentionPolicy.CLASS)
public @interface RegisterPlugin {
}
