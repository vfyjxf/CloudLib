package dev.vfyjxf.cloudlib.api.ui.element;

import java.util.*;

/**
 * Manages the build lifecycle for a tree of Elements.
 * <p>
 * BuildOwner is responsible for:
 * <ul>
 *   <li>Scheduling rebuilds (batching multiple mark-dirty calls)</li>
 *   <li>Ordering rebuilds by depth (parents before children)</li>
 *   <li>Coordinating the build phase</li>
 * </ul>
 * <p>
 * This is similar to Flutter's BuildOwner which manages the build phase
 * of the widget lifecycle.
 */
public class BuildOwner {
    
    /** Elements that need to be rebuilt, sorted by depth */
    private final PriorityQueue<Element> dirtyElements = new PriorityQueue<>(
        Comparator.comparingInt(Element::getDepth)
    );
    
    /** Set for O(1) contains check */
    private final Set<Element> dirtySet = new HashSet<>();
    
    /** Whether a build is currently scheduled */
    private boolean buildScheduled = false;
    
    /** Whether we're currently in a build phase */
    private boolean building = false;
    
    /** Callback to schedule a frame */
    private Runnable frameCallback;
    
    /** Statistics */
    private int totalRebuilds = 0;
    private int skippedRebuilds = 0;
    
    public BuildOwner() {
        this(null);
    }
    
    public BuildOwner(Runnable frameCallback) {
        this.frameCallback = frameCallback;
    }
    
    /**
     * Schedules an element for rebuild.
     * Multiple calls before the build phase are batched together.
     */
    public void scheduleBuild(Element element) {
        if (element.lifecycle != ElementLifecycle.MOUNTED) {
            return;
        }
        
        if (dirtySet.contains(element)) {
            return; // Already scheduled
        }
        
        dirtyElements.add(element);
        dirtySet.add(element);
        
        if (!buildScheduled && !building) {
            buildScheduled = true;
            scheduleFrame();
        }
    }
    
    /**
     * Performs the build phase, rebuilding all dirty elements.
     * Elements are rebuilt in depth order (parents first).
     */
    public void buildScope() {
        if (building) {
            throw new IllegalStateException("Already building");
        }
        
        building = true;
        buildScheduled = false;
        
        try {
            while (!dirtyElements.isEmpty()) {
                Element element = dirtyElements.poll();
                dirtySet.remove(element);
                
                // Skip if unmounted (could happen if parent unmounted us)
                if (element.lifecycle != ElementLifecycle.MOUNTED) {
                    skippedRebuilds++;
                    continue;
                }
                
                // Skip if no longer dirty (could be cleaned by ancestor rebuild)
                if (!element.dirty) {
                    skippedRebuilds++;
                    continue;
                }
                
                // Check if we can skip (optimization for stateful components)
                if (element instanceof ComponentElement ce && !ce.shouldRebuild()) {
                    skippedRebuilds++;
                    element.dirty = false;
                    continue;
                }
                
                // Perform rebuild
                element.performRebuild();
                totalRebuilds++;
            }
        } finally {
            building = false;
        }
    }
    
    /**
     * Immediately rebuilds all dirty elements.
     * Usually called by the framework at the start of a frame.
     */
    public void flushBuild() {
        if (!dirtyElements.isEmpty()) {
            buildScope();
        }
    }
    
    /**
     * Schedules a frame callback.
     * Override or set frameCallback to integrate with your rendering system.
     */
    protected void scheduleFrame() {
        if (frameCallback != null) {
            frameCallback.run();
        } else {
            // Default: build immediately (synchronous mode)
            buildScope();
        }
    }
    
    /**
     * Sets the frame callback for scheduling builds.
     */
    public void setFrameCallback(Runnable callback) {
        this.frameCallback = callback;
    }
    
    // ===== Statistics =====
    
    public int getPendingBuildCount() {
        return dirtyElements.size();
    }
    
    public int getTotalRebuilds() {
        return totalRebuilds;
    }
    
    public int getSkippedRebuilds() {
        return skippedRebuilds;
    }
    
    public void resetStatistics() {
        totalRebuilds = 0;
        skippedRebuilds = 0;
    }
    
    public boolean isBuilding() {
        return building;
    }
}
