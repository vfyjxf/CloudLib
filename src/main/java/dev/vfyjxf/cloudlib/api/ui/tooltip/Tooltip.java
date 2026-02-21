package dev.vfyjxf.cloudlib.api.ui.tooltip;

import com.mojang.datafixers.util.Either;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static dev.vfyjxf.cloudlib.api.ui.tooltip.TooltipLocator.At;
import static dev.vfyjxf.cloudlib.api.ui.tooltip.TooltipLocator.Head;
import static dev.vfyjxf.cloudlib.api.ui.tooltip.TooltipLocator.MatchPriority;
import static dev.vfyjxf.cloudlib.api.ui.tooltip.TooltipLocator.Relative;
import static dev.vfyjxf.cloudlib.api.ui.tooltip.TooltipLocator.Tail;

/**
 * A flat, ordered list of tooltip entries, each tagged with a marker.
 * <p>
 * Entries are stored and rendered in insertion order. When no marker is specified,
 * entries default to {@link #body}.
 *
 * <h3>Quick usage:</h3>
 * <pre>{@code
 * Tooltip tooltip = new Tooltip()
 *     .add(Component.literal("Hello"))
 *     .add(Component.literal("World"));
 * }</pre>
 *
 * <h3>Explicit markers:</h3>
 * <pre>{@code
 * Tooltip tooltip = new Tooltip();
 * tooltip.add(Component.literal("Sword of Fire"), Tooltip.title);
 * tooltip.add(Component.literal("Damage: +10"), TooltipMarker.of(Namespace.of("mymod","stats")));
 * tooltip.add(Component.literal("Hold Shift for more"), Tooltip.footer);
 * }</pre>
 *
 * <h3>Transforms:</h3>
 * <pre>{@code
 * tooltip.addTransform(t ->
 *     t.add(Component.literal("Injected by plugin"), Tooltip.extra)
 * );
 * }</pre>
 *
 * @see TooltipEntry
 */
public final class Tooltip {

    //region built-in markers

    /**
     * Title section — typically rendered at the very top.
     */
    public static final Namespace title = Namespace.of(Constants.namespace, "title");

    /**
     * Description section — rendered below the title.
     */
    public static final Namespace description = Namespace.of(Constants.namespace, "description");

    /**
     * Default marker used by quick {@link #add(Component)} calls.
     */
    public static final Namespace body = Namespace.of(Constants.namespace, "body");

    /**
     * Extra information section — rendered below the body.
     */
    public static final Namespace extra = Namespace.of(Constants.namespace, "extra");

    /**
     * Footer section — typically rendered at the very bottom.
     */
    public static final Namespace footer = Namespace.of(Constants.namespace, "footer");

    //endregion

    //region sentinel

    private record TaggedEntry(TooltipEntry entry, Namespace marker) {}

    //endregion

    private final MutableList<TaggedEntry> entries;
    private final MutableList<Consumer<Tooltip>> transforms;
    private @Nullable TooltipStack<?> stack;


    public static Tooltip create() {
        return new Tooltip();
    }

    public Tooltip() {
        this.entries = MutableLists.empty();
        this.transforms = MutableLists.empty();
        this.stack = null;
    }

    private Tooltip(
        MutableList<TaggedEntry> entries,
        MutableList<Consumer<Tooltip>> transforms,
        @Nullable TooltipStack<?> stack
    ) {
        this.entries = entries;
        this.transforms = transforms;
        this.stack = stack;
    }

    //region add (body marker defaults)

    @Contract("_ -> this")
    public Tooltip add(Component text) {
        return add(new TooltipEntry.TextEntry(text), body);
    }

    @Contract("_ -> this")
    public Tooltip add(TooltipComponent component) {
        return add(new TooltipEntry.ComponentEntry(component), body);
    }

    @Contract("_ -> this")
    public Tooltip add(Supplier<Component> provider) {
        return add(new TooltipEntry.DynamicEntry(provider), body);
    }

    @Contract("_ -> this")
    public Tooltip add(TooltipEntry entry) {
        return add(entry, body);
    }

    //endregion

    //region add (explicit marker)

    @Contract("_, _ -> this")
    public Tooltip add(Component text, Namespace marker) {
        return add(new TooltipEntry.TextEntry(text), marker);
    }

