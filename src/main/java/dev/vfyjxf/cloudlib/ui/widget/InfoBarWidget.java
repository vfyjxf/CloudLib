package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;

/**
 * The meter of a target info panel: one bar, one or more segments, optional
 * threshold marks.
 * <p>
 * A single segment is the common case —
 * {@code InfoBarWidget.of(() -> health / maxHealth)} — and a stack of segments
 * covers the compound readings a Jade-style panel wants (health, then the
 * absorption sitting after it, then a shield charge). Segments are laid out in
 * declaration order: each one starts where the previous one ended and owns the
 * fraction of the bar it declares, so {@code 0.6} + {@code 0.2} paints
 * {@code [0.0, 0.6)} and {@code [0.6, 0.8)}, and a stack whose fractions
 * overrun the bar is clipped at the end. {@link #segmentGap} px are taken off
 * every segment but the first, which is the hairline that keeps two readings
 * from reading as one.
 * <p>
 * <b>Semantic colour lives in the theme, never here.</b> A segment carries only
 * a style class ({@link #styleOk}, {@link #styleWarn}, {@link #styleDanger},
 * {@link #styleInfo}, or any name a mod's sheet defines), which lands on that
 * segment's {@code ::part(fill)} node: {@code info-bar .danger::part(fill) {
 * background: var(--bar-danger) }}. The code-side textures are achromatic on
 * purpose — the bar is a meter, and only the sheet knows what "danger" means.
 * <p>
 * Parts: {@code ::part(track)} is the trough, {@code ::part(fill)} one per
 * segment (each carrying its own class), {@code ::part(mark)} one per threshold,
 * {@code ::part(frame)} the ring around the whole bar. The sheet owns the look —
 * every part's texture and the height ({@code info-bar { height: … }}), which the
 * code only declares a fallback for. The frame is the one piece of chrome the code
 * owns a default for: a 1px dark outline drawn <em>over</em> the trough, with
 * {@link #frameThickness} px kept free inside it, so a bar reads as a framed
 * channel even with no sheet in play and a full bar of ink can never cover its own
 * outline. Rounded corners come from whichever texture the sheet picks for a part —
 * a nine-slice with rounded edges, or the {@code sdf(...)} function — and never
 * from the code, which draws rectangles only.
 * <p>
 * A supplier segment is read on every pass — through the parts' layout handler
 * and again right before the children render — so a live value moves the fill
 * with no setter call, no style write and no relayout, exactly like
 * {@link ProgressBarWidget}.
 */
public final class InfoBarWidget extends CompositeWidget<Widget> {

    // region parts

    /** {@code ::part(track)} — the trough the segments sit in. */
    static final String partTrack = "track";
    /** {@code ::part(fill)} — one node per segment, carrying that segment's class. */
    static final String partFill = "fill";
    /** {@code ::part(mark)} — one node per threshold. */
    static final String partMark = "mark";
    /** {@code ::part(frame)} — the ring around the trough. */
    static final String partFrame = "frame";

    // endregion

    // region semantic vocabulary

    /** The class name the shipped sheets paint as "healthy" — {@code info-bar .ok::part(fill)}. */
    public static final String styleOk = "ok";
    /** The class name the shipped sheets paint as "getting low". */
    public static final String styleWarn = "warn";
    /** The class name the shipped sheets paint as "critical". */
    public static final String styleDanger = "danger";
    /** The class name the shipped sheets paint as neutral information. */
    public static final String styleInfo = "info";

    // endregion

    // region fallbacks (achromatic by contract)

    private static final int defaultBarHeight = 7;
    private static final int defaultMarkThickness = 1;
    private static final int defaultFrameThickness = 1;
    private static final int defaultSegmentGap = 1;
    private static final int maxFrameThickness = 3;
    private static final int maxSegmentGap = 3;

    private @Nullable VisualTexture trackTexture = new ColorTexture(0xC0000000);
    private @Nullable VisualTexture fillTexture = new ColorTexture(0xFF9E9E9E);
    private @Nullable VisualTexture markTexture = new ColorTexture(0xFFFFFFFF);
    private @Nullable VisualTexture frameTexture = BorderTexture.of(0xFF000000, defaultFrameThickness);

