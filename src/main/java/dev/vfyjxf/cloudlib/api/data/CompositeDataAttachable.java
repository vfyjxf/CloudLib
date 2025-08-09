package dev.vfyjxf.cloudlib.api.data;

import dev.vfyjxf.cloudlib.api.data.attachment.SyncableAttachmentContainer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO:WIP
interface CompositeDataAttachable extends DataAttachable, IAttachmentHolder {

    @Override
    @NotNull AttachableDataContainer attachableDataContainer();

    SyncableAttachmentContainer attachmentContainer();

    @Override
    default boolean hasAttachments() {
        return attachmentContainer().hasAttachments();
    }

    @Override
    default boolean hasData(@NotNull AttachmentType<?> type) {
        return attachmentContainer().hasData(type);
    }

    @Override
    default <T> @NotNull T getData(@NotNull AttachmentType<T> type) {
        return attachmentContainer().getData(type);
    }

    @Override
    default <T> @Nullable T setData(@NotNull AttachmentType<T> type, @NotNull T data) {
        return attachmentContainer().setData(type, data);
    }

    @Override
    default <T> @Nullable T removeData(@NotNull AttachmentType<T> type) {
        return attachmentContainer().removeData(type);
    }

    @Override
    default void syncData(@NotNull AttachmentType<?> type) {
        attachmentContainer().syncData(type);
    }
}
