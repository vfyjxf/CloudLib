package dev.vfyjxf.cloudlib.api.ui.reactive.debug;

import dev.vfyjxf.cloudlib.api.ui.reactive.BuildOwner;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A debug-enabled BuildOwner that tracks rebuild events for testing and debugging.
 * <p>
 * This class provides visibility into:
 * <ul>
 *   <li>Which elements are scheduled for rebuild</li>
 *   <li>How many times each element has been rebuilt</li>
 *   <li>The order of rebuild events</li>
 *   <li>Detailed rebuild history</li>
 * </ul>
 */
@ApiStatus.Experimental
public class DebugBuildOwner extends BuildOwner {

    /**
     * Record of a single rebuild event.
     */
    public record RebuildEvent(
            UIElement<?> element,
            String elementType,
            String blueprintType,
            long timestamp,
            int rebuildCount
    ) {
        @Override
        public String toString() {
            return String.format("[%d] %s<%s> (rebuild #%d)",
                    timestamp, elementType, blueprintType, rebuildCount);
        }
    }

    private final List<RebuildEvent> rebuildHistory = new ArrayList<>();
    private final Map<UIElement<?>, Integer> rebuildCounts = new LinkedHashMap<>();
    private final List<UIElement<?>> scheduledElements = new ArrayList<>();
    private boolean trackingEnabled = true;
    private long eventCounter = 0;

    @Override
    public void scheduleBuildFor(UIElement<?> element) {
        super.scheduleBuildFor(element);
        if (trackingEnabled) {
            scheduledElements.add(element);
        }
    }

    @Override
    public void buildScope() {
        if (trackingEnabled) {
            // Record rebuild events for all scheduled elements
            for (UIElement<?> element : scheduledElements) {
                recordRebuild(element);
            }
            scheduledElements.clear();
        }
        super.buildScope();
    }

    private void recordRebuild(UIElement<?> element) {
        int count = rebuildCounts.getOrDefault(element, 0) + 1;
        rebuildCounts.put(element, count);

        RebuildEvent event = new RebuildEvent(
                element,
                element.getClass().getSimpleName(),
                element.getBlueprint().getClass().getSimpleName(),
                eventCounter++,
                count
        );
        rebuildHistory.add(event);
    }

    /**
     * Gets the total number of rebuild events recorded.
     */
    public int getTotalRebuildCount() {
        return rebuildHistory.size();
    }

    /**
     * Gets how many times a specific element has been rebuilt.
     */
    public int getRebuildCount(UIElement<?> element) {
        return rebuildCounts.getOrDefault(element, 0);
    }

    /**
     * Gets the rebuild history.
     */
    public List<RebuildEvent> getRebuildHistory() {
        return List.copyOf(rebuildHistory);
    }

    /**
     * Gets elements that were scheduled but not yet processed.
     */
    public List<UIElement<?>> getPendingElements() {
        return List.copyOf(scheduledElements);
    }

    /**
     * Gets all elements that have been rebuilt at least once.
     */
    public List<UIElement<?>> getRebuiltElements() {
        return List.copyOf(rebuildCounts.keySet());
    }

    /**
     * Checks if a specific element was rebuilt.
     */
    public boolean wasRebuilt(UIElement<?> element) {
        return rebuildCounts.containsKey(element);
    }

    /**
     * Clears all debug history.
     */
    public void clearHistory() {
        rebuildHistory.clear();
        rebuildCounts.clear();
        scheduledElements.clear();
        eventCounter = 0;
    }

    /**
     * Enables or disables tracking.
     */
    public void setTrackingEnabled(boolean enabled) {
        this.trackingEnabled = enabled;
    }

    /**
     * Checks if tracking is enabled.
     */
    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    /**
     * Prints a summary of rebuild activity.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DebugBuildOwner Summary ===\n");
        sb.append("Total rebuild events: ").append(rebuildHistory.size()).append("\n");
        sb.append("Unique elements rebuilt: ").append(rebuildCounts.size()).append("\n");
        sb.append("\nRebuild counts per element:\n");

        for (var entry : rebuildCounts.entrySet()) {
            UIElement<?> element = entry.getKey();
            int count = entry.getValue();
            sb.append("  - ")
                    .append(element.getClass().getSimpleName())
                    .append("<")
                    .append(element.getBlueprint().getClass().getSimpleName())
                    .append(">: ")
                    .append(count)
                    .append(" times\n");
        }

        if (!rebuildHistory.isEmpty()) {
            sb.append("\nRecent rebuild events:\n");
            int start = Math.max(0, rebuildHistory.size() - 10);
            for (int i = start; i < rebuildHistory.size(); i++) {
                sb.append("  ").append(rebuildHistory.get(i)).append("\n");
            }
        }

        return sb.toString();
    }
}