    @Contract("_, _ -> this")
    public Tooltip add(TooltipComponent component, Namespace marker) {
        return add(new TooltipEntry.ComponentEntry(component), marker);
    }

    @Contract("_, _ -> this")
    public Tooltip add(Supplier<Component> provider, Namespace marker) {
        return add(new TooltipEntry.DynamicEntry(provider), marker);
    }

    @Contract("_, _ -> this")
    public Tooltip add(TooltipEntry entry, Namespace marker) {
        entries.add(new TaggedEntry(entry, marker));
        return this;
    }

    //endregion

    //region insert (locator-based)

    /**
     * Inserts an entry at the position determined by the given locator, tagged with {@link #body}.
     *
     * @see TooltipLocator
     */
    @Contract("_, _ -> this")
    public Tooltip insert(Component text, TooltipLocator locator) {
        return insert(new TooltipEntry.TextEntry(text), body, locator);
    }

    @Contract("_, _ -> this")
    public Tooltip insert(TooltipComponent component, TooltipLocator locator) {
        return insert(new TooltipEntry.ComponentEntry(component), body, locator);
    }

    @Contract("_, _ -> this")
    public Tooltip insert(Supplier<Component> provider, TooltipLocator locator) {
        return insert(new TooltipEntry.DynamicEntry(provider), body, locator);
    }

    @Contract("_, _ -> this")
    public Tooltip insert(TooltipEntry entry, TooltipLocator locator) {
        return insert(entry, body, locator);
    }

    /**
     * Inserts an entry at the position determined by the given locator, tagged with the given marker.
     * <p>
     * Examples:
     * <pre>{@code
     * // Insert before the first "body" entry
     * tooltip.insert(Component.literal("Stats"), Tooltip.extra, TooltipLocator.before(Tooltip.body));
     *
     * // Insert after the last "title" entry with offset
     * tooltip.insert(entry, Tooltip.description, TooltipLocator.after(Tooltip.title).offset(1));
     *
     * // Insert at a specific index
     * tooltip.insert(entry, TooltipLocator.at(3));
     * }</pre>
     */
    @Contract("_, _, _ -> this")
    public Tooltip insert(Component text, Namespace marker, TooltipLocator locator) {
        return insert(new TooltipEntry.TextEntry(text), marker, locator);
    }

    @Contract("_, _, _ -> this")
    public Tooltip insert(TooltipComponent component, Namespace marker, TooltipLocator locator) {
        return insert(new TooltipEntry.ComponentEntry(component), marker, locator);
    }

    @Contract("_, _, _ -> this")
    public Tooltip insert(Supplier<Component> provider, Namespace marker, TooltipLocator locator) {
        return insert(new TooltipEntry.DynamicEntry(provider), marker, locator);
    }

    @Contract("_, _, _ -> this")
    public Tooltip insert(TooltipEntry entry, Namespace marker, TooltipLocator locator) {
        int index = resolveIndex(locator);
        entries.add(index, new TaggedEntry(entry, marker));
        return this;
    }

    //endregion

    //region mark / remove

    /**
     * Reassigns the marker for all entries matching {@code matcher}.
     * <p>
     * Example:
     * <pre>{@code
     * tooltip.mark(
     *     e -> e instanceof TooltipEntry.TextEntry(var t) && t.getString().startsWith("[Title]"),
     *     Tooltip.title
     * );
     * }</pre>
     */
    @Contract("_, _ -> this")
    public Tooltip mark(Predicate<TooltipEntry> matcher, Namespace marker) {
        entries.replaceAll(tagged -> matcher.test(tagged.entry()) ? new TaggedEntry(tagged.entry(), marker) : tagged);
        return this;
    }

    /**
     * Reassigns the marker for all existing entries carrying {@code oldMarker} to {@code newMarker}.
     */
    @Contract("_, _ -> this")
    public Tooltip remark(Namespace oldMarker, Namespace newMarker) {
        entries.replaceAll(tagged -> tagged.marker.equals(oldMarker) ? new TaggedEntry(tagged.entry(), newMarker) : tagged);
        return this;
    }

    /**
     * Removes all entries matching {@code matcher}.
     */
    @Contract("_ -> this")
    public Tooltip remove(Predicate<TooltipEntry> matcher) {
        entries.removeIf(tagged -> matcher.test(tagged.entry()));
        return this;
    }