    // endregion

    // region state

    private final TrackPart trackPart;
    private final FramePart framePart;
    private final List<SegmentPart> fills = new ArrayList<>();
    private final List<MarkPart> marks = new ArrayList<>();

    private int barHeight = defaultBarHeight;
    private int markThickness = defaultMarkThickness;
    private int frameThickness = defaultFrameThickness;
    private int segmentGap = defaultSegmentGap;

    // endregion

    // region factories

    /** A one-segment bar holding {@code fraction} of its width. */
    public static InfoBarWidget of(double fraction) {
        return new InfoBarWidget().setFraction(fraction);
    }

    /** A one-segment bar whose fraction is re-read every frame. */
    public static InfoBarWidget of(DoubleSupplier fraction) {
        return new InfoBarWidget().setFraction(fraction);
    }

    /** A stacked bar — the segments in declaration order, left to right. */
    public static InfoBarWidget segmented(Segment... segments) {
        return new InfoBarWidget().setSegments(List.of(segments));
    }

    /** A stacked bar — the segments in declaration order, left to right. */
    public static InfoBarWidget segmented(List<Segment> segments) {
        return new InfoBarWidget().setSegments(segments);
    }

    private InfoBarWidget() {
        // the trough is the first child, so it paints under every segment; the frame
        // joins last and paints over all of them
        trackPart = addWidget(new TrackPart(this));
        framePart = addWidget(new FramePart(this));
        syncIntrinsicSize();
    }

    // endregion

    // region segments

    /**
     * One segment of a stacked bar: the fraction of the bar it owns and the
     * style class its {@code ::part(fill)} node carries. A null class leaves the
     * segment to the sheet's bare {@code info-bar::part(fill)} rule.
     */
    public record Segment(DoubleSupplier fraction, @Nullable String styleClass) {

        /** A neutral segment — no semantic class. */
        public static Segment of(double fraction) {
            return new Segment(() -> fraction, null);
        }

        /** A fixed segment painted by {@code info-bar .<styleClass>::part(fill)}. */
        public static Segment of(double fraction, String styleClass) {
            return new Segment(() -> fraction, styleClass);
        }

        /** A live segment painted by {@code info-bar .<styleClass>::part(fill)}. */
        public static Segment of(DoubleSupplier fraction, String styleClass) {
            return new Segment(fraction, styleClass);
        }

        /** The segment's share of the bar, clamped to {@code [0, 1]}. */
        public double value() {
            return Math.clamp(fraction.getAsDouble(), 0.0, 1.0);
        }
    }

    public List<Segment> segments() {
        return fills.stream().map(SegmentPart::segment).toList();
    }

    /**
     * The first segment's share — the whole bar in the single-segment case.
     */
    public double fraction() {
        return fills.isEmpty() ? 0.0 : fills.getFirst().fraction();
    }

    /** Replaces every segment with one neutral segment holding {@code fraction}. */
    public InfoBarWidget setFraction(double fraction) {
        return setFraction(() -> fraction);
    }

    /** Replaces every segment with one neutral segment re-read every frame. */
    public InfoBarWidget setFraction(DoubleSupplier fraction) {
        return setSegments(List.of(new Segment(fraction, null)));
    }

    /** Replaces the stack; parts are reused positionally, the rest is added or dropped. */
    public InfoBarWidget setSegments(List<Segment> segments) {
        List<Segment> next = List.copyOf(segments);
        for (int i = 0; i < Math.min(fills.size(), next.size()); i++) {
            fills.get(i).setSegment(next.get(i));
        }
        while (fills.size() > next.size()) {
            remove(fills.removeLast());
        }
        while (fills.size() < next.size()) {
            SegmentPart added = new SegmentPart(this, next.get(fills.size()));
            fills.add(added);
            addWidget(added);
        }
        invalidateParts();
        return this;
    }

    /** Appends one segment after the ones already declared. */
    public InfoBarWidget addSegment(Segment segment) {
        List<Segment> next = new ArrayList<>(segments());
        next.add(segment);
        return setSegments(next);
    }

