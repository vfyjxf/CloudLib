package dev.vfyjxf.cloudlib.api.ui.base;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Position-based state management. State slots are determined by call order and persist across rebuilds.
 */
public final class StateSlot {

    private static final ThreadLocal<StateContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private StateSlot() {}

    /**
     * Get or create state. Must be called within StateContext. Slot is determined by call order.
     */
    public static <T> StateAccessor<T> useState(Supplier<T> initialValue) {
        StateContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                "useState must be called during blueprint evaluation within a StateContext"
            );
        }
        return context.getOrCreateState(initialValue);
    }

    public static <T> StateAccessor<T> useState(T initialValue) {
        return useState(() -> initialValue);
    }

    /**
     * Create memoized value. Only recomputed when dependencies change.
     */
    public static <T> T useMemo(Supplier<T> compute, Object... dependencies) {
        StateContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            return compute.get();
        }
        return context.memoize(compute, dependencies);
    }

    /**
     * Register side effect. Runs after mount/update.
     */
    public static void useEffect(Runnable effect, Object... dependencies) {
        StateContext context = CURRENT_CONTEXT.get();
        if (context != null) {
            context.registerEffect(effect, dependencies);
        }
    }

    public static @Nullable StateContext currentContext() {
        return CURRENT_CONTEXT.get();
    }

    public static <T> T withContext(StateContext context, Supplier<T> block) {
        StateContext previous = CURRENT_CONTEXT.get();
        CURRENT_CONTEXT.set(context);
        context.resetSlotIndex();
        try {
            return block.get();
        } finally {
            CURRENT_CONTEXT.set(previous);
        }
    }

    public static void withContext(StateContext context, Runnable block) {
        withContext(context, () -> {
            block.run();
            return null;
        });
    }

    /**
     * State read/write accessor.
     */
    public static final class StateAccessor<T> {
        private final StateContext context;
        private final int slotIndex;

        StateAccessor(StateContext context, int slotIndex) {
            this.context = context;
            this.slotIndex = slotIndex;
        }

        /**
         * Gets the current value.
         */
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) context.getSlotValue(slotIndex);
        }

        /**
         * Sets a new value and triggers update if changed.
         */
        public void set(T value) {
            Object old = context.getSlotValue(slotIndex);
            if (!Objects.equals(old, value)) {
                context.setSlotValue(slotIndex, value);
                context.markDirty();
            }
        }

        /**
         * Updates value using a function.
         */
        public void update(Function<T, T> updater) {
            set(updater.apply(get()));
        }

        @Override
        public String toString() {
            return "StateAccessor[" + get() + "]";
        }
    }

    // ==================== StateContext ====================

    /**
     * Context that holds state slots for a widget.
     * Each widget has its own StateContext.
     */
    public static final class StateContext {
        private final List<Object> slots = new ArrayList<>();
        private final List<MemoEntry> memos = new ArrayList<>();
        private final List<EffectEntry> effects = new ArrayList<>();
        private int currentSlotIndex = 0;
        private int currentMemoIndex = 0;
        private int currentEffectIndex = 0;
        private boolean dirty = false;
        private @Nullable Runnable onDirty;

        /**
         * Creates a new StateContext.
         */
        public StateContext() {}

        //TODO:决定是否保留这个

        /**
         * Sets the callback for when state becomes dirty.
         */
        public void setOnDirty(@Nullable Runnable onDirty) {
            this.onDirty = onDirty;
        }

        /**
         * Gets or creates a state at the current slot.
         */
        <T> StateAccessor<T> getOrCreateState(Supplier<T> initialValue) {
            int index = currentSlotIndex++;

            if (index >= slots.size()) {
                // First render, initialize state
                slots.add(initialValue.get());
            }

            return new StateAccessor<>(this, index);
        }

        /**
         * Memorizes a value.
         */
        @SuppressWarnings("unchecked")
        <T> T memoize(Supplier<T> compute, Object[] dependencies) {
            int index = currentMemoIndex++;

            if (index >= memos.size()) {
                // First render
                T value = compute.get();
                memos.add(new MemoEntry(value, dependencies.clone()));
                return value;
            }

            MemoEntry entry = memos.get(index);
            if (!dependenciesEqual(entry.dependencies, dependencies)) {
                // Dependencies changed, recompute
                T value = compute.get();
                memos.set(index, new MemoEntry(value, dependencies.clone()));
                return value;
            }

            return (T) entry.value;
        }

        /**
         * Registers an effect.
         */
        void registerEffect(Runnable effect, Object[] dependencies) {
            int index = currentEffectIndex++;

            if (index >= effects.size()) {
                // First render, will run effect
                effects.add(new EffectEntry(effect, dependencies.clone(), true));
            } else {
                EffectEntry entry = effects.get(index);
                if (!dependenciesEqual(entry.dependencies, dependencies)) {
                    // Dependencies changed, mark to run
                    effects.set(index, new EffectEntry(effect, dependencies.clone(), true));
                }
            }
        }

        /**
         * Runs pending effects.
         */
        public void runEffects() {
            for (int i = 0; i < effects.size(); i++) {
                EffectEntry entry = effects.get(i);
                if (entry.shouldRun) {
                    entry.effect.run();
                    effects.set(i, new EffectEntry(entry.effect, entry.dependencies, false));
                }
            }
        }

        Object getSlotValue(int index) {
            return slots.get(index);
        }

        void setSlotValue(int index, Object value) {
            slots.set(index, value);
        }

        void resetSlotIndex() {
            currentSlotIndex = 0;
            currentMemoIndex = 0;
            currentEffectIndex = 0;
        }

        void markDirty() {
            dirty = true;
            if (onDirty != null) {
                onDirty.run();
            }
        }

        public boolean isDirty() {
            return dirty;
        }

        public void clearDirty() {
            dirty = false;
        }

        private static boolean dependenciesEqual(Object[] a, Object[] b) {
            if (a.length != b.length) return false;
            for (int i = 0; i < a.length; i++) {
                if (!Objects.equals(a[i], b[i])) return false;
            }
            return true;
        }

        private record MemoEntry(Object value, Object[] dependencies) {}

        private record EffectEntry(Runnable effect, Object[] dependencies, boolean shouldRun) {}
    }
}