    /**
     * Returns {@code true} if the tooltip has no entries.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean notEmpty() {
        return !isEmpty();
    }

    //endregion

    //region transforms

    /**
     * Registers a lazy transform that will be applied when the tooltip is built.
     * Transforms run in registration order via {@link #apply()}.
     */
    @Contract("_ -> this")
    public Tooltip addTransform(Consumer<Tooltip> transform) {
        transforms.add(transform);
        return this;
    }

    /**
     * Applies all registered transforms sequentially, then clears the list.
     * Called automatically by {@link #flatEntries()} / {@link #toVanilla()}.
     */
    @Contract("-> this")
    public Tooltip apply() {
        for (Consumer<Tooltip> transform : transforms) {
            transform.accept(this);
        }
        transforms.clear();
        return this;
    }

    //endregion

    //region merge

    /**
     * Appends all entries and transforms from {@code other} into this tooltip.
     */
    @Contract("_ -> this")
    public Tooltip merge(Tooltip other) {
        entries.addAll(other.entries);
        transforms.addAll(other.transforms);
        return this;
    }

    //endregion

    //region stack

    @Nullable
    public TooltipStack<?> stack() {
        return stack;
    }

    @Contract("_ -> this")
    public Tooltip setStack(@Nullable TooltipStack<?> stack) {
        this.stack = stack;
        return this;
    }

    //endregion

    //region conversion

    /**
     * Applies pending transforms, then returns all entries in insertion order.
     */
    public MutableList<TooltipEntry> flatEntries() {
        apply();
        return entries.collect(TaggedEntry::entry);
    }

    /**
     * Converts the tooltip into vanilla's {@code Either<FormattedText, TooltipComponent>} list
     * for use with {@code GuiGraphics.renderComponentTooltipFromElements}.
     */
    public MutableList<Either<FormattedText, TooltipComponent>> toVanilla() {
        MutableList<TooltipEntry> flat = flatEntries();
        MutableList<Either<FormattedText, TooltipComponent>> result = MutableLists.empty();
        for (TooltipEntry entry : flat) {
            result.add(switch (entry) {
                case TooltipEntry.TextEntry(var text) -> Either.left(text);
                case TooltipEntry.DynamicEntry(var provider) -> Either.left(provider.get());
                case TooltipEntry.ComponentEntry(var component) -> Either.right(component);
            });
        }
        return result;
    }

    //endregion

    //region copy

    /**
     * Creates a shallow copy of the entry list. Immutable {@link TooltipEntry} records are shared;
     * {@link TaggedEntry} wrappers and the list itself are new instances.
     */
    public Tooltip copy() {
        return new Tooltip(MutableLists.ofAll(entries), MutableLists.ofAll(transforms), stack);
    }

    //endregion

    //region locator resolution

    private int resolveIndex(TooltipLocator locator) {
        return switch (locator) {
            case Head ignored -> 0;
            case Tail ignored -> entries.size();
            case At(var index) -> Math.clamp(index, 0, entries.size());
            case Relative(var anchor, var finder, var offset, var matchPriority) -> {
                int matchIdx = findMatch(finder, matchPriority);
                if (matchIdx < 0) yield entries.size();
                int base = switch (anchor) {
                    case before -> matchIdx;
                    case after -> matchIdx + 1;
                };
                yield Math.clamp(base + offset, 0, entries.size());
            }
        };
    }

    private int findMatch(TooltipFinder finder, MatchPriority priority) {
        return switch (finder) {
            case TooltipFinder.ByMarker(var marker) -> scanEntries(tagged -> tagged.marker().equals(marker), priority);
            case TooltipFinder.ByEntry(var matcher) -> scanEntries(tagged -> matcher.test(tagged.entry()), priority);
        };
    }

    /**
     * Scans entries for the first or last match depending on priority.
     *
     * @return the matching index, or {@code -1} if not found
     */
    private int scanEntries(Predicate<TaggedEntry> predicate, MatchPriority priority) {
        int result = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (predicate.test(entries.get(i))) {
                if (priority == MatchPriority.first) return i;
                result = i;
            }
        }
        return result;
    }

    //endregion

}
