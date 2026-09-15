package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.text.render.CustomRenderer;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.SizedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * A rich text document: an immutable tree of {@link RichNode}s combining styled text,
 * localization, inline images, items, blocks, entities, embedded interactive widgets
 * and custom-rendered boxes.
 * <p>
 * RichText is deliberately vanilla-friendly: plain {@link Component}s (including
 * translatable ones) can be embedded as-is via {@link #of(Component)} or
 * {@link Builder#component(Component)}, and a purely textual document degrades back
 * to a vanilla component via {@link #toComponent()} for vanilla integration points
 * (tooltips, chat, ...).
 * <p>
 * Build instances with the fluent {@link Builder}:
 * <pre>{@code
 * RichText text = RichText.builder()
 *         .text("Crafts with ").color(0xFFAA00)
 *         .item(new ItemStack(Items.DIAMOND_PICKAXE))
 *         .translatable("mymod.recipe.hint", new ImageNode(texture, 9, 9))
 *         .onHoverLast(HoverAction.text(Component.literal("Required")))
 *         .build();
 * }</pre>
 *
 * @see RichNode
 */
public final class RichText {

    private static final RichText EMPTY = new RichText(new GroupNode(List.of()));

    private final GroupNode root;

    private RichText(GroupNode root) {
        this.root = root;
    }

    //region factories

    public static RichText empty() {
        return EMPTY;
    }

    public static RichText of(String text) {
        return new RichText(new GroupNode(new TextNode(text)));
    }

    public static RichText of(Component component) {
        return new RichText(new GroupNode(new ComponentNode(component)));
    }

    public static RichText of(LangEntry entry, Object... args) {
        return new RichText(new GroupNode(new TranslatableNode(entry.key(), args)));
    }

    public static RichText of(RichNode node) {
        return new RichText(new GroupNode(node));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a builder with an initial text run.
     */
    public static Builder text(String text) {
        return builder().text(text);
    }

    /**
     * Starts a builder with an initial vanilla component.
     */
    public static Builder component(Component component) {
        return builder().component(component);
    }

    //endregion

    //region accessors

    public GroupNode root() {
        return root;
    }

    public List<RichNode> children() {
        return root.children();
    }

    public boolean isEmpty() {
        return root.children().isEmpty();
    }

    /**
     * @return whether this document contains only textual content
     * @see RichNode#isTextual()
     */
    public boolean isTextual() {
        return root.isTextual();
    }

    /**
     * Degrades this document to a vanilla component.
     *
     * @throws IllegalStateException when the document embeds non-textual nodes
     *                               (images, items, entities, widgets, ...)
     */
    public MutableComponent toComponent() {
        return ComponentAdapter.toComponent(root);
    }

    /**
     * Whether a {@link TranslatableNode} argument is (or contains only) text.
     */
    public static boolean isTextualArg(Object arg) {
        return switch (arg) {
            case RichNode node -> node.isTextual();
            case Component ignored -> true;
            case null -> true;
            default -> true;
        };
    }

    //endregion

    /**
     * Fluent document builder. Style methods move a "pen" applied to all
     * subsequently added content; {@link #pushStyle()}/{@link #popStyle()} save and
     * restore the pen, and {@code onClickLast}/{@code onHoverLast} decorate the most
     * recently added node.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {

        private final List<RichNode> children = new ArrayList<>();
        private final Deque<RichTextStyle> penStack = new ArrayDeque<>();
        private RichTextStyle pen = RichTextStyle.EMPTY;

        private Builder() {
        }

        //region pen style

        @Contract("_ -> this")
        public Builder style(RichTextStyle style) {
            pen = pen.merge(style);
            return this;
        }

        @Contract("_ -> this")
        public Builder style(Style style) {
            pen = pen.withStyle(pen.style().applyTo(style));
            return this;
        }

        @Contract("_ -> this")
        public Builder color(int rgb) {
            return style(Style.EMPTY.withColor(rgb));
        }

        @Contract("_ -> this")
        public Builder color(TextColor color) {
            return style(Style.EMPTY.withColor(color));
        }

        @Contract("_ -> this")
        public Builder color(ChatFormatting formatting) {
            return style(Style.EMPTY.withColor(formatting));
        }

        @Contract("-> this")
        public Builder bold() {
            return style(Style.EMPTY.withBold(true));
        }

        @Contract("-> this")
        public Builder italic() {
            return style(Style.EMPTY.withItalic(true));
        }

        @Contract("-> this")
        public Builder underlined() {
            return style(Style.EMPTY.withUnderlined(true));
        }

        @Contract("-> this")
        public Builder strikethrough() {
            return style(Style.EMPTY.withStrikethrough(true));
        }

        @Contract("-> this")
        public Builder obfuscated() {
            return style(Style.EMPTY.withObfuscated(true));
        }

        @Contract("_ -> this")
        public Builder shadow(boolean shadow) {
            pen = pen.withShadow(shadow);
            return this;
        }

        @Contract("_ -> this")
        public Builder highlight(int argb) {
            pen = pen.withHighlightColor(argb);
            return this;
        }

        @Contract("_ -> this")
        public Builder verticalAlign(VerticalAlign align) {
            pen = pen.withVerticalAlign(align);
            return this;
        }

        @Contract("_ -> this")
        public Builder padding(Insets padding) {
            pen = pen.withPadding(padding);
            return this;
        }

        @Contract("_ -> this")
        public Builder font(ResourceLocation font) {
            return style(Style.EMPTY.withFont(font));
        }

        /**
         * Resets the pen to no styling.
         */
        @Contract("-> this")
        public Builder clearStyle() {
            pen = RichTextStyle.EMPTY;
            return this;
        }

        @Contract("-> this")
        public Builder pushStyle() {
            penStack.push(pen);
            return this;
        }

        @Contract("-> this")
        public Builder popStyle() {
            if (penStack.isEmpty()) {
                throw new IllegalStateException("popStyle without matching pushStyle");
            }
            pen = penStack.pop();
            return this;
        }

        //endregion

        //region content

        private Builder addNode(RichNode node) {
            children.add(pen.isEmpty() ? node : new StyledNode(node, pen));
            return this;
        }

        @Contract("_ -> this")
        public Builder text(String text) {
            return addNode(new TextNode(text));
        }

        @Contract("_ -> this")
        public Builder component(Component component) {
            return addNode(new ComponentNode(component));
        }

        @Contract("_, _ -> this")
        public Builder translatable(String key, Object... args) {
            return addNode(new TranslatableNode(key, args));
        }

        @Contract("_, _ -> this")
        public Builder translatable(LangEntry entry, Object... args) {
            return translatable(entry.key(), args);
        }

        @Contract("_, _, _ -> this")
        public Builder image(VisualTexture texture, int width, int height) {
            return addNode(new ImageNode(texture, width, height));
        }

        @Contract("_ -> this")
        public Builder image(SizedTexture texture) {
            return addNode(new ImageNode(texture));
        }

        @Contract("_ -> this")
        public Builder item(ItemStack stack) {
            return addNode(new ItemNode(stack));
        }

        @Contract("_, _ -> this")
        public Builder item(ItemStack stack, int size) {
            return addNode(new ItemNode(stack, size, false));
        }

        @Contract("_, _, _ -> this")
        public Builder item(ItemStack stack, int size, boolean showDecorations) {
            return addNode(new ItemNode(stack, size, showDecorations));
        }

        @Contract("_ -> this")
        public Builder block(BlockState state) {
            return addNode(new BlockNode(state));
        }

        @Contract("_, _ -> this")
        public Builder block(BlockState state, int size) {
            return addNode(new BlockNode(state, size));
        }

        @Contract("_, _, _ -> this")
        public Builder entity(Supplier<? extends Entity> entity, int width, int height) {
            return addNode(new EntityNode(entity, width, height));
        }

        @Contract("_, _, _, _, _ -> this")
        public Builder entity(Supplier<? extends Entity> entity, int width, int height, int scale, boolean followMouse) {
            return addNode(new EntityNode(entity, width, height, scale, followMouse));
        }

        @Contract("_, _, _ -> this")
        public Builder widget(Widget widget, int width, int height) {
            return addNode(new WidgetNode(widget, width, height));
        }

        @Contract("_ -> this")
        public Builder spacer(int width) {
            return addNode(new SpacerNode(width));
        }

        @Contract("-> this")
        public Builder newline() {
            return addNode(BreakNode.INSTANCE);
        }

        @Contract("_, _, _ -> this")
        public Builder custom(int width, int height, CustomRenderer renderer) {
            return addNode(new CustomRenderNode(width, height, renderer));
        }

        @Contract("_ -> this")
        public Builder append(RichNode node) {
            return addNode(node);
        }

        @Contract("_ -> this")
        public Builder append(RichText text) {
            for (RichNode child : text.children()) {
                addNode(child);
            }
            return this;
        }

        //endregion

        //region decorate last

        private Builder decorateLast(
                @Nullable ClickAction onClick,
                @Nullable HoverAction onHover
        ) {
            if (children.isEmpty()) {
                throw new IllegalStateException("No node to decorate: add content first");
            }
            int last = children.size() - 1;
            RichNode node = children.get(last);
            if (node instanceof StyledNode styled) {
                children.set(last, new StyledNode(
                        styled.child(), styled.style(),
                        onClick != null ? onClick : styled.onClick(),
                        onHover != null ? onHover : styled.onHover()
                ));
            } else {
                children.set(last, new StyledNode(node, RichTextStyle.EMPTY, onClick, onHover));
            }
            return this;
        }

        /**
         * Attaches a click action to the most recently added node.
         */
        @Contract("_ -> this")
        public Builder onClickLast(ClickAction action) {
            return decorateLast(action, null);
        }

        /**
         * Attaches a hover action to the most recently added node.
         */
        @Contract("_ -> this")
        public Builder onHoverLast(HoverAction action) {
            return decorateLast(null, action);
        }

        //endregion

        public RichText build() {
            return new RichText(new GroupNode(List.copyOf(children)));
        }
    }
}
