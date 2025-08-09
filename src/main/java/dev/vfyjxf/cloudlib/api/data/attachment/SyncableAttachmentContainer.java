package dev.vfyjxf.cloudlib.api.data.attachment;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class SyncableAttachmentContainer implements IAttachmentHolder {

    private final Map<AttachmentType<?>, Object> attachmentMap = new IdentityHashMap<>();

    public SyncableAttachmentContainer() {
        super();
    }

    public void syncAll() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public boolean hasAttachments() {
        return false;
    }

    @Override
    public boolean hasData(@NotNull AttachmentType<?> type) {
        return false;
    }

    @Override
    public <T> @NotNull T getData(@NotNull AttachmentType<T> type) {
        return null;
    }

    @Override
    public <T> @Nullable T setData(@NotNull AttachmentType<T> type, @NotNull T data) {
        return null;
    }

    @Override
    public <T> @Nullable T removeData(@NotNull AttachmentType<T> type) {
        return null;
    }

    @Override
    public void syncData(@NotNull AttachmentType<?> type) {
    }
}

