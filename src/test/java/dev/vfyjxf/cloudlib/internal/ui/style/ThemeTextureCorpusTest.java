package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.StyleRule;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The texture vocabulary against the shipped corpus: every texture-shaped
 * declaration in every bundled theme must parse. A theme that names an unknown
 * function, or passes arguments a function cannot read, fails here instead of
 * silently drawing nothing.
 */
class ThemeTextureCorpusTest {

    /** The function names the vocabulary owns — a declaration starting with one is a texture. */
    private static final Set<String> functions = Set.of(
        "nine-slice",
        "sprite",
        "tiled",
        "color",
        "linear-gradient",
        "border-texture",
        "sdf",
        "rect",
        "group",
        "built-in",
        "empty",
        "none"
    );

    private static List<Path> themeSheets() throws Exception {
        Path themes = Path.of(ThemeTextureCorpusTest.class.getResource("/assets/cloudlib/ui/themes").toURI());
        try (Stream<Path> walk = Files.walk(themes)) {
            return walk.filter(path -> path.toString().endsWith(".css")).toList();
        }
    }

    private static List<StyleRule> rules(Stylesheet sheet) {
        List<StyleRule> out = new ArrayList<>();
        for (var rule : sheet.rules()) {
            if (rule instanceof StyleRule styleRule) {
                out.add(styleRule);
            }
        }
        return out;
    }

    private static @Nullable String functionOf(List<ComponentValue> values) {
        for (ComponentValue value : values) {
            if (value == ComponentValue.Whitespace.instance) {
                continue;
            }
            return value instanceof ComponentValue.Function fn ? fn.name().toLowerCase(Locale.ROOT) : null;
        }
        return null;
    }

    @Test
    void everyBundledTextureDeclarationParses() throws IOException {
        List<String> failures = new ArrayList<>();
        int declarations = 0;
        List<Path> sheets;
        try {
            sheets = themeSheets();
        } catch (Exception e) {
            throw new IOException(e);
        }
        for (Path sheet : sheets) {
            Stylesheet stylesheet = CssParser.parse(Files.readString(sheet));
            for (StyleRule rule : rules(stylesheet)) {
                for (var declaration : rule.declarations()) {
                    String name = functionOf(declaration.value());
                    if (name == null || !functions.contains(name)) {
                        continue;
                    }
                    declarations++;
                    VisualTexture texture = CssTextures.parse(declaration.value());
                    if (texture == null) {
                        failures.add(
                            sheet.getFileName() + " " + declaration.property() + ": "
                                    + Tokens.serialize(declaration.value())
                        );
                    }
                }
            }
        }
        assertEquals(List.of(), failures);
        assertTrue(declarations > 50, "the corpus has shrunk: " + declarations + " texture declarations");
    }
}
