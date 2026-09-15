package dev.vfyjxf.cloudlib.gradle.lang;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangKeysClassWriterTest {

    private static final String ENTRY = "dev.vfyjxf.cloudlib.data.lang.LangEntry";
    private static final String ANNOTATION = "javax.annotation.processing.Generated";

    @Test
    void generatesNestedClassesWithEntryConstants() {
        Map<String, String> entries = new TreeMap<>();
        entries.put("cloudlib.keys.debug", "Debug");
        entries.put("cloudlib.keys.rebuild_ui", "Rebuild UI");
        entries.put("cloudlib.ui.title", "Title");
        entries.put("item.cloudlib.test_block", "Test Block");

        String source = LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, ANNOTATION, "cloudlib", entries, Map.of(), LangNameStyle.UPPER_SNAKE);

        assertTrue(source.contains("package com.example.lang;"));
        assertTrue(source.contains("import javax.annotation.processing.Generated;"));
        assertTrue(source.contains("import dev.vfyjxf.cloudlib.data.lang.LangEntry;"));
        assertTrue(source.contains("@Generated(\"cloudLang gradle plugin\")"));
        assertTrue(source.contains("public final class ModLang {"));
        assertTrue(source.contains("public static final class Keys {"));
        assertTrue(source.contains("public static final LangEntry DEBUG = LangEntry.of(\"cloudlib.keys.debug\");"));
        assertTrue(source.contains("public static final LangEntry REBUILD_UI = LangEntry.of(\"cloudlib.keys.rebuild_ui\");"));
        assertTrue(source.contains("public static final class Ui {"));
        assertTrue(source.contains("/** Debug */"));
        //non mod keys are packed into json but never referenced from the class
        assertFalse(source.contains("test_block"));
        assertFalse(source.contains("TEST_BLOCK"));
    }

    @Test
    void annotationCanBeDisabled() {
        String source = LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", Map.of(), Map.of(), LangNameStyle.CAMEL);
        assertFalse(source.contains("@Generated"));
        assertFalse(source.contains("import javax.annotation.processing.Generated;"));
    }

    @Test
    void supportsCamelPascalAndUpperSnakeConstantStyles() {
        Map<String, String> entries = new TreeMap<>();
        entries.put("cloudlib.keys.debug", "Debug");
        entries.put("cloudlib.keys.rebuild_ui", "Rebuild UI");

        String camel = LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of(), LangNameStyle.CAMEL);
        assertTrue(camel.contains("public static final LangEntry debug = LangEntry.of(\"cloudlib.keys.debug\");"));
        assertTrue(camel.contains("public static final LangEntry rebuildUi = LangEntry.of(\"cloudlib.keys.rebuild_ui\");"));

        String pascal = LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of(), LangNameStyle.PASCAL);
        assertTrue(pascal.contains("public static final LangEntry Debug = LangEntry.of(\"cloudlib.keys.debug\");"));
        assertTrue(pascal.contains("public static final LangEntry RebuildUi = LangEntry.of(\"cloudlib.keys.rebuild_ui\");"));

        String upperSnake = LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of(), LangNameStyle.UPPER_SNAKE);
        assertTrue(upperSnake.contains("public static final LangEntry REBUILD_UI = LangEntry.of(\"cloudlib.keys.rebuild_ui\");"));
    }

    @Test
    void explicitFieldNamesOverrideTheStyle() {
        Map<String, String> entries = new TreeMap<>();
        entries.put("cloudlib.keys.debug", "Debug");
        entries.put("cloudlib.keys.rebuild_ui", "Rebuild UI");
        Map<String, String> fieldNames = Map.of("cloudlib.keys.rebuild_ui", "rebuildUIButton");

        String source = LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, fieldNames, LangNameStyle.UPPER_SNAKE);

        assertTrue(source.contains("public static final LangEntry rebuildUIButton = LangEntry.of(\"cloudlib.keys.rebuild_ui\");"));
        assertTrue(source.contains("public static final LangEntry DEBUG = LangEntry.of(\"cloudlib.keys.debug\");"));
    }

    @Test
    void rejectsInvalidExplicitFieldNames() {
        Map<String, String> entries = Map.of("cloudlib.keys.debug", "Debug");

        assertThrows(IllegalStateException.class, () ->
                LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of("cloudlib.keys.debug", "not a name"), LangNameStyle.CAMEL));
        assertThrows(IllegalStateException.class, () ->
                LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of("cloudlib.keys.debug", "class"), LangNameStyle.CAMEL));
    }

    @Test
    void escapesKeywordsAndLeadingDigits() {
        assertEquals("class_", LangNameStyle.CAMEL.fieldName("class"));
        assertEquals("_2fa", LangNameStyle.CAMEL.fieldName("2fa"));
        assertEquals("rebuildUI", LangNameStyle.CAMEL.fieldName("rebuildUI"));
    }

    @Test
    void rejectsCollidingConstantNames() {
        Map<String, String> entries = new TreeMap<>();
        entries.put("cloudlib.keys.rebuild_ui", "Rebuild UI");
        entries.put("cloudlib.keys.rebuildUi", "Rebuild UI");

        //case-insensitively equal segments map to the same name in every style
        assertThrows(IllegalStateException.class, () ->
                LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of(), LangNameStyle.CAMEL));
        assertThrows(IllegalStateException.class, () ->
                LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of(), LangNameStyle.UPPER_SNAKE));
    }

    @Test
    void rejectsExplicitNameCollidingWithDerivedName() {
        Map<String, String> entries = new TreeMap<>();
        entries.put("cloudlib.keys.debug", "Debug");
        entries.put("cloudlib.keys.rebuild_ui", "Rebuild UI");

        assertThrows(IllegalStateException.class, () ->
                LangKeysClassWriter.write("com.example.lang", "ModLang", ENTRY, "", "cloudlib", entries, Map.of("cloudlib.keys.rebuild_ui", "debug"), LangNameStyle.CAMEL));
    }

    @Test
    void parseRejectsUnknownStyle() {
        assertThrows(IllegalArgumentException.class, () -> LangNameStyle.parse("nope"));
        assertThrows(IllegalArgumentException.class, () -> LangNameStyle.parse(null));
    }

    @Test
    void skipsImportWhenSharingPackageWithEntryClass() {
        String source = LangKeysClassWriter.write("dev.vfyjxf.cloudlib.data.lang", "CloudLang", ENTRY, "", "cloudlib", Map.of(), Map.of(), LangNameStyle.CAMEL);
        assertFalse(source.contains("import dev.vfyjxf.cloudlib.data.lang.LangEntry;"));
        assertTrue(source.contains("public final class CloudLang {"));
    }

}
