package dev.vfyjxf.cloudlib.api.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.vfyjxf.cloudlib.api.text.TestTextEnvs.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextBuilderTest {

    @Test
    void plainTextStaysUnstyled() {
        RichText text = RichText.of("hi");
        assertEquals(1, text.children().size());
        assertInstanceOf(TextNode.class, text.children().getFirst());
        assertTrue(text.isTextual());
    }

    @Test
    void penWrapsContentInStyledNode() {
        RichText text = RichText.builder().color(0xFF0000).text("red").clearStyle().text("plain").build();

        StyledNode styled = assertInstanceOf(StyledNode.class, text.children().get(0));
        assertEquals(0xFF0000, styled.style().style().getColor().getValue());
        assertInstanceOf(TextNode.class, text.children().get(1));
    }

    @Test
    void pushPopRestoresPen() {
        RichText text = RichText.builder().pushStyle().bold().text("bold").popStyle().text("plain").build();

        StyledNode styled = assertInstanceOf(StyledNode.class, text.children().get(0));
        assertTrue(styled.style().style().isBold());
        assertInstanceOf(TextNode.class, text.children().get(1));
    }

    @Test
    void penStyleMerges() {
        RichText text = RichText.builder().bold().color(0x00FF00).text("both").build();

        StyledNode styled = assertInstanceOf(StyledNode.class, text.children().getFirst());
        assertTrue(styled.style().style().isBold());
        assertEquals(0x00FF00, styled.style().style().getColor().getValue());
    }

    @Test
    void onClickLastDecoratesMostRecentNode() {
        AtomicBoolean fired = new AtomicBoolean();
        RichText text = RichText.builder().text("click me").onClickLast(ClickAction.run(() -> fired.set(true))).build();

        StyledNode styled = assertInstanceOf(StyledNode.class, text.children().getFirst());
        ClickAction.Callback callback = assertInstanceOf(ClickAction.Callback.class, styled.onClick());
        callback.handler().accept(new ClickAction.Context(0, 0, 0));
        assertTrue(fired.get());
    }

    @Test
    void decorateWithoutContentThrows() {
        RichText.Builder builder = RichText.builder();
        assertThrows(IllegalStateException.class, () -> builder.onClickLast(ClickAction.run(() -> {})));
    }

    @Test
    void popWithoutPushThrows() {
        RichText.Builder builder = RichText.builder();
        assertThrows(IllegalStateException.class, builder::popStyle);
    }

    @Test
    void toComponentRoundTripsPlainText() {
        RichText text = RichText.builder().text("hello ").bold().color(ChatFormatting.RED).text("world").build();

        assertEquals("hello world", text.toComponent().getString());
    }

    @Test
    void translatableConvertsToVanillaTranslatable() {
        RichText text = RichText.builder().translatable("test.key", "a", 42).build();

        // The group root converts to an empty component with the translatable as sibling.
        Component component = text.toComponent().getSiblings().getFirst();
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, component.getContents());
        assertEquals("test.key", contents.getKey());
    }

    @Test
    void objectNodesAreNotTextual() {
        RichText text = RichText.builder().text("a").spacer(4).build();
        assertFalse(text.isTextual());
        assertThrows(IllegalStateException.class, text::toComponent);
    }

    @Test
    void flattenMergesInheritedStyles() {
        Component component = Component.literal("a").withStyle(ChatFormatting.RED)
                .append(Component.literal("b").withStyle(ChatFormatting.BOLD));

        List<ComponentAdapter.Segment> segments = new ArrayList<>();
        ComponentAdapter.flatten(
            component,
            Style.EMPTY,
            (text, style) -> segments.add(new ComponentAdapter.Segment(text, style))
        );

        assertEquals(2, segments.size());
        assertEquals("a", segments.get(0).text());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), segments.get(0).style().getColor());
        assertFalse(segments.get(0).style().isBold());
        assertEquals("b", segments.get(1).text());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), segments.get(1).style().getColor());
        assertTrue(segments.get(1).style().isBold());
    }

    @Test
    void appendInlinesOtherDocument() {
        RichText inner = RichText.builder().text("x").text("y").build();
        RichText outer = RichText.builder().text("a").append(inner).text("b").build();

        assertEquals("axyb", outer.toComponent().getString());
    }

    @Test
    void styledNodeCarriesNullActionsByDefault() {
        RichText text = RichText.builder().bold().text("x").build();
        StyledNode styled = assertInstanceOf(StyledNode.class, text.children().getFirst());
        assertNull(styled.onClick());
        assertNull(styled.onHover());
    }
}
