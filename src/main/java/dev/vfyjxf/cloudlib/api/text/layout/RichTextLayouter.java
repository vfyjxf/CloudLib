package dev.vfyjxf.cloudlib.api.text.layout;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.text.BlockNode;
import dev.vfyjxf.cloudlib.api.text.BreakNode;
import dev.vfyjxf.cloudlib.api.text.ClickAction;
import dev.vfyjxf.cloudlib.api.text.ComponentAdapter;
import dev.vfyjxf.cloudlib.api.text.ComponentNode;
import dev.vfyjxf.cloudlib.api.text.CustomRenderNode;
import dev.vfyjxf.cloudlib.api.text.EntityNode;
import dev.vfyjxf.cloudlib.api.text.GroupNode;
import dev.vfyjxf.cloudlib.api.text.HoverAction;
import dev.vfyjxf.cloudlib.api.text.ImageNode;
import dev.vfyjxf.cloudlib.api.text.ItemNode;
import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.text.RichTextStyle;
import dev.vfyjxf.cloudlib.api.text.SpacerNode;
import dev.vfyjxf.cloudlib.api.text.StyledNode;
import dev.vfyjxf.cloudlib.api.text.TextNode;
import dev.vfyjxf.cloudlib.api.text.TranslatableNode;
import dev.vfyjxf.cloudlib.api.text.VerticalAlign;
import dev.vfyjxf.cloudlib.api.text.WidgetNode;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The rich text layout engine.
 * <p>
 * Layout runs in three phases, following CSS inline-layout semantics:
 * <ol>
 *   <li><b>flatten</b> — the node tree is walked with an inherited-style context and
 *       reduced to a sequence of inline atoms: words (whitespace-delimited text runs),
 *       breakable spaces, replaced objects (images, items, blocks, entities, widgets,
 *       custom boxes) and explicit breaks. Vanilla components are resolved through
 *       the vanilla component pipeline; translatable nodes splice their arguments at
 *       the placeholder positions.</li>
 *   <li><b>break</b> — atoms are greedily packed into lines of at most the
 *       constrained width; overlong words fall back to per-codepoint breaking, and
 *       leading whitespace of a fresh line is dropped.</li>
 *   <li><b>assemble</b> — each line gets its box height (max of the text line height
 *       and object heights), fragments are vertically aligned per their
 *       {@link VerticalAlign} and lines are horizontally aligned per
 *       {@link LayoutConstraints#alignment()}.</li>
 * </ol>
 * The engine is pure Java (only vanilla data types like {@link Style} appear), so it
 * is fully unit-testable with a fake {@link GlyphMeasurer}.
 */
public final class RichTextLayouter {

    /**
     * Distance from the bottom of the text line box to the text baseline.
     */
    private static final int textDescent = 2;

    private final GlyphMeasurer measurer;
    private final TranslationResolver translations;

    public RichTextLayouter(GlyphMeasurer measurer, TranslationResolver translations) {
        this.measurer = measurer;
        this.translations = translations;
    }

    public GlyphMeasurer measurer() {
        return measurer;
    }

    public TranslationResolver translations() {
        return translations;
    }

    //region public API

    /**
     * Lays out the given document under the given constraints.
     */
    public LaidOutText layout(RichNode root, LayoutConstraints constraints) {
        List<InlineAtom> atoms = new ArrayList<>();
        flattenInto(root, FlattenContext.root, atoms);
        return assemble(breakLines(atoms, constraints.maxWidth()), constraints);
    }

    /**
     * The width of the longest natural (unwrapped) line — the CSS max-content size.
     */
    public float maxContentWidth(RichNode root) {
        List<InlineAtom> atoms = new ArrayList<>();
        flattenInto(root, FlattenContext.root, atoms);
        float max = 0;
        float current = 0;
        for (InlineAtom atom : atoms) {
            if (atom instanceof BreakAtom) {
                max = Math.max(max, current);
                current = 0;
            } else {
                current += atom.width();
            }
        }
        return Math.max(max, current);
    }

    /**
     * The width of the longest unbreakable unit (word or object) — the CSS
     * min-content size.
     */
    public float minContentWidth(RichNode root) {
        List<InlineAtom> atoms = new ArrayList<>();
        flattenInto(root, FlattenContext.root, atoms);
        float max = 0;
        for (InlineAtom atom : atoms) {
            if (atom instanceof WordAtom || atom instanceof ObjectAtom) {
                max = Math.max(max, atom.width());
            }
        }
        return max;
    }

    //endregion

    //region flatten

    /**
     * The inherited context while walking the node tree.
     */
    private record FlattenContext(RichTextStyle style, @Nullable ClickAction click, @Nullable HoverAction hover) {
        static final FlattenContext root = new FlattenContext(RichTextStyle.empty, null, null);

        FlattenContext merge(StyledNode node) {
            return new FlattenContext(
                style.merge(node.style()),
                node.onClick() != null ? node.onClick() : click,
                node.onHover() != null ? node.onHover() : hover
            );
        }

        ClickAction effectiveClick() {
            if (click != null) return click;
            ClickEvent event = style.style().getClickEvent();
            return event != null ? new ClickAction.Vanilla(event) : null;
        }

        HoverAction effectiveHover() {
            if (hover != null) return hover;
            HoverEvent event = style.style().getHoverEvent();
            return event != null ? new HoverAction.Vanilla(event) : null;
        }
    }

    private void flattenInto(RichNode node, FlattenContext context, List<InlineAtom> out) {
        switch (node) {
            case StyledNode styled -> flattenInto(styled.child(), context.merge(styled), out);
            case GroupNode group -> {
                for (RichNode child : group.children()) {
                    flattenInto(child, context, out);
                }
            }
            case TextNode text -> splitText(text.text(), context, text, out);
            case ComponentNode component -> flattenComponent(component.component(), context, component, out);
            case TranslatableNode translatable -> flattenTranslatable(translatable, context, out);
            case BreakNode ignored -> out.add(BreakAtom.instance);
            case ImageNode image -> out.add(objectAtom(image, image.width(), image.height(), context));
            case ItemNode item -> out.add(objectAtom(item, item.size(), item.size(), context));
            case BlockNode block -> out.add(objectAtom(block, block.size(), block.size(), context));
            case EntityNode entity -> out.add(objectAtom(entity, entity.width(), entity.height(), context));
            case WidgetNode widget -> out.add(objectAtom(widget, widget.width(), widget.height(), context));
            case CustomRenderNode custom -> out.add(objectAtom(custom, custom.width(), custom.height(), context));
            case SpacerNode spacer -> out.add(objectAtom(spacer, spacer.width(), 0, context));
        }
    }

    private ObjectAtom objectAtom(RichNode node, int width, int height, FlattenContext context) {
        Insets padding = context.style.paddingOr(Insets.zero);
        return new ObjectAtom(
            node,
            width + padding.left() + padding.right(),
            height + padding.top() + padding.bottom(),
            context.style,
            context.effectiveClick(),
            context.effectiveHover()
        );
    }

    private void flattenComponent(Component component, FlattenContext context, RichNode source, List<InlineAtom> out) {
        ComponentAdapter.flatten(component, context.style.style(), (text, style) -> {
            // `style` arrives fully resolved (the inherited style is already applied
            // by the vanilla visit); only the CloudLib extras inherit.
            RichTextStyle effective = new RichTextStyle(
                style,
                context.style.shadow(),
                context.style.highlightColor(),
                context.style.verticalAlign(),
                context.style.padding(),
                context.style.colorVar()
            );
            FlattenContext segmentContext = new FlattenContext(effective, context.click(), context.hover());
            splitText(text, segmentContext, source, out);
        });
    }

    private static final char placeholder = '%';

    /**
     * Splices a translation pattern with its arguments into atoms. Supports
     * {@code %s} (sequential), {@code %n$s} (indexed) and {@code %%}; unrecognized
     * or unsatisfied specifiers are emitted literally, mirroring vanilla.
     */
    private void flattenTranslatable(TranslatableNode node, FlattenContext context, List<InlineAtom> out) {
        String pattern = translations.pattern(node.key()).orElse(node.key());
        List<Object> args = node.args();
        StringBuilder text = new StringBuilder();
        int sequential = 0;
        int i = 0;
        int n = pattern.length();
        while (i < n) {
            char c = pattern.charAt(i);
            if (c == placeholder && i + 1 < n) {
                char next = pattern.charAt(i + 1);
                if (next == placeholder) {
                    text.append(placeholder);
                    i += 2;
                    continue;
                }
                int argIndex = -1;
                int consumed = 0;
                if (next == 's') {
                    argIndex = sequential++;
                    consumed = 2;
                } else if (Character.isDigit(next)) {
                    int j = i + 1;
                    while (j < n && Character.isDigit(pattern.charAt(j))) j++;
                    if (j + 1 < n && pattern.charAt(j) == '$' && pattern.charAt(j + 1) == 's') {
                        try {
                            argIndex = Integer.parseInt(pattern.substring(i + 1, j)) - 1;
                            consumed = j + 2 - i;
                        } catch (NumberFormatException ignored) {
                            // fall through: emit literally
                        }
                    }
                }
                if (argIndex >= 0) {
                    if (text.length() > 0) {
                        splitText(text.toString(), context, node, out);
                        text.setLength(0);
                    }
                    if (argIndex < args.size()) {
                        spliceArg(args.get(argIndex), context, node, out);
                    } else {
                        // Unsatisfied placeholder renders literally, like vanilla.
                        splitText(pattern.substring(i, i + consumed), context, node, out);
                    }
                    i += consumed;
                    continue;
                }
            }
            text.append(c);
            i++;
        }
        if (text.length() > 0) {
            splitText(text.toString(), context, node, out);
        }
    }

    private void spliceArg(Object arg, FlattenContext context, TranslatableNode source, List<InlineAtom> out) {
        switch (arg) {
            case null -> splitText("null", context, source, out);
            case RichNode node -> flattenInto(node, context, out);
            case Component component -> flattenComponent(component, context, source, out);
            default -> splitText(String.valueOf(arg), context, source, out);
        }
    }

    /**
     * Splits text into word / space / break atoms. Words are delimited by spaces;
     * {@code \n} produces explicit breaks.
     */
    private void splitText(String text, FlattenContext context, RichNode source, List<InlineAtom> out) {
        int n = text.length();
        int i = 0;
        StringBuilder word = new StringBuilder();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\n') {
                emitWord(word, context, source, out);
                out.add(BreakAtom.instance);
                i++;
            } else if (c == ' ') {
                emitWord(word, context, source, out);
                int start = i;
                while (i < n && text.charAt(i) == ' ') i++;
                int count = i - start;
                int spaceWidth = measurer.width(" ", context.style.style());
                out.add(
                    new SpaceAtom(
                        spaceWidth * count,
                        context.style,
                        context.effectiveClick(),
                        context.effectiveHover(),
                        source
                    )
                );
            } else {
                word.append(c);
                i++;
            }
        }
        emitWord(word, context, source, out);
    }

    private void emitWord(StringBuilder word, FlattenContext context, RichNode source, List<InlineAtom> out) {
        if (word.length() == 0) return;
        String text = word.toString();
        word.setLength(0);
        out.add(
            new WordAtom(
                text,
                measurer.width(text, context.style.style()),
                context.style,
                context.effectiveClick(),
                context.effectiveHover(),
                source
            )
        );
    }

    //endregion

    //region atoms

    private sealed interface InlineAtom {

        float width();
    }

    private record WordAtom(
        String text,
        float width,
        RichTextStyle style,
        @Nullable ClickAction onClick,
        @Nullable HoverAction onHover,
        RichNode source
    ) implements InlineAtom {}

    private record SpaceAtom(
        float width,
        RichTextStyle style,
        @Nullable ClickAction onClick,
        @Nullable HoverAction onHover,
        RichNode source
    ) implements InlineAtom {}

    private record ObjectAtom(
        RichNode node,
        float width,
        float height,
        RichTextStyle style,
        @Nullable ClickAction onClick,
        @Nullable HoverAction onHover
    ) implements InlineAtom {}

    private record BreakAtom() implements InlineAtom {

        static final BreakAtom instance = new BreakAtom();

        @Override
        public float width() {
            return 0;
        }
    }

    //endregion

    //region line breaking

    private static final class LineBuild {

        final List<PlacedAtom> atoms = new ArrayList<>();
        float width;
        boolean hadBreak;

        boolean isEmpty() {
            return atoms.isEmpty();
        }
    }

    /**
     * An atom with its horizontal position within a line, before vertical assembly.
     */
    private record PlacedAtom(InlineAtom atom, float x) {}

    private List<LineBuild> breakLines(List<InlineAtom> atoms, int maxWidth) {
        List<LineBuild> lines = new ArrayList<>();
        LineBuild current = new LineBuild();
        boolean lastWasBreak = false;
        for (InlineAtom atom : atoms) {
            switch (atom) {
                case BreakAtom ignored -> {
                    current.hadBreak = true;
                    commitLine(lines, current);
                    current = new LineBuild();
                    lastWasBreak = true;
                }
                case SpaceAtom space -> {
                    lastWasBreak = false;
                    if (current.isEmpty()) {
                        // Leading whitespace of a fresh line is dropped.
                        continue;
                    }
                    if (current.width + space.width() > maxWidth) {
                        // The space triggering a break is consumed by the break.
                        commitLine(lines, current);
                        current = new LineBuild();
                        continue;
                    }
                    place(current, space);
                }
                case WordAtom word -> {
                    lastWasBreak = false;
                    if (current.width + word.width() <= maxWidth) {
                        place(current, word);
                    } else {
                        if (!current.isEmpty()) {
                            commitLine(lines, current);
                            current = new LineBuild();
                        }
                        if (word.width() <= maxWidth) {
                            place(current, word);
                        } else {
                            current = breakWord(word, maxWidth, lines, current);
                        }
                    }
                }
                case ObjectAtom object -> {
                    lastWasBreak = false;
                    if (current.width + object.width() > maxWidth && !current.isEmpty()) {
                        commitLine(lines, current);
                        current = new LineBuild();
                    }
                    place(current, object);
                }
            }
        }
        if (!current.isEmpty() || lastWasBreak) {
            commitLine(lines, current);
        }
        return lines;
    }

    /**
     * Commits a line: trailing whitespace collapses at line ends (CSS white-space
     * semantics), so it neither renders nor counts toward the line width.
     */
    private static void commitLine(List<LineBuild> lines, LineBuild line) {
        while (!line.atoms.isEmpty()) {
            int last = line.atoms.size() - 1;
            if (line.atoms.get(last).atom() instanceof SpaceAtom space) {
                line.width -= space.width();
                line.atoms.remove(last);
            } else {
                break;
            }
        }
        lines.add(line);
    }

    private void place(LineBuild line, InlineAtom atom) {
        line.atoms.add(new PlacedAtom(atom, line.width));
        line.width += atom.width();
    }

    /**
     * Breaks an overlong word per codepoint, filling the current line first.
     *
     * @return the line the remainder of the word landed in
     */
    private LineBuild breakWord(WordAtom word, int maxWidth, List<LineBuild> lines, LineBuild current) {
        String remaining = word.text();
        while (!remaining.isEmpty()) {
            int fit = countFitting(remaining, word.style(), maxWidth - current.width);
            if (fit <= 0) {
                lines.add(current);
                current = new LineBuild();
                continue;
            }
            String part = remaining.substring(0, fit);
            place(
                current,
                new WordAtom(
                    part,
                    measurer.width(part, word.style().style()),
                    word.style(),
                    word.onClick(),
                    word.onHover(),
                    word.source()
                )
            );
            remaining = remaining.substring(fit);
            if (!remaining.isEmpty()) {
                lines.add(current);
                current = new LineBuild();
            }
        }
        return current;
    }

    /**
     * Counts how many leading characters of {@code text} fit into {@code available}
     * pixels. Returns at least one codepoint when anything fits at all, so breaking
     * always makes progress.
     */
    private int countFitting(String text, RichTextStyle style, float available) {
        if (available <= 0) return 0;
        float used = 0;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            float width = measurer.width(new String(Character.toChars(codePoint)), style.style());
            if (used + width > available) {
                return offset == 0 ? charCount : offset;
            }
            used += width;
            offset += charCount;
        }
        return offset;
    }

    //endregion

    //region assembly

    private LaidOutText assemble(List<LineBuild> builds, LayoutConstraints constraints) {
        if (builds.isEmpty()) return LaidOutText.empty;
        int textHeight = measurer.lineHeight();

        float totalWidth = 0;
        float[] lineHeights = new float[builds.size()];
        for (int i = 0; i < builds.size(); i++) {
            LineBuild build = builds.get(i);
            float height = textHeight;
            for (PlacedAtom placed : build.atoms) {
                if (placed.atom() instanceof ObjectAtom object) {
                    height = Math.max(height, object.height());
                }
            }
            lineHeights[i] = height;
            totalWidth = Math.max(totalWidth, build.width);
        }

        float referenceWidth = constraints.constrained() ? constraints.maxWidth() : totalWidth;
        int lineSpacing = constraints.lineSpacing();

        List<TextLine> lines = new ArrayList<>(builds.size());
        float y = 0;
        for (int i = 0; i < builds.size(); i++) {
            LineBuild build = builds.get(i);
            float lineHeight = lineHeights[i];
            float xStart = switch (constraints.alignment()) {
                case left -> 0;
                case center -> Math.max(0, (referenceWidth - build.width) / 2f);
                case right -> Math.max(0, referenceWidth - build.width);
            };

            List<TextFragment> fragments = new ArrayList<>(build.atoms.size());
            for (PlacedAtom placed : build.atoms) {
                fragments.add(materialize(placed, xStart, y, lineHeight, textHeight));
            }
            lines.add(new TextLine(y, build.width, lineHeight, fragments));
            y += lineHeight + lineSpacing;
        }

        float totalHeight = y - (builds.isEmpty() ? 0 : lineSpacing);
        float width = constraints.constrained() ? referenceWidth : totalWidth;
        return new LaidOutText(width, totalHeight, lines);
    }

    private TextFragment materialize(PlacedAtom placed, float xStart, float lineY, float lineHeight, int textHeight) {
        InlineAtom atom = placed.atom();
        float x = xStart + placed.x();
        return switch (atom) {
            case WordAtom word -> new TextFragment(
                TextFragment.Kind.text,
                x,
                lineY + lineHeight - textHeight,
                word.width(),
                textHeight,
                word.text(),
                word.style(),
                word.source(),
                word.onClick(),
                word.onHover()
            );
            case SpaceAtom space -> new TextFragment(
                TextFragment.Kind.space,
                x,
                lineY + lineHeight - textHeight,
                space.width(),
                textHeight,
                null,
                space.style(),
                space.source(),
                space.onClick(),
                space.onHover()
            );
            case ObjectAtom object -> {
                float height = object.height();
                float offsetY = switch (object.style().verticalAlignOr(VerticalAlign.baseline)) {
                    // MC glyphs sit on a baseline ~2px above the line bottom, which is
                    // where inline objects look right as well.
                    case baseline, bottom -> lineHeight - height;
                    case top -> 0;
                    case middle -> (lineHeight - height) / 2f;
                };
                yield new TextFragment(
                    TextFragment.Kind.object,
                    x,
                    lineY + offsetY,
                    object.width(),
                    height,
                    null,
                    object.style(),
                    object.node(),
                    object.onClick(),
                    object.onHover()
                );
            }
            case BreakAtom ignored -> throw new IllegalStateException("Break atoms never reach assembly");
        };
    }

    //endregion
}
