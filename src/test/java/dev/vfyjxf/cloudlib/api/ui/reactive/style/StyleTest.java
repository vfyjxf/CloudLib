package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.api.ui.reactive.style.Styles.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Style API.
 */
@DisplayName("Style API Tests")
class StyleTest {

    @Nested
    @DisplayName("Style.of() creation")
    class CreationTests {

        @Test
        @DisplayName("Create empty style")
        void emptyStyle() {
            Style style = Style.of();
            assertTrue(style.isEmpty());
            assertEquals(0, style.size());
            assertSame(Style.EMPTY, style);
        }

        @Test
        @DisplayName("Create style with single property")
        void singleProperty() {
            Style style = Style.of(padding(10));
            assertFalse(style.isEmpty());
            assertEquals(1, style.size());
            assertTrue(style.has("padding"));
        }

        @Test
        @DisplayName("Create style with multiple properties")
        void multipleProperties() {
            Style style = Style.of(
                    padding(12),
                    background(0xFFFFFFFF),
                    border(1, 0xFF666666),
                    rounded(4)
            );

            assertEquals(4, style.size());
            assertTrue(style.has("padding"));
            assertTrue(style.has("background"));
            assertTrue(style.has("border"));
            assertTrue(style.has("borderRadius"));
        }

        @Test
        @DisplayName("Duplicate properties keep last one")
        void duplicateProperties() {
            Style style = Style.of(
                    padding(10),
                    padding(20)
            );

            assertEquals(1, style.size());
            PaddingProperty padding = style.get("padding", PaddingProperty.class);
            assertNotNull(padding);
            assertEquals(20, padding.getTop());
        }
    }

    @Nested
    @DisplayName("Style composition")
    class CompositionTests {

        @Test
        @DisplayName("with() adds properties")
        void withAddsProperties() {
            Style base = Style.of(padding(10));
            Style extended = base.with(background(0xFFFFFFFF));

            assertEquals(1, base.size());
            assertEquals(2, extended.size());
            assertTrue(extended.has("padding"));
            assertTrue(extended.has("background"));
        }

        @Test
        @DisplayName("with() overrides same-name properties")
        void withOverrides() {
            Style base = Style.of(padding(10));
            Style extended = base.with(padding(20));

            PaddingProperty padding = extended.get("padding", PaddingProperty.class);
            assertNotNull(padding);
            assertEquals(20, padding.getTop());
        }

        @Test
        @DisplayName("merge() combines styles")
        void mergeCombines() {
            Style style1 = Style.of(padding(10), background(0xFFFFFFFF));
            Style style2 = Style.of(border(1, 0xFF000000), rounded(4));

            Style merged = style1.merge(style2);

            assertEquals(4, merged.size());
            assertTrue(merged.has("padding"));
            assertTrue(merged.has("background"));
            assertTrue(merged.has("border"));
            assertTrue(merged.has("borderRadius"));
        }

        @Test
        @DisplayName("merge() later style overrides")
        void mergeOverrides() {
            Style style1 = Style.of(padding(10));
            Style style2 = Style.of(padding(20));

            Style merged = style1.merge(style2);

            PaddingProperty padding = merged.get("padding", PaddingProperty.class);
            assertNotNull(padding);
            assertEquals(20, padding.getTop());
        }

        @Test
        @DisplayName("without() removes property")
        void withoutRemoves() {
            Style style = Style.of(padding(10), background(0xFFFFFFFF));
            Style filtered = style.without("padding");

            assertEquals(1, filtered.size());
            assertFalse(filtered.has("padding"));
            assertTrue(filtered.has("background"));
        }

        @Test
        @DisplayName("Chained composition")
        void chainedComposition() {
            Style style = Style.of(padding(10))
                    .with(background(0xFFFFFFFF))
                    .with(border(1, 0xFF000000))
                    .with(rounded(4));

            assertEquals(4, style.size());
        }
    }

    @Nested
    @DisplayName("StyleContext application")
    class ApplicationTests {

        @Test
        @DisplayName("Apply padding to context")
        void applyPadding() {
            Style style = Style.of(padding(10, 20, 30, 40));
            StyleContext context = style.createContext();

            // Padding is a layout property - verify it was recorded
            assertTrue(context.hasProperty("padding"));
            PaddingProperty prop = context.getAppliedProperties().stream()
                    .filter(p -> p instanceof PaddingProperty)
                    .map(p -> (PaddingProperty) p)
                    .findFirst()
                    .orElse(null);
            assertNotNull(prop);
            assertEquals(10, prop.getTop());
            assertEquals(20, prop.getRight());
            assertEquals(30, prop.getBottom());
            assertEquals(40, prop.getLeft());
        }

        @Test
        @DisplayName("Apply background to context")
        void applyBackground() {
            Style style = Style.of(background(0xFFFF0000));
            StyleContext context = style.createContext();

            assertEquals(0xFFFF0000, context.getVisualContext().getBackgroundColor());
        }

        @Test
        @DisplayName("Apply border to context")
        void applyBorder() {
            Style style = Style.of(border(2, 0xFF00FF00));
            StyleContext context = style.createContext();

            assertEquals(2, context.getVisualContext().getBorderWidth());
            assertEquals(0xFF00FF00, context.getVisualContext().getBorderColor());
        }

        @Test
        @DisplayName("Apply rounded corners to context")
        void applyRounded() {
            Style style = Style.of(rounded(8));
            StyleContext context = style.createContext();

            VisualContext visual = context.getVisualContext();
            assertEquals(8, visual.getBorderRadiusTopLeft());
            assertEquals(8, visual.getBorderRadiusTopRight());
            assertEquals(8, visual.getBorderRadiusBottomRight());
            assertEquals(8, visual.getBorderRadiusBottomLeft());
        }