    /** Appends one segment painted by {@code info-bar .<styleClass>::part(fill)}. */
    public InfoBarWidget addSegment(double fraction, String styleClass) {
        return addSegment(Segment.of(fraction, styleClass));
    }

    // endregion

    // region marks

    /** The thresholds, as positions in {@code [0, 1]} in declaration order. */
    public List<Double> marks() {
        return marks.stream().map(MarkPart::position).toList();
    }

    /** Adds a threshold marker at {@code position} — 0.25 is a quarter in from the left. */
    public InfoBarWidget mark(double position) {
        MarkPart added = new MarkPart(this, position);
        marks.add(added);
        addWidget(added);
        invalidateParts();
        return this;
    }

    /** Drops every marker. */
    public InfoBarWidget clearMarks() {
        for (MarkPart mark : marks) {
            remove(mark);
        }
        marks.clear();
        invalidateParts();
        return this;
    }

    /** The width of a marker slot in px — a 1px tick by default. */
    public int markThickness() {
        return markThickness;
    }

    /** Sets the marker width; kept inside {@code [1, 2]} px, the batchable slot width. */
    public InfoBarWidget setMarkThickness(int thickness) {
        int clamped = Math.max(1, Math.min(2, thickness));
        if (this.markThickness != clamped) {
            this.markThickness = clamped;
            invalidateParts();
        }
        return this;
    }

    // endregion

    // region metrics

    /** The bar height in px when the sheet declares none. */
    public int barHeight() {
        return barHeight;
    }

    /** The fallback height, below the theme — {@code info-bar { height: … }} still wins. */
    public InfoBarWidget setBarHeight(int height) {
        int clamped = Math.max(1, height);
        if (this.barHeight != clamped) {
            this.barHeight = clamped;
            syncIntrinsicSize();
        }
        return this;
    }

    /**
     * The px the ring takes on every side of the bar — {@code 1} by default, and the
     * same number the fills and the markers are inset by.
     */
    public int frameThickness() {
        return frameThickness;
    }

    /**
     * Sets how far the frame cuts into the bar. Kept inside {@code [0, 3]} px:
     * {@code 0} turns the ring off and lets the fills reach the bar's own edges,
     * and past 3 px the frame starts eating the channel it is meant to outline.
     * The sheet's own outline ({@code info-bar::part(frame) { background:
     * border-texture(…, n) }}) has to agree with it, exactly as a nine-slice's
     * border has to agree with the box it paints.
     */
    public InfoBarWidget setFrameThickness(int thickness) {
        int clamped = Math.max(0, Math.min(maxFrameThickness, thickness));
        if (this.frameThickness != clamped) {
            this.frameThickness = clamped;
            invalidateParts();
        }
        return this;
    }

    /** The px between two segments — {@code 1} by default, the hairline that separates two readings. */
    public int segmentGap() {
        return segmentGap;
    }

    /** Sets the segment hairline; kept inside {@code [0, 3]} px, {@code 0} tiling the stack edge to edge. */
    public InfoBarWidget setSegmentGap(int gap) {
        int clamped = Math.max(0, Math.min(maxSegmentGap, gap));
        if (this.segmentGap != clamped) {
            this.segmentGap = clamped;
            invalidateParts();
        }
        return this;
    }

    // endregion

    // region textures

    public @Nullable VisualTexture trackTexture() {
        return trackTexture;
    }

    public InfoBarWidget setTrackTexture(@Nullable VisualTexture texture) {
        this.trackTexture = texture;
        return this;
    }

    public @Nullable VisualTexture fillTexture() {
        return fillTexture;
    }

    public InfoBarWidget setFillTexture(@Nullable VisualTexture texture) {
        this.fillTexture = texture;
        return this;
    }

    public @Nullable VisualTexture markTexture() {
        return markTexture;
    }

    public InfoBarWidget setMarkTexture(@Nullable VisualTexture texture) {
        this.markTexture = texture;
        return this;
    }

