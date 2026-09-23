package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.render.RenderOptions;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RichTextThemeColorTest {

    private static final StyleVar<Integer> accent = StyleVar.color("--accent");
    private static final StyleVar<Integer> missing = StyleVar.color("--missing");
    private static final int fallback = 0xFF123456;

    /** A theme that binds {@code --accent} only. */
    private static ThemeColorResolver theme(int argb) {
        return slot -> slot == accent ? argb : null;
    }

    @Test
    void resolvedSlotWinsAndKeepsItsAlpha() {
        RichTextStyle style = RichTextStyle.empty.withColorVar(accent).withStyle(Style.EMPTY.withColor(0x00FF00));

        assertEquals(0x80FF0000, style.textColor(theme(0x80FF0000), fallback));
    }

    @Test
    void boundSlotWithoutThemeValueFallsBackToLiteral() {
        RichTextStyle style = RichTextStyle.empty.withColorVar(accent).withStyle(Style.EMPTY.withColor(0x00FF00));

        assertEquals(0xFF00FF00, style.textColor(slot -> null, fallback));
    }

    @Test
    void boundSlotWithoutLiteralIsTransparent() {
        RichTextStyle style = RichTextStyle.empty.withColorVar(accent);

        assertEquals(0x00000000, style.textColor(slot -> null, fallback));
    }

    @Test
    void unboundSlotUsesLiteralThenRendererDefault() {
        assertEquals(
            0xFF0000FF,
            RichTextStyle.of(Style.EMPTY.withColor(0x0000FF)).textColor(theme(0xFFFFFF), fallback)
        );
        assertEquals(fallback, RichTextStyle.empty.textColor(theme(0xFFFFFF), fallback));
    }

    @Test
    void absentThemeResolvesNothing() {
        RichTextStyle withLiteral = RichTextStyle.empty.withColorVar(accent).withStyle(Style.EMPTY.withColor(0x00FF00));

        assertEquals(0xFF00FF00, withLiteral.textColor(null, fallback));
        assertEquals(0x00000000, RichTextStyle.empty.withColorVar(accent).textColor(null, fallback));
    }

    @Test
    void slotMergesLikeEveryOtherStyleField() {
        RichTextStyle inherited = RichTextStyle.empty.withColorVar(missing);
        RichTextStyle own = RichTextStyle.empty.withColorVar(accent);

        // other's non-null fields win, so the argument's slot replaces the receiver's
        assertSame(missing, own.merge(inherited).colorVar());
        assertSame(accent, inherited.merge(own).colorVar());
        assertSame(accent, own.merge(RichTextStyle.empty).colorVar());
    }

    @Test
    void renderOptionsCarryTheResolver() {
        ThemeColorResolver resolver = theme(0xFFFFFFFF);

        assertSame(resolver, RenderOptions.defaults.withThemeColors(resolver).themeColors());
        assertNull(RenderOptions.defaults.themeColors());
    }

    @Test
    void penColorSlotKeepsTheLiteralAsFallback() {
        RichText text = RichText.builder().color(0x00FF00).color(accent).text("warning").build();

        RichTextStyle style = ((StyledNode) text.children().getFirst()).style();
        assertSame(accent, style.colorVar());
        assertEquals(0x00FF00, style.style().getColor().getValue());
        assertEquals(0xFF00FF00, style.textColor(null, fallback));
        assertFalse(style.isEmpty());
    }
}
