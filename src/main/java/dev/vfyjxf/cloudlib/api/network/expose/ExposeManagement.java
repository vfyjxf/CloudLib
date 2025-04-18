package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.utils.Maybe;
import dev.vfyjxf.cloudlib.utils.Checks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.eclipse.collections.api.map.primitive.MutableShortObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * manages the expose data for the client and server
 */
public final class ExposeManagement {

    public enum SyncStrategy {
        /**
         * Send all expose
         */
        FULL,
        /**
         * Send the changed value, prioritizing difference.,
         */
        DIFFERENCE,
        /**
         * Send the changed value,but don't care difference.
         */
        FULL_VALUE
    }

    private final MutableShortObjectMap<ExposeCommon> exposes = new ShortObjectHashMap<>();
    private final AtomicInteger currentId = new AtomicInteger(0);

    public void updateSnapshotState() {
        for (ExposeCommon expose : exposes) {
            expose.updateSnapshot();
        }
    }

    public boolean hasExpose() {
        return !exposes.isEmpty();
    }

    public short nextId() {
        return (short) currentId.getAndIncrement();
    }

    public <T extends ExposeCommon> T registerExpose(T expose) {
        Checks.checkNotNull(expose, "Expose cannot be null");
        if (exposes.containsKey(expose.id())) {
            throw new IllegalArgumentException("Expose with id " + expose.id() + " already exists");
        }
        exposes.put(expose.id(), expose);
        return expose;
    }

    public <T extends ReversedOnly<?, ?>> T registerReversed(T reversed) {
        Checks.checkNotNull(reversed, "Reversed cannot be null");
        ExposeCommon expose = (ExposeCommon) reversed;
        if (exposes.containsKey(expose.id())) {
            throw new IllegalArgumentException("Reversed with id " + expose.id() + " already exists");
        }
        exposes.put(expose.id(), expose);
        return reversed;
    }

    public ExposeCommon getExpose(short id) {
        ExposeCommon expose = exposes.get(id);
        if (expose == null) throw new IllegalArgumentException("No expose found with id: " + id);
        return expose;
    }

    public boolean anyToClient() {
        return exposes.anySatisfy(ExposeCommon::changed);
    }

    public boolean anyToServer() {
        return exposes.selectInstancesOf(Reversed.class)
                .anySatisfy(Reversed::hasReversedData);
    }

    public void writeAllToClient(RegistryFriendlyByteBuf byteBuf) {
        writeToClient(byteBuf, SyncStrategy.FULL);
    }

    public void writeDifferenceToClient(RegistryFriendlyByteBuf byteBuf) {
        writeToClient(byteBuf, SyncStrategy.DIFFERENCE);
    }

    public void writeChangesToClient(RegistryFriendlyByteBuf byteBuf) {
        writeToClient(byteBuf, SyncStrategy.FULL_VALUE);
    }

    public void writeToClient(RegistryFriendlyByteBuf byteBuf, SyncStrategy strategy) {
        if (strategy == SyncStrategy.FULL) {
            for (ExposeCommon expose : exposes) {
                if (expose instanceof ReversedOnly<?, ?>) continue;
                byteBuf.writeShort(expose.id());
                writeExpose(expose, byteBuf);
                expose.updateSnapshot();
            }
        } else {
            for (ExposeCommon expose : exposes) {
                try {
                    if (!expose.changed()) continue;
                    byteBuf.writeShort(expose.id());
                    if (strategy == SyncStrategy.DIFFERENCE && expose instanceof Differential<?> differential) {
                        writeDiff(expose, differential, byteBuf);
                    } else {
                        writeExpose(expose, byteBuf);
                    }
                    expose.updateSnapshot();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to write expose: (id:" + expose.id() + " name:" + expose.name() + ")", e);
                }
            }
        }
        byteBuf.writeShort(Short.MIN_VALUE);
    }

    public void receiveFromServer(RegistryFriendlyByteBuf byteBuf) {
        for (short id = byteBuf.readShort(); id != Short.MIN_VALUE; id = byteBuf.readShort()) {
            ExposeCommon expose = exposes.get(id);
            if (expose == null) throw new IllegalStateException("No expose found with id: " + id);
            try {
                Transcoder transcoder = (Transcoder) expose;
                if (expose instanceof Differential<?> differential && byteBuf.readBoolean()) {//boolean:send difference
                    differential.decodeDifference(byteBuf);
                } else {
                    transcoder.readFromServer(byteBuf);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read expose: (id:" + expose.id() + " name:" + expose.name() + ")", e);
            }
        }
    }

    public void receiveFromClient(RegistryFriendlyByteBuf byteBuf) {
        for (short id = byteBuf.readShort(); id != Short.MIN_VALUE; id = byteBuf.readShort()) {
            ExposeCommon expose = exposes.get(id);
            if (!(expose instanceof ReversedTranscoder transcoder))
                throw new IllegalStateException("No reversed found with id: " + id);

            try {
                transcoder.readFromClient(byteBuf);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read reversed: (id:" + expose.id() + " name:" + expose + ")", e);
            }
        }
    }

    private static void writeExpose(ExposeCommon exposeCommon, RegistryFriendlyByteBuf byteBuf) {
        switch (exposeCommon) {
            case BasicDownstreamExpose<?> expose -> expose.writeToClient(byteBuf);
            case BasicLayerExpose<?> layerExpose -> layerExpose.writeToClient(byteBuf);
            default -> throw new IllegalStateException("Unexpected expose: " + exposeCommon);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static <D> void writeDiff(ExposeCommon expose, Differential<D> differential, RegistryFriendlyByteBuf byteBuf) {
        Maybe<D> difference = differential.difference();
        if (difference.defined()) {
            byteBuf.writeShort(expose.id());
            byteBuf.writeBoolean(true);
            differential.encodeDifference(byteBuf, difference.get());
        } else {
            byteBuf.writeShort(expose.id());
            byteBuf.writeBoolean(false);
        }
    }

    public void writeToServer(RegistryFriendlyByteBuf byteBuf) {
        for (ExposeCommon expose : exposes) {
            if (expose instanceof Reversed<?, ?> reversed) {
                if (reversed.hasReversedData()) {
                    byteBuf.writeShort(expose.id());
                    ((ReversedTranscoder) reversed).writeToServer(byteBuf);
                }
            }
        }
        byteBuf.writeShort(Short.MIN_VALUE);
    }
}
