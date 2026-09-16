package dev.vfyjxf.cloudlib.gradle.style;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FqnLintTest {

    private static String source(String body) {
        return "package com.example;\n\nclass Thing {\n" + body + "\n}\n";
    }

    @Test
    void flagsQualifiedTypeReference() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> FqnLint.check(source("    private java.util.Map<String, Integer> m;")));
        assertTrue(e.getMessage().contains("java.util.Map"));
        assertTrue(e.getMessage().contains("line 4"));
    }

    @Test
    void flagsRedundantQualificationOfImportedType() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> FqnLint.check("package com.example;\n\nimport java.util.Map;\n\nclass Thing {\n"
                        + "    private java.util.Map<String, Integer> m;\n}\n"));
        assertTrue(e.getMessage().contains("java.util.Map"));
    }

    @Test
    void allowsQualificationForcedByImportCollision() {
        // java.awt.List imported; java.util.List must stay qualified
        assertDoesNotThrow(() -> FqnLint.check("package com.example;\n\nimport java.awt.List;\n\nclass Thing {\n"
                + "    java.util.List<String> a;\n    List<String> b;\n}\n"));
    }

    @Test
    void allowsQualificationAgainstDeclaredType() {
        assertDoesNotThrow(() -> FqnLint.check("package com.example;\n\nclass Timer {\n"
                + "    java.util.Timer other;\n}\n"));
    }

    @Test
    void allowsNestedTypeViaOuterCollision() {
        // another 'Map' is imported — java.util.Map.Entry must stay qualified
        assertDoesNotThrow(() -> FqnLint.check("package com.example;\n\nimport other.pkg.Map;\n\nclass Thing {\n"
                + "    java.util.Map.Entry<String, Integer> e;\n}\n"));
    }

    @Test
    void ignoresLiteralsAndComments() {
        assertDoesNotThrow(() -> FqnLint.check(source("    String s = \"java.util.Map\";\n"
                + "    // java.util.List is mentioned here\n"
                + "    /* java.util.Set in a block comment */\n"
                + "    String t = \"\"\"\n"
                + "        java.util.HashMap in a text block\n"
                + "        \"\"\";\n"
                + "    char c = 'x';")));
    }

    @Test
    void ignoresImportAndPackageLines() {
        assertDoesNotThrow(() -> FqnLint.check("package com.example.sub;\n\n"
                + "import java.util.Map;\nimport static java.util.Collections.emptyList;\n\n"
                + "class Thing {\n    Map<String, Integer> m;\n}\n"));
    }

    @Test
    void ignoresMemberAccessChains() {
        assertDoesNotThrow(() -> FqnLint.check(source("    Object o = this.inner.Widget.create();\n"
                + "    Object p = super.inner.Widget.create();")));
    }

    @Test
    void skipsPackageInfo() {
        assertDoesNotThrow(() -> FqnLint.check("/** docs with {@link java.util.Map} */\n"
                + "@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault\n"
                + "package com.example;\n"));
    }

    @Test
    void suppressionMarker() {
        assertDoesNotThrow(() -> FqnLint.check(
                source("    private java.util.Map<String, Integer> m; // fqn-ok: field name chain")));
    }

    @Test
    void reportsLineNumbers() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> FqnLint.check(source("\n    java.util.Map<String, Integer> m;")));
        assertTrue(e.getMessage().contains("line 5"), e.getMessage());
    }

    @Test
    void nestedTypeReportsOuterImport() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> FqnLint.check(source("    java.util.Map.Entry<String, Integer> e;")));
        assertTrue(e.getMessage().contains("java.util.Map.Entry"));
        assertTrue(e.getMessage().contains("Map.Entry"));
    }
}