    /**
     * The ring around the bar — a {@code BorderTexture} by default, so the outline
     * is there with no sheet in play. A sheet replaces it through
     * {@code info-bar::part(frame)}; the code texture is the fallback, exactly as
     * the trough's and the fills' are.
     */
    public @Nullable VisualTexture frameTexture() {
        return frameTexture;
    }

    public InfoBarWidget setFrameTexture(@Nullable VisualTexture texture) {
        this.frameTexture = texture;
        return this;
    }

    // endregion

    // region geometry (pure — the headless tests drive these)

    /**
     * The px offset of {@code fraction} along the bar. Every edge of every
     * segment goes through this one function, so adjacent segments share an edge
     * exactly: no seams from rounding, and a stack's widths always add up to the
     * width of the whole stack. A negative width reads as an empty bar.
     */
    public static int pixelAt(int width, double fraction) {
        if (width <= 0) {
            return 0;
        }
        double clamped = Math.clamp(fraction, 0.0, 1.0);
        return (int) Math.floor(width * clamped + 1e-6);
    }

    /**
     * A segment's rect: from {@code start} to {@code start + fraction}, both read
     * through {@link #pixelAt}, clipped at the right edge of the bar.
     */
    public static Rect segmentBounds(int width, int height, double start, double fraction) {
        int from = pixelAt(width, start);
        int to = pixelAt(width, start + fraction);
        return new Rect(from, 0, Math.max(0, to - from), height);
    }

    /**
     * The band the segments and the markers are drawn in: the bar inset by
     * {@code border} px on every side, which is exactly the room the frame's ring
     * takes. Every edge of the stack is measured against this band, so a full bar
     * of ink stops at the ring instead of painting over it, and a bar too small to
     * hold its own frame keeps at least a pixel of channel.
     */
    public static Rect contentBounds(int width, int height, int border) {
        int insetX = Math.max(0, Math.min(border, width / 2));
        int insetY = Math.max(0, Math.min(border, height / 2));
        return new Rect(insetX, insetY, Math.max(0, width - 2 * insetX), Math.max(0, height - 2 * insetY));
    }

    /**
     * One segment inside a content band — the {@link #segmentBounds(int, int, double, double)}
     * form, with the band's own origin carried in and {@code leadGap} px trimmed
     * off the segment's leading edge. The gap comes off the leading edge so a
     * segment that is not the stack's first leaves the hairline in front of it,
     * while the last one still reaches the band's trailing edge exactly: a stack
     * keeps tiling the bar, however it is split.
     */
    public static Rect segmentBounds(Rect content, double start, double fraction, int leadGap) {
        int from = pixelAt(content.width(), start) + Math.max(0, leadGap);
        int to = pixelAt(content.width(), start + fraction);
        int left = Math.min(from, to);
        return new Rect(content.x() + left, content.y(), to - left, content.height());
    }

    /**
     * The px offset a stack has accumulated by the time {@code index} starts — the
     * running sum every stack rect is cut at, so two neighbours share their edge
     * exactly and a stack's widths always add up to the width of the whole stack.
     */
    private static double startOf(List<Double> fractions, int index) {
        double start = 0;
        for (int i = 0; i < index && i < fractions.size(); i++) {
            start += Math.clamp(fractions.get(i), 0.0, 1.0);
        }
        return start;
    }

    /** The rect of one segment of a stack, by its index — the running-offset form of {@link #segmentBounds(int, int, double, double)}. */
    public static Rect nthSegmentBounds(int width, int height, List<Double> fractions, int index) {
        double fraction = index >= 0 && index < fractions.size() ? Math.clamp(fractions.get(index), 0.0, 1.0) : 0.0;
        return segmentBounds(width, height, startOf(fractions, index), fraction);
    }

    /**
     * The rect of one segment of a stack laid out inside {@code content} — the band
     * form of {@link #nthSegmentBounds(int, int, List, int)}, with {@code gap} px of
     * the band taken off every segment but the first: the hairline that separates
     * two readings the way {@link #markBounds(Rect, double, int)} separates two
     * thresholds.
     */
    public static Rect nthSegmentBounds(Rect content, List<Double> fractions, int index, int gap) {
        double fraction = index >= 0 && index < fractions.size() ? Math.clamp(fractions.get(index), 0.0, 1.0) : 0.0;
        return segmentBounds(content, startOf(fractions, index), fraction, index == 0 ? 0 : gap);
    }

