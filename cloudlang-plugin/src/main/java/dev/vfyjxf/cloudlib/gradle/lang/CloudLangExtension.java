package dev.vfyjxf.cloudlib.gradle.lang;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for the distributed language system.
 *
 * <pre>{@code
 * cloudLang {
 *     modId = 'mymod'
 *     packageName = 'com.example.mymod.lang'
 *     className = 'ModLang'
 *     constantNameStyle = 'camel'
 * }
 * }</pre>
 */
public abstract class CloudLangExtension {

    /**
     * The mod id, used as the translation key prefix and the asset namespace of the
     * packed json files ({@code assets/<modId>/lang/<locale>.json}).
     */
    public abstract Property<String> getModId();

    /**
     * Package of the generated key class.
     */
    public abstract Property<String> getPackageName();

    /**
     * Simple name of the generated key class (the "R-like" class). Defaults to {@code LangKeys}.
     */
    public abstract Property<String> getClassName();

    /**
     * Naming style of the generated constants: {@code camel}, {@code pascal} or
     * {@code upper_snake}. Defaults to {@code upper_snake} (the classic "R class" look).
     * Individual entries can override this with an explicit {@code $field:} name in the
     * yaml files.
     */
    public abstract Property<String> getConstantNameStyle();

    /**
     * Fully qualified annotation put on the generated class to mark it as generated.
     * Defaults to {@code javax.annotation.processing.Generated}; set to an empty string
     * to disable.
     */
    public abstract Property<String> getGeneratedAnnotation();

    /**
     * Fully qualified name of the entry type referenced by the generated key class.
     * Defaults to {@code dev.vfyjxf.cloudlib.data.lang.LangEntry}.
     */
    public abstract Property<String> getEntryClass();

    /**
     * Locale whose entries drive the generated key class. Defaults to {@code en_us}.
     */
    public abstract Property<String> getDefaultLocale();

    /**
     * Root of the hand-written distributed yaml files ({@code <dir>/<locale>/**.yaml}).
     * Defaults to {@code src/main/lang}.
     */
    public abstract DirectoryProperty getSourceDirectory();

    /**
     * Root of the datagen-produced distributed yaml files, merged over the hand-written ones.
     * Defaults to {@code build/generated/datagen/lang}, the sibling of the standard ModDev
     * data run output folder ({@code build/generated/datagen/resources}).
     */
    public abstract DirectoryProperty getGeneratedSourceDirectory();

}
