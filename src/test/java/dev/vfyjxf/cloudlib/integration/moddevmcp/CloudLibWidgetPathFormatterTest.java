package dev.vfyjxf.cloudlib.integration.moddevmcp;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CloudLibWidgetPathFormatter Tests")
class CloudLibWidgetPathFormatterTest {

    @Test
    @DisplayName("Formats path using widget keys when present")
    void formatsPathUsingKeys() {
        Widget root = new Widget();
        root.setKey("root");
        Widget child = new Widget();
        child.setKey("child");
        WidgetPath path = WidgetPath.of(root, child);

        String result = CloudLibWidgetPathFormatter.format(path);

        assertEquals("root/child", result);
    }

    @Test
    @DisplayName("Formats path with fallback type and index when key missing")
    void formatsPathWithFallback() {
        Widget root = new Widget();
        Widget child = new Widget();
        WidgetPath path = WidgetPath.of(root, child);

        String result = CloudLibWidgetPathFormatter.format(path);

        assertEquals("Widget#0/Widget#1", result);
    }

    @Test
    @DisplayName("Formats repeated sibling types with sibling-stable ordinals")
    void formatsRepeatedSiblingTypesWithSiblingStableOrdinals() {
        TestWidgetGroup root = new TestWidgetGroup();
        TestWidgetGroup content = root.addChild(new TestWidgetGroup());
        Widget first = content.addChild(new Widget());
        Widget second = content.addChild(new Widget());
        Widget third = content.addChild(new Widget());

        String firstPath = CloudLibWidgetPathFormatter.format(WidgetPath.of(root, content, first));
        String secondPath = CloudLibWidgetPathFormatter.format(WidgetPath.of(root, content, second));
        String thirdPath = CloudLibWidgetPathFormatter.format(WidgetPath.of(root, content, third));

        assertEquals("TestWidgetGroup#0/TestWidgetGroup#0/Widget#0", firstPath);
        assertEquals("TestWidgetGroup#0/TestWidgetGroup#0/Widget#1", secondPath);
        assertEquals("TestWidgetGroup#0/TestWidgetGroup#0/Widget#2", thirdPath);
    }

    @Test
    @DisplayName("Formats empty path as empty string")
    void formatsEmptyPath() {
        String result = CloudLibWidgetPathFormatter.format(WidgetPath.empty());

        assertEquals("", result);
    }

    private static final class TestWidgetGroup extends WidgetGroup<Widget> {
        private <W extends Widget> W addChild(W child) {
            return addWidget(child);
        }
    }
}