    /** A threshold marker: a vertical slot of {@code thickness} px, centred on {@code position}. */
    public static Rect markBounds(int width, int height, double position, int thickness) {
        if (width <= 0) {
            return new Rect(0, 0, 0, height);
        }
        int slot = Math.max(1, Math.min(thickness, width));
        int centred = pixelAt(width, position) - slot / 2;
        return new Rect(Math.max(0, Math.min(centred, width - slot)), 0, slot, height);
    }

    /**
     * A threshold marker inside a content band: the {@link #markBounds(int, int, double, int)}
     * slot, spanning the band's height — a marker that stood on the bar's own edge
     * would read as part of the frame.
     */
    public static Rect markBounds(Rect content, double position, int thickness) {
        if (content.width() <= 0) {
            return new Rect(content.x(), content.y(), 0, content.height());
        }
        int slot = Math.max(1, Math.min(thickness, content.width()));
        int centred = pixelAt(content.width(), position) - slot / 2;
        int clamped = Math.max(0, Math.min(centred, content.width() - slot));
        return new Rect(content.x() + clamped, content.y(), slot, content.height());
    }

    // endregion

    // region layout

    /**
     * The height the bar falls back to. The parts make this node a taffy
     * <em>container</em>, and taffy only consults a measure function on a node
     * without children — so the fallback is declared as a default style instead,
     * which sits below the theme: {@code info-bar { height: … }} overrides it,
     * exactly like {@link SlotGridWidget} declares its grid size.
     */
    private void syncIntrinsicSize() {
        defaultStyle(UIStyle.of(UIStyles.heightOf(barHeight)));
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    /** The band the segments and the markers live in — the bar inside its own frame. */
    private Rect contentBounds() {
        return contentBounds(width(), height(), frameThickness);
    }

    /** The rect of one segment — derived from the stack and the segment's place in it. */
    private Rect boundsOf(SegmentPart part) {
        int index = fills.indexOf(part);
        if (index < 0) {
            return Rect.empty;
        }
        double start = 0;
        for (int i = 0; i < index; i++) {
            start += fills.get(i).fraction();
        }
        return segmentBounds(contentBounds(), start, part.fraction(), index == 0 ? 0 : segmentGap);
    }

    /** The rect of one marker. */
    private Rect boundsOf(MarkPart part) {
        return markBounds(contentBounds(), part.position(), markThickness);
    }

    // endregion

    // region parts

    /**
     * Paint order: the trough under everything, the segments over it, the
     * markers over the segments, and the frame over all of them — a full bar of
     * ink can then never cover the outline that frames it.
     * <p>
     * Order is expressed as a z-index rather than as child order, because a
     * segment declared after a marker has to keep painting under it — and a
     * widget joined to the tree later has no way to be inserted "before" its
     * siblings (the child API only appends).
     */
    private static final int trackZIndex = 0;
    private static final int fillZIndex = 1;
    private static final int markZIndex = 2;
    private static final int frameZIndex = 3;

    /** The trough — the whole widget. */
    private static final class TrackPart extends WidgetPart {

        private final InfoBarWidget owner;

        TrackPart(InfoBarWidget owner) {
            super(owner, partTrack, null, () -> owner.trackTexture);
            this.owner = owner;
            useStyle(UIStyle.of(UIStyles.zIndex(trackZIndex)));
        }

        @Override
        protected Rect resolveBounds() {
            return new Rect(0, 0, owner.width(), owner.height());
        }
    }

    /**
     * The ring around the trough. It paints the widget's whole rect and lets its
     * texture decide which pixels are ink — a {@code BorderTexture} strokes the
     * four sides at the rect's edge, which is exactly the outline, and it stays
     * one batchable quad however thick the frame is. A frame thickness of 0 takes
     * the code ring away with the inset it belongs to (a sheet that still paints
     * {@code ::part(frame)} keeps whatever it paints: that is the sheet's call).
     */
    private static final class FramePart extends WidgetPart {

        private final InfoBarWidget owner;

        FramePart(InfoBarWidget owner) {
            super(owner, partFrame, null, () -> owner.frameThickness == 0 ? null : owner.frameTexture);
            this.owner = owner;
            useStyle(UIStyle.of(UIStyles.zIndex(frameZIndex)));
        }

        @Override
        protected Rect resolveBounds() {
            return new Rect(0, 0, owner.width(), owner.height());
        }
    }

    /**
     * One segment. Every segment shares the {@code fill} part name — they are the
     * same role repeated — and is told apart by its own style class.
     */
    private static final class SegmentPart extends WidgetPart {

        private final InfoBarWidget owner;
        private Segment segment;

        SegmentPart(InfoBarWidget owner, Segment segment) {
            super(owner, partFill, null, () -> owner.fillTexture);
            this.owner = owner;
            this.segment = segment;
            useStyle(UIStyle.of(UIStyles.zIndex(fillZIndex)));
            if (segment.styleClass() != null) {
                addStyleClass(segment.styleClass());
            }
        }

        @Override
        protected Rect resolveBounds() {
            return owner.boundsOf(this);
        }

        Segment segment() {
            return segment;
        }

        double fraction() {
            return segment.value();
        }

        /** Swaps the semantic class — the class is what the sheet selects the segment by. */
        void setSegment(Segment segment) {
            String previous = this.segment.styleClass();
            String next = segment.styleClass();
            if (!Objects.equals(previous, next)) {
                if (previous != null) {
                    removeStyleClass(previous);
                }
                if (next != null) {
                    addStyleClass(next);
                }
            }
            this.segment = segment;
        }
    }

    /** One threshold marker. */
    private static final class MarkPart extends WidgetPart {

        private final InfoBarWidget owner;
        private final double position;

        MarkPart(InfoBarWidget owner, double position) {
            super(owner, partMark, null, () -> owner.markTexture);
            this.owner = owner;
            this.position = Math.clamp(position, 0.0, 1.0);
            useStyle(UIStyle.of(UIStyles.zIndex(markZIndex)));
        }

        @Override
        protected Rect resolveBounds() {
            return owner.boundsOf(this);
        }

        double position() {
            return position;
        }
    }

    // endregion

    // region hooks

    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        // a supplier can move a segment or the bar can be resized with no layout
        // change of our own — re-read every part's rect before the canvas
        // translates to them
        WidgetPart.syncAll(this);
        super.render(canvas, mouseX, mouseY, partialTicks);
    }

