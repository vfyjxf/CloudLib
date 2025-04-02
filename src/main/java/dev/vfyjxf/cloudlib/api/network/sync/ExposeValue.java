package dev.vfyjxf.cloudlib.api.network.sync;

import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.snapshot.SnapshotFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * @param <T>
 */
public interface ExposeValue<T> {

    //region factory

    static <T> ExposeValue<T> of(Supplier<T> supplier, SnapshotFactory factory, CheckStrategy<T> strategy) {
        return new DirectReferenceExposeValue<>(supplier);
    }

    //endregion

    //region identity and debug info

    /**
     * @return the debug name of this value
     */
    default String name() {
        //TODO: implement this
        throw new NotImplementedException();
    }

    /**
     * The id of this value,it represents the type of this value in the data stream.
     *
     * @return the id of this value
     */
    default short id() {
        //TODO: implement this
        throw new NotImplementedException();
    }

    //endregion

    @Contract(pure = true)
    default Snapshot<T> snapshot() {
        //TODO: implement this
        throw new NotImplementedException();
    }

    default @UnknownNullability T previous() {
        return Snapshot.getValue(snapshot());
    }

    default boolean changed() {
        var snapshot = snapshot();
        return switch (Snapshot.currentState(snapshot, current())) {
            case UNCHANGED -> false;
            case CHANGED -> true;
            case ILLEGAL -> {
                boolean readOnly = snapshot instanceof Snapshot.Readonly;
                throw new IllegalStateException("Illegally modifity a " + (readOnly ? "readonly" : "immutable reference") + " snapshot(id:" + id() + " name:" + name() + ")");
            }
        };
    }

    T current();

    //region primitive

    sealed interface IntValue extends ExposeValue<Integer> permits PrimitiveExposedValues.Int {

        static IntValue of(IntSupplier supplier) {
            return new PrimitiveExposedValues.Int(supplier);
        }

        int getInt();

        default Integer current() {
            return getInt();
        }
    }

    sealed interface LongValue extends ExposeValue<Long> permits PrimitiveExposedValues.Long {

        static LongValue of(LongSupplier supplier) {
            return new PrimitiveExposedValues.Long(supplier);
        }

        long getLong();

        default Long current() {
            return getLong();
        }
    }

    sealed interface FloatValue extends ExposeValue<Float> permits PrimitiveExposedValues.Float {

        static FloatValue of(FloatSupplier supplier) {
            return new PrimitiveExposedValues.Float(supplier);
        }

        float getFloat();

        default Float current() {
            return getFloat();
        }
    }

    sealed interface DoubleValue extends ExposeValue<Double> permits PrimitiveExposedValues.Double {

        static DoubleValue of(DoubleSupplier supplier) {
            return new PrimitiveExposedValues.Double(supplier);
        }

        double getDouble();

        default Double current() {
            return getDouble();
        }
    }

    sealed interface BooleanValue extends ExposeValue<Boolean> permits PrimitiveExposedValues.Boolean {

        static BooleanValue of(BooleanSupplier supplier) {
            return new PrimitiveExposedValues.Boolean(supplier);
        }

        boolean getBoolean();

        default Boolean current() {
            return getBoolean();
        }
    }

    sealed interface ByteValue extends ExposeValue<Byte> permits PrimitiveExposedValues.Byte {

        static ByteValue of(ByteSupplier supplier) {
            return new PrimitiveExposedValues.Byte(supplier);
        }

        byte getByte();

        default Byte current() {
            return getByte();
        }
    }

    sealed interface ShortValue extends ExposeValue<Short> permits PrimitiveExposedValues.Short {

        static ShortValue of(ShortSupplier supplier) {
            return new PrimitiveExposedValues.Short(supplier);
        }

        short getShort();

        default Short current() {
            return getShort();
        }
    }


    @FunctionalInterface
    interface FloatSupplier {
        float getAsFloat();
    }

    @FunctionalInterface
    interface ByteSupplier {
        byte getAsByte();
    }

    @FunctionalInterface
    interface ShortSupplier {
        short getAsShort();
    }

    //endregion

}
