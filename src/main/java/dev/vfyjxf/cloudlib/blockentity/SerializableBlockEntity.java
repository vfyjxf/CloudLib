package dev.vfyjxf.cloudlib.blockentity;

/** A BlockEntity that owns a {@link BlockEntitySerializer} for declarative persistence. */
@FunctionalInterface
public interface SerializableBlockEntity {
    BlockEntitySerializer serializer();
}
