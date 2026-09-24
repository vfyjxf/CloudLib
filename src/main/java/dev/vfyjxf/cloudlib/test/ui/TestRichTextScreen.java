package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.text.ClickAction;
import dev.vfyjxf.cloudlib.api.text.CustomRenderNode;
import dev.vfyjxf.cloudlib.api.text.HoverAction;
import dev.vfyjxf.cloudlib.api.text.ItemNode;
import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.text.VerticalAlign;
import dev.vfyjxf.cloudlib.api.text.WidgetNode;
import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.RichTextWidget;
import dev.vfyjxf.cloudlib.ui.widget.ToggleWidget;
import dev.vfyjxf.taffy.style.TextAlign;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.scrollable;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.background;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.flexGrow;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.minHeight;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.padding;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.textAlign;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.widthOf;

/**
 * Rich demo for the rich text system: styled runs, localization with rich
 * arguments, inline images/items/blocks/entities, interactive fragments,
 * embedded widgets, alignment and wrapping.
 */
@TestScreen
public class TestRichTextScreen extends BasicScreen {

    private int clickCount;
    private boolean toggleState;
    private long startTime = -1;

    public TestRichTextScreen() {
        var scroll = ScrollState.create(ScrollDirection.vertical).scrollSpeed(14).smooth(true).smoothSpeed(0.35f)
                .trackTexture(new ColorTexture(0x60000000)).thumbTexture(new ColorTexture(0xCC7777DD))
                .scrollbarWidth(5);

        var column = ColumnWidget.create(10);
        column.useStyle(UIStyle.of(flexGrow(1), minHeight(0), padding(14), background(new ColorTexture(0xC0101014))));
        column.useEffect(scrollable(scroll));
        column.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
            scroll.scrollBy(0, (float) (-scrollY * scroll.scrollSpeed()));
            return EventDispatch.consumed;
        });

        column.addWidget(header());
        column.addWidget(section("Styled Runs", styledRuns()));
        column.addWidget(section("Localization", localization()));
        column.addWidget(section("Inline Media", inlineMedia()));
        column.addWidget(section("Entities", entities()));
        column.addWidget(section("Interactive Fragments", interactive()));
        column.addWidget(section("Embedded Widgets", embeddedWidgets()));
        column.addWidget(section("Alignment & Wrapping", alignmentAndWrapping()));
        column.addWidget(section("Rich Tooltip", richTooltip()));

        var footer = LabelWidget.of("clicks: 0").setColor(0xFF66FF66);
        column.addWidget(footer);
        column.onTick(() -> footer.setText("clicks: " + clickCount + (toggleState ? "  |  toggle on" : "")));

        mainGroup().addWidget(column);
    }

    private Widget header() {
        var title = RichTextWidget.of(
            RichText.builder().shadow(true).text("✦ ").color(0xFF55FFFF).text("CloudLib Rich Text").color(0xFFFFAA00)
                    .bold().text(" ✦").color(0xFF55FFFF).clearStyle().build()
        );
        title.useStyle(UIStyle.of(UIStyles.alignSelfStretch(), textAlign(TextAlign.CENTER)));
        return title;
    }

    /**
     * Section card: a titled box around one demo.
     */
    private Widget section(String title, Widget content) {
        var card = ColumnWidget.create(6);
        card.useStyle(UIStyle.of(padding(8), background(new ColorTexture(0x40222830))));
        card.addWidget(LabelWidget.of(title).setColor(0xFFFFFF66));
        card.addWidget(content);
        return card;
    }

    private Widget styledRuns() {
        return RichTextWidget.of(
            RichText.builder().text("plain ").color(0xFFCCCCCC).text("rgb ").color(0xFF6699FF).bold().text("bold ")
                    .italic().text("bold-italic ").clearStyle().underlined().text("underline ").strikethrough()
                    .text("strike ").clearStyle().obfuscated().text("magic ").clearStyle().shadow(true).text("shadow ")
                    .clearStyle().highlight(0x6633CC55).text("highlight").clearStyle().newline()
                    .text("vanilla component: ").color(0xFF999999)
                    .component(
                        Component.literal("styled component")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true).withItalic(true))
                    ).build()
        );
    }

    private Widget localization() {
        var column = ColumnWidget.create(4);
        // "<%s> %s" with a styled component argument
        column.addWidget(
            RichTextWidget.of(
                RichText.builder()
                        .translatable(
                            "chat.type.text",
                            "Steve",
                            Component.literal("localized with styled args").withStyle(ChatFormatting.AQUA)
                        ).build()
            )
        );
        // "%s has made the advancement %s" — with an inline item as the second argument
        column.addWidget(
            RichTextWidget.of(
                RichText.builder()
                        .translatable(
                            "chat.type.advancement.task",
                            "Alex",
                            new ItemNode(new ItemStack(Items.NETHERITE_PICKAXE), 16, true)
                        ).build()
            )
        );
        // Missing key falls back to the key itself, like vanilla
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("missing key: ").color(0xFF888888).translatable("cloudlib.demo.missing.key")
                        .build()
            )
        );
        return column;
    }

    private Widget inlineMedia() {
        var column = ColumnWidget.create(4);
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("icons ").color(0xFFCCCCCC)
                        .image(
                            ImageTexture.of(ResourceLocation.withDefaultNamespace("textures/item/diamond.png"), 16, 16)
                        ).text(" ")
                        .image(
                            ImageTexture
                                    .of(ResourceLocation.withDefaultNamespace("textures/item/golden_apple.png"), 16, 16)
                        ).text(" items ").color(0xFFCCCCCC).item(new ItemStack(Items.DIAMOND_SWORD), 16, true).text(" ")
                        .item(new ItemStack(Items.TNT, 42), 16, true).text(" scaled-up ").color(0xFFCCCCCC)
                        .item(new ItemStack(Items.RECOVERY_COMPASS), 24, false).build()
            )
        );
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("blocks ").color(0xFFCCCCCC).block(Blocks.GRASS_BLOCK.defaultBlockState(), 20)
                        .text(" ").block(Blocks.FURNACE.defaultBlockState(), 20).text(" ")
                        .block(Blocks.CRAFTING_TABLE.defaultBlockState(), 20).text(" ")
                        .block(Blocks.REDSTONE_LAMP.defaultBlockState(), 20).text(" padded ").color(0xFFCCCCCC)
                        .padding(Insets.uniform(4)).block(Blocks.BEACON.defaultBlockState(), 16).clearStyle().build()
            )
        );
        // A little animated gradient box painted by a CustomRenderNode
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("custom ").color(0xFFCCCCCC)
                        .append(new CustomRenderNode(80, 14, (canvas, x, y, w, h) -> {
                            float t = (System.currentTimeMillis() % 3000L) / 3000f;
                            int r = (int) (128 + 127 * Math.sin(t * Math.PI * 2));
                            int g = (int) (128 + 127 * Math.sin(t * Math.PI * 2 + 2));
                            int b = (int) (128 + 127 * Math.sin(t * Math.PI * 2 + 4));
                            canvas.roundedRect(
                                (int) x,
                                (int) y,
                                (int) w,
                                (int) h,
                                6f,
                                0xFF000000 | r << 16 | g << 8 | b
                            );
                        })).text(" <- animated CustomRenderNode").color(0xFF888888).build()
            )
        );
        return column;
    }

    private Widget entities() {
        if (Minecraft.getInstance().level == null) {
            return LabelWidget.of("(open this screen in a world to see entities)").setColor(0xFF777777);
        }
        return RichTextWidget.of(RichText.builder().text("pig follows mouse ").color(0xFFCCCCCC).entity(() -> {
            var level = Minecraft.getInstance().level;
            return level == null ? null : new Pig(EntityType.PIG, level);
        }, 24, 30, 14, true).text(" zombie static ").color(0xFFCCCCCC).entity(() -> {
            var level = Minecraft.getInstance().level;
            return level == null ? null : new Zombie(EntityType.ZOMBIE, level);
        }, 24, 30, 14, false).build());
    }

    private Widget interactive() {
        var column = ColumnWidget.create(4);
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("[+1]").color(ChatFormatting.GREEN).bold()
                        .onClickLast(ClickAction.run(() -> clickCount++))
                        .onHoverLast(HoverAction.text(Component.literal("Increments the footer counter"))).text("  ")
                        .text("[copy]").color(ChatFormatting.YELLOW)
                        .onClickLast(ClickAction.copyToClipboard("CloudLib rich text demo"))
                        .onHoverLast(HoverAction.text(Component.literal("Copies text to your clipboard"))).text("  ")
                        .text("[link]").color(ChatFormatting.BLUE).underlined()
                        .onClickLast(ClickAction.openUrl("https://github.com")).text("  ").text("[vanilla click event]")
                        .color(ChatFormatting.LIGHT_PURPLE)
                        .onClickLast(
                            ClickAction.vanilla(
                                new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "copied via vanilla ClickEvent")
                            )
                        ).build()
            )
        );
        // Hover with a full CloudLib tooltip
        var richTooltip = Tooltip.create()
                .add(Component.literal("Rich hover tooltip").withStyle(ChatFormatting.GOLD), Tooltip.title).add(
                    RichText.builder().text("inline items work here too ").item(new ItemStack(Items.DIAMOND), 12, false)
                            .build()
                );
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("[hover for rich tooltip]").color(0xFF55FFFF)
                        .onHoverLast(HoverAction.tooltip(richTooltip)).build()
            )
        );
        return column;
    }

    private Widget embeddedWidgets() {
        var button = ButtonWidget.of("+10", () -> clickCount += 10).setColors(0xFF0066CC, 0xFF0088FF, 0xFF004499);
        var toggle = ToggleWidget.create(false).onToggle(state -> toggleState = state)
                .setColors(0xFF555555, 0xFF00CC66);
        toggle.useStyle(UIStyle.of(UIStyles.sizeOf(30, 12)));
        return RichTextWidget.of(
            RichText.builder().text("a button ").color(0xFFCCCCCC).append(new WidgetNode(button, 40, 16))
                    .text(" and a toggle ").color(0xFFCCCCCC).append(new WidgetNode(toggle, 30, 12))
                    .text(" live inside the text flow").color(0xFF888888).build()
        );
    }

    private Widget alignmentAndWrapping() {
        var column = ColumnWidget.create(4);

        var paragraph = RichTextWidget.of(
            RichText.builder().color(0xFFCCCCCC).text("A longer paragraph wraps at the imposed width. ")
                    .color(ChatFormatting.GOLD).text("Styles carry across line breaks, ").color(0xFFCCCCCC)
                    .text("and inline objects like ").item(new ItemStack(Items.BOOK), 12, false)
                    .text(" flow with the words.").build()
        );
        paragraph.useStyle(UIStyle.of(widthOf(240), background(new ColorTexture(0x20FFFFFF))));
        column.addWidget(paragraph);

        for (TextAlign align : new TextAlign[]{TextAlign.LEFT, TextAlign.CENTER, TextAlign.RIGHT}) {
            var aligned = RichTextWidget.of(RichText.of("text-align: " + align));
            aligned.useStyle(UIStyle.of(widthOf(240), textAlign(align), background(new ColorTexture(0x20FFFFFF))));
            column.addWidget(aligned);
        }

        // Vertical alignment of inline objects against the text line
        column.addWidget(
            RichTextWidget.of(
                RichText.builder().text("valign ").color(0xFFCCCCCC).item(new ItemStack(Items.ARROW), 16, false)
                        .text(" baseline / ").color(0xFF888888).verticalAlign(VerticalAlign.top)
                        .item(new ItemStack(Items.ARROW), 16, false).text(" top / ").color(0xFF888888)
                        .verticalAlign(VerticalAlign.middle).item(new ItemStack(Items.ARROW), 16, false).text(" middle")
                        .color(0xFF888888).build()
            )
        );
        return column;
    }

    private Widget richTooltip() {
        var label = LabelWidget.of("hover this label → tooltip with item + styled text").setColor(0xFF88CCFF);
        label.setTooltip(
            Tooltip.create()
                    .add(
                        Component.literal("Rich tooltip").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Tooltip.title
                    ).add(Component.literal("plain line").withStyle(ChatFormatting.GRAY))
                    .add(
                        RichText.builder().text("crafted from ").item(new ItemStack(Items.DIAMOND, 3), 12, true)
                                .text(" + ").block(Blocks.CRAFTING_TABLE.defaultBlockState(), 12).build()
                    )
        );
        return label;
    }
}
