package dev.vfyjxf.cloudlib.api.text;


/**
 * A node of a rich text document.
 * <p>
 * Nodes are pure content: styling and interaction are applied by wrapping a node in
 * a {@link StyledNode}, and sequences are built with {@link GroupNode}. This keeps
 * the node kinds orthogonal and makes style inheritance uniform.
 * <p>
 * Textual nodes ({@link TextNode}, {@link ComponentNode}, {@link TranslatableNode})
 * flow through the line breaker; object nodes ({@link ImageNode}, {@link ItemNode},
 * {@link BlockNode}, {@link EntityNode}, {@link WidgetNode}, {@link CustomRenderNode},
 * {@link SpacerNode}) are laid out as replaced elements with a fixed reserved box,
 * following CSS inline-block semantics.
 *
 * @see RichText
 */
public sealed interface RichNode permits
        TextNode, ComponentNode, TranslatableNode,
        ImageNode, ItemNode, BlockNode, EntityNode, WidgetNode,
        SpacerNode, BreakNode, CustomRenderNode,
        GroupNode, StyledNode {

    /**
     * @return {@code true} when this subtree contains only text (literal, component
     * or translatable content) and can be converted to a vanilla
     * {@link net.minecraft.network.chat.Component}.
     */
    default boolean isTextual() {
        return switch (this) {
            case TextNode ignored -> true;
            case ComponentNode ignored -> true;
            case TranslatableNode node -> node.args().stream().allMatch(RichText::isTextualArg);
            case GroupNode node -> node.children().stream().allMatch(RichNode::isTextual);
            case StyledNode node -> node.child().isTextual();
            default -> false;
        };
    }
}
