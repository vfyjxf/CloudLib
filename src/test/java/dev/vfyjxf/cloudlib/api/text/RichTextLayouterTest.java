package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.LayoutConstraints;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextLayouter;
import dev.vfyjxf.cloudlib.api.text.layout.TextFragment;
import dev.vfyjxf.cloudlib.api.text.layout.TextLine;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static dev.vfyjxf.cloudlib.api.text.TestTextEnvs.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextLayouterTest {

    private final RichTextLayouter layouter = TestTextEnvs.layouter();

    private static LaidOutText wrap(String text, int maxWidth) {
        return TestTextEnvs.layouter().layout(RichText.of(text).root(), LayoutConstraints.wrap(maxWidth));
    }

    @Test
    void singleLineUnconstrained() {
        LaidOutText laidOut = layouter.layout(RichText.of("hello world").root(), LayoutConstraints.unconstrained());
        assertEquals(1, laidOut.lines().size());
        assertEquals(TestTextEnvs.width("hello world"), laidOut.width());
        assertEquals(TestTextEnvs.LINE_HEIGHT, laidOut.height());
        assertEquals("hello", laidOut.lines().getFirst().fragments().getFirst().text());
    }

    @Test
    void wrapsAtDefiniteWidth() {
        // "hello"(30) space(6) "world"(30), wrap at 36: line break before "world";
        // the trailing space collapses and does not count toward the line width.
        LaidOutText laidOut = wrap("hello world", 36);
        assertEquals(2, laidOut.lines().size());
        assertEquals(36, laidOut.width());
        assertEquals(2 * TestTextEnvs.LINE_HEIGHT, laidOut.height());
        assertEquals(30, laidOut.lines().get(0).width());
        assertEquals(30, laidOut.lines().get(1).width());
    }

    @Test
    void leadingSpaceOfFreshLineIsDropped() {
        // "a"(6) space(6) "b"(6) wrapped at 8: the space is consumed by the break.
        LaidOutText laidOut = wrap("a b", 8);
        assertEquals(2, laidOut.lines().size());
        TextLine second = laidOut.lines().get(1);
        assertEquals(1, second.fragments().size());
        assertEquals("b", second.fragments().getFirst().text());
        assertEquals(6, second.width());
    }

    @Test
    void overlongWordBreaksPerCodepoint() {
        // "abcdefghij"(60) at width 20: 3 codepoints (18px) fit per line.
        LaidOutText laidOut = wrap("abcdefghij", 20);
        assertEquals(4, laidOut.lines().size());
        assertEquals("abc", laidOut.lines().get(0).fragments().getFirst().text());
        assertEquals("def", laidOut.lines().get(1).fragments().getFirst().text());
        assertEquals("ghi", laidOut.lines().get(2).fragments().getFirst().text());
        assertEquals("j", laidOut.lines().get(3).fragments().getFirst().text());
    }

    @Test
    void explicitBreaksAndTrailingBreak() {
        LaidOutText laidOut = layouter.layout(
                RichText.builder().text("a").newline().text("b").build().root(),
                LayoutConstraints.unconstrained()
        );
        assertEquals(2, laidOut.lines().size());

        LaidOutText trailing = layouter.layout(RichText.of("a\n").root(), LayoutConstraints.unconstrained());
        assertEquals(2, trailing.lines().size());
        assertTrue(trailing.lines().get(1).fragments().isEmpty());
        assertEquals(TestTextEnvs.LINE_HEIGHT, trailing.lines().get(1).height());
    }

    @Test
    void objectAtomIsUnbreakableAndRaisesLineHeight() {
        ImageNode image = new ImageNode(VisualTexture.empty, 20, 20);
        RichText text = RichText.builder().text("ab").image(VisualTexture.empty, 20, 20).build();
        LaidOutText laidOut = layouter.layout(text.root(), LayoutConstraints.wrap(30));
        // "ab"(12) + image(20) = 32 > 30 -> image wraps to line 2, which is 20 tall.
        assertEquals(2, laidOut.lines().size());
        assertEquals(TestTextEnvs.LINE_HEIGHT, laidOut.lines().get(0).height());
        TextLine second = laidOut.lines().get(1);
        assertEquals(20, second.height());
        TextFragment fragment = second.fragments().getFirst();
        assertEquals(TextFragment.Kind.OBJECT, fragment.kind());
        assertSame(image.texture(), ((ImageNode) fragment.source()).texture());
        // BASELINE/BOTTOM: bottom-aligned within the line box.
        assertEquals(0, fragment.y() - (second.y() + second.height() - fragment.height()), 0.001);
    }

    @Test
    void verticalAlignPositions() {
        // Object smaller than the text line: line height = 9, offsets differ per mode.
        for (VerticalAlign align : VerticalAlign.values()) {
            ImageNode small = new ImageNode(VisualTexture.empty, 6, 6);
            RichText text = RichText.builder()
                    .text("a")
                    .append(new StyledNode(small, RichTextStyle.EMPTY.withVerticalAlign(align)))
                    .build();
            LaidOutText laidOut = layouter.layout(text.root(), LayoutConstraints.unconstrained());
            TextLine line = laidOut.lines().getFirst();
            TextFragment fragment = line.fragments().get(1);
            assertEquals(TestTextEnvs.LINE_HEIGHT, line.height());
            float expected = switch (align) {
                case BASELINE, BOTTOM -> TestTextEnvs.LINE_HEIGHT - 6f;
                case TOP -> 0f;
                case MIDDLE -> (TestTextEnvs.LINE_HEIGHT - 6f) / 2f;
            };
            assertEquals(expected, fragment.y() - line.y(), 0.001, align.name());
        }
    }

    @Test
    void horizontalAlignment() {
        LaidOutText centered = TestTextEnvs.layouter().layout(
                RichText.of("ab").root(),
                new LayoutConstraints(60, dev.vfyjxf.cloudlib.api.text.layout.TextAlignment.CENTER, 0)
        );
        assertEquals(24, centered.lines().getFirst().fragments().getFirst().x(), 0.001);

        LaidOutText right = TestTextEnvs.layouter().layout(
                RichText.of("ab").root(),
                new LayoutConstraints(60, dev.vfyjxf.cloudlib.api.text.layout.TextAlignment.RIGHT, 0)
        );
        assertEquals(48, right.lines().getFirst().fragments().getFirst().x(), 0.001);
    }

    @Test
    void lineSpacingAddsBetweenLinesOnly() {
        LaidOutText laidOut = TestTextEnvs.layouter().layout(
                RichText.builder().text("a").newline().text("b").build().root(),
                new LayoutConstraints(LayoutConstraints.UNCONSTRAINED, dev.vfyjxf.cloudlib.api.text.layout.TextAlignment.LEFT, 3)
        );
        assertEquals(2 * TestTextEnvs.LINE_HEIGHT + 3, laidOut.height());
        assertEquals(TestTextEnvs.LINE_HEIGHT + 3, laidOut.lines().get(1).y());
    }

    @Test
    void componentNodeFlattensWithVanillaStyles() {
        Component component = Component.literal("a ").append(
                Component.literal("b").withStyle(Style.EMPTY.withColor(0xFF0000)));
        LaidOutText laidOut = layouter.layout(RichText.of(component).root(), LayoutConstraints.unconstrained());
        List<TextFragment> fragments = laidOut.lines().getFirst().fragments();
        assertEquals("a", fragments.get(0).text());
        assertEquals("b", fragments.get(2).text());
        assertEquals(0xFF0000, fragments.get(2).style().style().getColor().getValue());
    }

    @Test
    void translatableSplicesSequentialArgs() {
        RichTextLayouter translating = TestTextEnvs.layouter(
                TestTextEnvs.patterns("test.craft", "Use %s to craft %s"));
        RichText text = RichText.builder()
                .translatable("test.craft", "key", new ImageNode(VisualTexture.empty, 12, 12))
                .build();
        LaidOutText laidOut = translating.layout(text.root(), LayoutConstraints.unconstrained());
        List<TextFragment> fragments = laidOut.lines().getFirst().fragments();
        StringBuilder plain = new StringBuilder();
        boolean hasObject = false;
        for (TextFragment fragment : fragments) {
            if (fragment.kind() == TextFragment.Kind.OBJECT) {
                hasObject = true;
            } else if (fragment.text() != null) {
                plain.append(fragment.text());
            } else {
                plain.append(' ');
            }
        }
        assertTrue(hasObject);
        assertEquals("Use key to craft ", plain.toString());
    }

    @Test
    void translatableSplicesIndexedArgsOutOfOrder() {
        RichTextLayouter translating = TestTextEnvs.layouter(
                TestTextEnvs.patterns("test.order", "%2$s %1$s"));
        RichText text = RichText.builder().translatable("test.order", "a", "b").build();
        LaidOutText laidOut = translating.layout(text.root(), LayoutConstraints.unconstrained());
        List<TextFragment> fragments = laidOut.lines().getFirst().fragments();
        assertEquals("b", fragments.get(0).text());
        assertEquals("a", fragments.get(2).text());
    }

    @Test
    void translatableFallsBackToKeyAndLiteralPlaceholder() {
        // Missing key -> key text itself.
        LaidOutText missing = layouter.layout(
                RichText.builder().translatable("test.missing").build().root(),
                LayoutConstraints.unconstrained());
        assertEquals("test.missing", missing.lines().getFirst().fragments().getFirst().text());

        // Unsatisfied placeholder renders literally, like vanilla.
        RichTextLayouter translating = TestTextEnvs.layouter(
                TestTextEnvs.patterns("test.unsatisfied", "value: %s at %s"));
        LaidOutText laidOut = translating.layout(
                RichText.builder().translatable("test.unsatisfied", "v").build().root(),
                LayoutConstraints.unconstrained());
        List<TextFragment> fragments = laidOut.lines().getFirst().fragments();
        StringBuilder plain = new StringBuilder();
        for (TextFragment fragment : fragments) {
            plain.append(fragment.text() != null ? fragment.text() : " ");
        }
        assertEquals("value: v at %s", plain.toString());
    }

    @Test
    void styleInheritsAndMergesThroughGroups() {
        RichText text = RichText.of(new StyledNode(
                new GroupNode(List.of(
                        new TextNode("a"),
                        new StyledNode(new TextNode("b"), RichTextStyle.of(Style.EMPTY.withBold(true)))
                )),
                RichTextStyle.of(Style.EMPTY.withColor(0x00FF00))
        ));
        LaidOutText laidOut = layouter.layout(text.root(), LayoutConstraints.unconstrained());
        List<TextFragment> fragments = laidOut.lines().getFirst().fragments();
        // "a" gets the outer color; "b" gets color AND its own bold.
        assertEquals(0x00FF00, fragments.get(0).style().style().getColor().getValue());
        assertFalse(fragments.get(0).style().style().isBold());
        assertEquals(0x00FF00, fragments.get(1).style().style().getColor().getValue());
        assertTrue(fragments.get(1).style().style().isBold());
    }

    @Test
    void fragmentsCarrySourceAndActions() {
        var action = ClickAction.run(() -> {
        });
        RichText text = RichText.builder()
                .text("plain ")
                .text("link")
                .onClickLast(action)
                .build();
        LaidOutText laidOut = layouter.layout(text.root(), LayoutConstraints.unconstrained());
        List<TextFragment> fragments = laidOut.lines().getFirst().fragments();
        // fragments: word "plain", space, word "link".
        assertNull(fragments.get(0).onClick());
        assertNull(fragments.get(1).onClick());
        assertSame(action, fragments.get(2).onClick());
        assertInstanceOf(TextNode.class, fragments.get(2).source());
    }

    @Test
    void vanillaStyleClickEventBecomesInteractive() {
        Style clickable = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com"));
        RichText text = RichText.of(new StyledNode(new TextNode("url"), RichTextStyle.of(clickable)));
        LaidOutText laidOut = layouter.layout(text.root(), LayoutConstraints.unconstrained());
        TextFragment fragment = laidOut.lines().getFirst().fragments().getFirst();
        assertInstanceOf(ClickAction.Vanilla.class, fragment.onClick());
        assertTrue(fragment.interactive());
    }

    @Test
    void fragmentAtHitTests() {
        LaidOutText laidOut = layouter.layout(RichText.of("hello world").root(), LayoutConstraints.unconstrained());
        TextFragment hello = laidOut.fragmentAt(3, 3);
        assertNotNull(hello);
        assertEquals("hello", hello.text());
        TextFragment world = laidOut.fragmentAt(TestTextEnvs.width("hello") + 8, 3);
        assertNotNull(world);
        assertEquals("world", world.text());
        assertNull(laidOut.fragmentAt(500, 3));
        assertNull(laidOut.fragmentAt(3, 500));
    }

    @Test
    void paddingExpandsObjectReservedBox() {
        RichText text = RichText.builder()
                .padding(dev.vfyjxf.cloudlib.api.math.Insets.uniform(2))
                .image(VisualTexture.empty, 10, 10)
                .build();
        LaidOutText laidOut = layouter.layout(text.root(), LayoutConstraints.unconstrained());
        TextFragment fragment = laidOut.lines().getFirst().fragments().getFirst();
        assertEquals(14, fragment.width());
        assertEquals(14, laidOut.lines().getFirst().height());
    }

    @Test
    void emptyDocumentLaysOutEmpty() {
        LaidOutText laidOut = layouter.layout(RichText.empty().root(), LayoutConstraints.unconstrained());
        assertTrue(laidOut.isEmpty());
        assertEquals(0, laidOut.height());
    }
}
