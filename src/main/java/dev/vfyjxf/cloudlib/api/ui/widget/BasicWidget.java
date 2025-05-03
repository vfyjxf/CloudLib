package dev.vfyjxf.cloudlib.api.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.LifecycleStage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Internal interface for basic widget lifecycle and state management.
 */
@ApiStatus.Internal
abstract class BasicWidget {

    final StateManagement stateManagement = new StateManagement(this);
    LifecycleStage stage = LifecycleStage.CONSTRUCT;
    boolean dirty = true;



    @Contract(pure = true)
    abstract @Nullable BasicWidget parent();

    void markDirty() {
        if (!dirty) {
            this.dirty = true;
        }
        var parent = parent();
        if (parent != null) {
            parent.markDirty(); ;
        }
    }

//
//    abstract void runInitStage();
//
//    abstract void dispose();
//
//    abstract void runDestroyStage();
}