        @Test
        @DisplayName("Apply size to context")
        void applySize() {
            Style style = Style.of(size(100, 50));
            StyleContext context = style.createContext();

            // Size is a layout property - verify it was recorded
            assertTrue(context.hasProperty("size"));
            SizeProperty prop = context.getAppliedProperties().stream()
                    .filter(p -> p instanceof SizeProperty)
                    .map(p -> (SizeProperty) p)
                    .findFirst()
                    .orElse(null);
            assertNotNull(prop);
            assertEquals(100, prop.getWidth());
            assertEquals(50, prop.getHeight());
        }

        @Test
        @DisplayName("Apply cursor to context")
        void applyCursor() {
            Style style = Style.of(cursor(Cursor.HAND));
            StyleContext context = style.createContext();

            assertEquals(Cursor.HAND, context.getVisualContext().getCursor());
        }

        @Test
        @DisplayName("Apply opacity to context")
        void applyOpacity() {
            Style style = Style.of(opacity(0.5));
            StyleContext context = style.createContext();

            assertEquals(0.5, context.getVisualContext().getOpacity());
        }

        @Test
        @DisplayName("Apply custom property to context")
        void applyCustom() {
            Style style = Style.of(custom("shadow", ctx -> {
                ctx.getVisualContext().setCustom("shadowBlur", 4.0);
                ctx.getVisualContext().setCustom("shadowColor", 0x80000000);
            }));
            StyleContext context = style.createContext();

            assertEquals(4.0, context.getVisualContext().getCustom("shadowBlur"));
            assertEquals(0x80000000, context.getVisualContext().<Integer>getCustom("shadowColor"));
        }
    }

    @Nested
    @DisplayName("Property DSL")
    class PropertyDSLTests {

        @Test
        @DisplayName("Padding variants")
        void paddingVariants() {
            // Single value
            PaddingProperty p1 = padding(10);
            assertEquals(10, p1.getTop());
            assertEquals(10, p1.getRight());
            assertEquals(10, p1.getBottom());
            assertEquals(10, p1.getLeft());

            // Two values (vertical, horizontal)
            PaddingProperty p2 = padding(10, 20);
            assertEquals(10, p2.getTop());
            assertEquals(20, p2.getRight());
            assertEquals(10, p2.getBottom());
            assertEquals(20, p2.getLeft());

            // Four values
            PaddingProperty p3 = padding(1, 2, 3, 4);
            assertEquals(1, p3.getTop());
            assertEquals(2, p3.getRight());
            assertEquals(3, p3.getBottom());
            assertEquals(4, p3.getLeft());
        }

        @Test
        @DisplayName("Text style properties")
        void textStyleProperties() {
            assertTrue(bold().isBold());
            assertTrue(italic().isItalic());
            assertTrue(underline().isUnderline());
            assertTrue(strikethrough().isStrikethrough());
        }

        @Test
        @DisplayName("Size constraints")
        void sizeConstraints() {
            SizeConstraintProperty minW = minWidth(100);
            assertEquals(100, minW.getMinWidth());

            SizeConstraintProperty maxW = maxWidth(200);
            assertEquals(200, maxW.getMaxWidth());
        }
    }

    @Nested
    @DisplayName("Builder API")
    class BuilderTests {

        @Test
        @DisplayName("Build style using builder")
        void buildStyle() {
            Style style = Style.builder()
                    .add(padding(10))
                    .add(background(0xFFFFFFFF))
                    .add(border(1, 0xFF000000))
                    .build();

            assertEquals(3, style.size());
        }

        @Test
        @DisplayName("Builder remove")
        void builderRemove() {
            Style style = Style.builder()
                    .add(padding(10))
                    .add(background(0xFFFFFFFF))
                    .remove("padding")
                    .build();

            assertEquals(1, style.size());
            assertFalse(style.has("padding"));
        }

        @Test
        @DisplayName("Convert to builder and modify")
        void toBuilder() {
            Style original = Style.of(padding(10));
            Style modified = original.toBuilder()
                    .add(background(0xFFFFFFFF))
                    .build();

            assertEquals(1, original.size());
            assertEquals(2, modified.size());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Empty style toString")
        void emptyToString() {
            assertEquals("Style.EMPTY", Style.EMPTY.toString());
        }

        @Test
        @DisplayName("Style with properties toString")
        void styleToString() {
            Style style = Style.of(padding(10), background(0xFFFFFFFF));
            String str = style.toString();

            assertTrue(str.contains("padding"));
            assertTrue(str.contains("background"));
        }
    }

    @Nested
    @DisplayName("Custom StyleProperty extension")
    class ExtensionTests {

        @Test
        @DisplayName("External custom property implementation")
        void customPropertyImplementation() {
            // Simulate an external custom property
            StyleProperty shadowProperty = new StyleProperty() {
                @Override
                public void apply(StyleContext context) {
                    context.getVisualContext().setCustom("shadowOffsetX", 2.0);
                    context.getVisualContext().setCustom("shadowOffsetY", 2.0);
                    context.getVisualContext().setCustom("shadowBlur", 4.0);
                    context.getVisualContext().setCustom("shadowColor", 0x80000000);
                }

                @Override
                public String name() {
                    return "shadow";
                }

                @Override
                public String valueToString() {
                    return "2px 2px 4px rgba(0,0,0,0.5)";
                }
            };

            Style style = Style.of(
                    padding(10),
                    shadowProperty
            );

            assertEquals(2, style.size());
            assertTrue(style.has("shadow"));

            StyleContext context = style.createContext();
            assertEquals(2.0, context.getVisualContext().getCustom("shadowOffsetX"));
            assertEquals(0x80000000, context.getVisualContext().<Integer>getCustom("shadowColor"));
        }
    }
}
