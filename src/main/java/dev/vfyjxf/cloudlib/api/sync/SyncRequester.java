package dev.vfyjxf.cloudlib.api.sync;

import dev.vfyjxf.cloudlib.api.sync.annotations.SyncValue;
import dev.vfyjxf.cloudlib.sync.SyncManager;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

/**
 * An object which want to sync with server should implement this interface.
 * It will provide its fields access to the sync system.
 * <p>
 * The fields which want to sync should be annotated with {@link SyncValue}.
 */

public interface SyncRequester {

    @MustBeInvokedByOverriders
    static void checkAccess() {
        Class<?> callerClass = StackWalker.getInstance().getCallerClass();
        if (callerClass != SyncManager.class) {
            throw new IllegalCallerException("Access denied");
        }
    }

    /**
     * Ensure you call {@link #checkAccess()} before return the field.
     */
    FieldAccessor getFieldAccess(String fieldName);


}
