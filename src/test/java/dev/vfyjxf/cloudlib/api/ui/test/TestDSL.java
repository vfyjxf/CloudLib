package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A tiny SwiftUI-like DSL used ONLY for tests.
 * <p>
 * It intentionally mirrors {@link ScopedReceiver#buildChildren(Runnable)} / {@link ScopedReceiver#add(Blueprint)} usage
 * so we can build complex trees in tests and verify reconciliation + {@link StateSlot} updates.
 */
public final class TestDSL {

    private TestDSL() {}

    // ==================== Style ====================

    public record Style(String name) {
        public static final Style NONE = new Style("none");

        public Style {
            Objects.requireNonNull(name, "name");
        }
    }

    // ==================== Widgets (test-only) ====================

    public static class TestTextWidget extends Widget {
        public String text;
        public Style style = Style.NONE;
        public int updateCount;
        public int unmountCount;

        public TestTextWidget() {
            onUnmount(() -> unmountCount++);
        }

        @Override
        public String toString() {
            return "TestTextWidget{text='" + text + "', key=" + key() + "}";
        }
    }

    public static class TestButtonWidget extends Widget {
        public String label;
        public Style style = Style.NONE;
        public @Nullable Runnable onClick;
        public int updateCount;
        public int unmountCount;

        public TestButtonWidget() {
            onUnmount(() -> unmountCount++);
        }

        public void click() {
            if (onClick != null) {
                onClick.run();
            }
        }

        @Override
        public String toString() {
            return "TestButtonWidget{label='" + label + "', key=" + key() + "}";
        }
    }

    public static class TestStackWidget extends CompositeWidget<Widget> {
        public final String kind;
        public @Nullable String name;
        public Style style = Style.NONE;

        public int onStateChangedCount;
        public int unmountCount;

        public TestStackWidget(String kind) {
            this.kind = kind;
            onUnmount(() -> unmountCount++);
        }

        @Override
        public void onStateChanged() {
            onStateChangedCount++;
        }

        @Override
        public String toString() {
            return "TestStackWidget{" + kind + ", name=" + name + ", key=" + key() + "}";
        }
    }

    // ==================== Blueprints (test-only) ====================

    private abstract static class AbstractTestBlueprint<T extends Widget> implements Blueprint<T> {
        protected @Nullable Object key;

        public AbstractTestBlueprint<T> key(@Nullable Object key) {
            this.key = key;
            return this;
        }

        @Override
        public @Nullable Object key() {
            return key;
        }
    }

    public static final class TextBlueprint extends AbstractTestBlueprint<TestTextWidget> {
        private final String text;
        private final Style style;

        public TextBlueprint(String text, Style style) {
            this.text = Objects.requireNonNull(text, "text");
            this.style = Objects.requireNonNull(style, "style");
        }

        public String text() {
            return text;
        }

        @Override
        public TestTextWidget createWidget(Scene scene, SceneContext context) {
            return new TestTextWidget();
        }

        @Override
        public void updateWidget(TestTextWidget widget, Scene scene, SceneContext context) {
            widget.text = text;
            widget.style = style;
            widget.updateCount++;
        }

        @Override
        public String toString() {
            return "Text[\"" + text + "\"]";
        }
    }

    public static final class ButtonBlueprint extends AbstractTestBlueprint<TestButtonWidget> {
        private final String label;
        private final @Nullable Runnable onClick;
        private final Style style;

        public ButtonBlueprint(String label, @Nullable Runnable onClick, Style style) {
            this.label = Objects.requireNonNull(label, "label");
            this.onClick = onClick;
            this.style = Objects.requireNonNull(style, "style");
        }

        public String label() {
            return label;
        }

        @Override
        public TestButtonWidget createWidget(Scene scene, SceneContext context) {
            return new TestButtonWidget();
        }

        @Override
        public void updateWidget(TestButtonWidget widget, Scene scene, SceneContext context) {
            widget.label = label;
            widget.onClick = onClick;
            widget.style = style;
            widget.updateCount++;
        }

        @Override
        public String toString() {
            return "Button[\"" + label + "\"]";
        }
    }

    private abstract static class AbstractGroupBlueprint extends AbstractTestBlueprint<TestStackWidget>
        implements Blueprint.Group<TestStackWidget, Widget> {

        protected final Style style;
        protected final Supplier<List<Blueprint<?>>> childrenSupplier;

        protected AbstractGroupBlueprint(List<Blueprint<?>> children, Style style) {
            this(() -> children, style);
        }

        protected AbstractGroupBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier, Style style) {
            this.style = Objects.requireNonNull(style, "style");
            this.childrenSupplier = Objects.requireNonNull(childrenSupplier, "childrenSupplier");
        }

        @Override
        public MutableList<Blueprint<Widget>> children() {
            return asWidgetBlueprints(childrenSupplier.get());
        }

        @Override
        public void updateWidget(TestStackWidget widget, Scene scene, SceneContext context) {
            widget.style = style;
        }

        protected abstract String kind();

        @Override
        public TestStackWidget createWidget(Scene scene, SceneContext context) {
            return new TestStackWidget(kind());
        }
    }

    public static final class VStackBlueprint extends AbstractGroupBlueprint {
        public VStackBlueprint(List<Blueprint<?>> children, Style style) {
            super(children, style);
        }

        public VStackBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier, Style style) {
            super(childrenSupplier, style);
        }

        @Override
        protected String kind() {
            return "VStack";
        }

        @Override
        public String toString() {
            return "VStack" + (key == null ? "" : "(key=" + key + ")");
        }
    }

    public static final class HStackBlueprint extends AbstractGroupBlueprint {
        public HStackBlueprint(List<Blueprint<?>> children, Style style) {
            super(children, style);
        }

        public HStackBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier, Style style) {
            super(childrenSupplier, style);
        }

        @Override
        protected String kind() {
            return "HStack";
        }

        @Override
        public String toString() {
            return "HStack" + (key == null ? "" : "(key=" + key + ")");
        }
    }

    public static final class ZStackBlueprint extends AbstractGroupBlueprint {
        public ZStackBlueprint(List<Blueprint<?>> children, Style style) {
            super(children, style);
        }

        public ZStackBlueprint(Supplier<List<Blueprint<?>>> childrenSupplier, Style style) {
            super(childrenSupplier, style);
        }

        @Override
        protected String kind() {
            return "ZStack";
        }

        @Override
        public String toString() {
            return "ZStack" + (key == null ? "" : "(key=" + key + ")");
        }
    }

    public static final class GroupBlueprint extends AbstractGroupBlueprint {
        private final @Nullable String name;

        public GroupBlueprint(@Nullable String name, List<Blueprint<?>> children, Style style) {
            super(children, style);
            this.name = name;
        }

        public GroupBlueprint(@Nullable String name, Supplier<List<Blueprint<?>>> childrenSupplier, Style style) {
            super(childrenSupplier, style);
            this.name = name;
        }

        @Override
        protected String kind() {
            return "Group";
        }

        public @Nullable String name() {
            return name;
        }

        @Override
        public void updateWidget(TestStackWidget widget, Scene scene, SceneContext context) {
            super.updateWidget(widget, scene, context);
            widget.name = name;
        }

        @Override
        public String toString() {
            String n = name == null ? "unnamed" : name;
            String k = key == null ? "" : ", key=" + key;
            return "Group[" + n + k + "]";
        }
    }

    // ==================== DSL API (test-only) ====================

    public static VStackBlueprint VStack(Runnable content) {
        return VStack(Style.NONE, content);
    }

    public static VStackBlueprint VStack(Style style, Runnable content) {
        VStackBlueprint blueprint = new VStackBlueprint(() -> ScopedReceiver.buildChildren(content), style);
        return ScopedReceiver.add(blueprint);
    }

    public static VStackBlueprint VStack(@Nullable Object key, Style style, Runnable content) {
        VStackBlueprint blueprint = new VStackBlueprint(() -> ScopedReceiver.buildChildren(content), style);
        blueprint.key(key);
        return ScopedReceiver.add(blueprint);
    }

    public static HStackBlueprint HStack(Runnable content) {
        return HStack(Style.NONE, content);
    }

    public static HStackBlueprint HStack(Style style, Runnable content) {
        HStackBlueprint blueprint = new HStackBlueprint(() -> ScopedReceiver.buildChildren(content), style);
        return ScopedReceiver.add(blueprint);
    }

    public static HStackBlueprint HStack(@Nullable Object key, Style style, Runnable content) {
        HStackBlueprint blueprint = new HStackBlueprint(() -> ScopedReceiver.buildChildren(content), style);
        blueprint.key(key);
        return ScopedReceiver.add(blueprint);
    }

    public static ZStackBlueprint ZStack(Runnable content) {
        return ZStack(Style.NONE, content);
    }

    public static ZStackBlueprint ZStack(Style style, Runnable content) {
        ZStackBlueprint blueprint = new ZStackBlueprint(() -> ScopedReceiver.buildChildren(content), style);
        return ScopedReceiver.add(blueprint);
    }

    public static ZStackBlueprint ZStack(@Nullable Object key, Style style, Runnable content) {
        ZStackBlueprint blueprint = new ZStackBlueprint(() -> ScopedReceiver.buildChildren(content), style);
        blueprint.key(key);
        return ScopedReceiver.add(blueprint);
    }

    public static GroupBlueprint Group(Runnable content) {
        return Group(null, Style.NONE, content);
    }

    public static GroupBlueprint Group(String name, Runnable content) {
        return Group(name, Style.NONE, content);
    }

    public static GroupBlueprint Group(@Nullable String name, Style style, Runnable content) {
        GroupBlueprint blueprint = new GroupBlueprint(name, () -> ScopedReceiver.buildChildren(content), style);
        return ScopedReceiver.add(blueprint);
    }

    public static GroupBlueprint Group(@Nullable Object key, @Nullable String name, Style style, Runnable content) {
        GroupBlueprint blueprint = new GroupBlueprint(name, () -> ScopedReceiver.buildChildren(content), style);
        blueprint.key(key);
        return ScopedReceiver.add(blueprint);
    }

    public static TextBlueprint Text(String text) {
        return Text(text, Style.NONE);
    }

    public static TextBlueprint Text(String text, Style style) {
        TextBlueprint blueprint = new TextBlueprint(text, style);
        return ScopedReceiver.add(blueprint);
    }

    public static TextBlueprint Text(@Nullable Object key, String text, Style style) {
        TextBlueprint blueprint = new TextBlueprint(text, style);
        blueprint.key(key);
        return ScopedReceiver.add(blueprint);
    }

    public static ButtonBlueprint Button(String label, Runnable onClick) {
        return Button(label, onClick, Style.NONE);
    }

    public static ButtonBlueprint Button(String label, Runnable onClick, Style style) {
        ButtonBlueprint blueprint = new ButtonBlueprint(label, onClick, style);
        return ScopedReceiver.add(blueprint);
    }

    public static ButtonBlueprint Button(@Nullable Object key, String label, Runnable onClick, Style style) {
        ButtonBlueprint blueprint = new ButtonBlueprint(label, onClick, style);
        blueprint.key(key);
        return ScopedReceiver.add(blueprint);
    }

    // ==================== Utils ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MutableList<Blueprint<Widget>> asWidgetBlueprints(List<Blueprint<?>> children) {
        // Keep insertion order; the reconciler will rely on it.
        return (MutableList) Lists.mutable.ofAll(children);
    }
}
