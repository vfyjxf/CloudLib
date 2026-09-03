package dev.vfyjxf.cloudlib.gradle.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangMergerTest {

    @TempDir
    Path tempDir;

    @Test
    void flattensNestedAndFlatKeys() {
        List<String> warnings = new ArrayList<>();
        var flattened = LangMerger.flatten(Map.of(
                "cloudlib", Map.of("keys", Map.of("debug", "Debug", "rebuild_ui", "Rebuild UI")),
                "item.cloudlib.test_block", "Test Block"
        ), warnings::add);

        assertEquals(Map.of(
                "cloudlib.keys.debug", "Debug",
                "cloudlib.keys.rebuild_ui", "Rebuild UI",
                "item.cloudlib.test_block", "Test Block"
        ), flattened.entries());
        assertTrue(flattened.fieldNames().isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void annotatedLeafCarriesExplicitFieldName() {
        List<String> warnings = new ArrayList<>();
        var flattened = LangMerger.flatten(Map.of(
                "cloudlib", Map.of("keys", Map.of("rebuild_ui", Map.of("value", "Rebuild UI", "$field", "rebuildUIButton")))
        ), warnings::add);

        assertEquals(Map.of("cloudlib.keys.rebuild_ui", "Rebuild UI"), flattened.entries());
        assertEquals(Map.of("cloudlib.keys.rebuild_ui", "rebuildUIButton"), flattened.fieldNames());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void plainFieldKeyIsARegularTranslationKey() {
        List<String> warnings = new ArrayList<>();
        var flattened = LangMerger.flatten(Map.of(
                "cloudlib", Map.of("settings", Map.of("field", "Some Field", "value", "Some Value"))
        ), warnings::add);

        //no $field token: 'field' and 'value' are ordinary nested keys
        assertEquals(Map.of(
                "cloudlib.settings.field", "Some Field",
                "cloudlib.settings.value", "Some Value"
        ), flattened.entries());
        assertTrue(flattened.fieldNames().isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void annotatedLeafRejectsMissingValueAndExtraSiblings() {
        List<String> warnings = new ArrayList<>();
        var flattened = LangMerger.flatten(Map.of(
                "a", Map.of("$field", "name"),
                "b", Map.of("value", "B", "$field", "bName", "extra", "ignored"),
                "c", Map.of("value", "C", "$field", Map.of("nested", "map"))
        ), warnings::add);

        assertTrue(flattened.entries().isEmpty());
        assertTrue(flattened.fieldNames().isEmpty());
        assertEquals(3, warnings.size());
    }

    @Test
    void valueAloneIsRegularNestingNotAnAnnotatedLeaf() {
        List<String> warnings = new ArrayList<>();
        var flattened = LangMerger.flatten(Map.of(
                "cloudlib", Map.of("test", Map.of("value", "X"))
        ), warnings::add);

        assertEquals(Map.of("cloudlib.test.value", "X"), flattened.entries());
        assertTrue(flattened.fieldNames().isEmpty());
    }

    @Test
    void mergesLocalesAcrossRootsWithGeneratedWinning() throws IOException {
        Path main = tempDir.resolve("main");
        Path generated = tempDir.resolve("generated");
        write(main, "en_us/ui.yaml", "cloudlib:\n  ui:\n    title: Old Title\n    close: Close\n");
        write(generated, "en_us/ui.yaml", "cloudlib:\n  ui:\n    title: New Title\n");
        write(generated, "zh_cn/ui.yaml", "cloudlib:\n  ui:\n    title: 标题\n");

        List<String> warnings = new ArrayList<>();
        var locales = LangMerger.merge(List.of(
                new LangMerger.SourceRoot(main, "src/main/lang"),
                new LangMerger.SourceRoot(generated, "build/generated/datagen/lang")
        ), warnings::add);

        assertEquals(Map.of("cloudlib.ui.title", "New Title", "cloudlib.ui.close", "Close"), locales.get("en_us").mergedEntries());
        assertEquals(Map.of("cloudlib.ui.title", "标题"), locales.get("zh_cn").mergedEntries());
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("cloudlib.ui.title"));

        //each source yaml file is one chunk, carrying its source description
        var chunk = locales.get("en_us").chunks().get("ui");
        assertEquals(2, chunk.entries().size());
        assertEquals(List.of("src/main/lang/en_us/ui.yaml", "build/generated/datagen/lang/en_us/ui.yaml"), chunk.sources());
    }

    @Test
    void skipsSequencesAndNullValuesWithWarnings() {
        List<String> warnings = new ArrayList<>();
        var flattened = LangMerger.flatten(Map.of(
                "list", List.of("a", "b"),
                "nothing", Map.of()
        ), warnings::add);

        assertTrue(flattened.entries().isEmpty());
        assertEquals(1, warnings.size());
    }

    private static void write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

}