    /** The parts mirror this widget's selector surface — a state flip has to re-resolve them too. */
    @Override
    public void markStyleDirty() {
        super.markStyleDirty();
        WidgetPart.markStyleDirtyAll(this);
    }

    /** Re-runs the parts' layout handlers after a change taffy cannot see. */
    private void invalidateParts() {
        if (!lifecycle().mounted()) {
            return;
        }
        for (Widget part : children()) {
            // a part queued for creation this frame has no node yet — the next
            // pass mounts it and lays it out
            if (part.lifecycle().mounted()) {
                scene().layoutTree().markDirty(part.nodeId());
            }
        }
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("segments", fills.size(), InspectionProperty.categoryData);
        if (!fills.isEmpty()) {
            collector.add(
                "fractions",
                fills.stream().map(part -> String.format("%.1f%%", part.fraction() * 100)).toList(),
                InspectionProperty.categoryData
            );
        }
        collector.addWithDefault("marks", marks(), List.of(), InspectionProperty.categoryData);
        collector.addWithDefault("barHeight", barHeight, defaultBarHeight, InspectionProperty.categoryLayout);
        collector.addWithDefault(
            "frameThickness",
            frameThickness,
            defaultFrameThickness,
            InspectionProperty.categoryLayout
        );
        collector.addWithDefault("segmentGap", segmentGap, defaultSegmentGap, InspectionProperty.categoryLayout);
        collector.add("track", trackPart.bounds(), InspectionProperty.categoryLayout);
        collector.add("frame", framePart.bounds(), InspectionProperty.categoryLayout);
    }

    // endregion
}
