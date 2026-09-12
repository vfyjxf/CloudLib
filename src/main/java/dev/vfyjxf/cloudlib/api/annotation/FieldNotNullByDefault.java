package dev.vfyjxf.cloudlib.api.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierDefault;

@Documented
@NotNull
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifierDefault({ElementType.FIELD})
public @interface FieldNotNullByDefault {}
