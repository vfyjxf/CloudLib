package dev.vfyjxf.cloudlib.api.network.sync;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

class PrimitiveExposedValues {

    static final class Int implements ExposeValue.IntValue {
        private final IntSupplier supplier;

        public Int(IntSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public int getInt() {
            return supplier.getAsInt();
        }
    }

    static final class Long implements ExposeValue.LongValue {
        private final LongSupplier supplier;

        public Long(LongSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public long getLong() {
            return supplier.getAsLong();
        }
    }

    static final class Float implements ExposeValue.FloatValue {
        private final FloatSupplier supplier;

        public Float(FloatSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public float getFloat() {
            return supplier.getAsFloat();
        }
    }

    static final class Double implements ExposeValue.DoubleValue {
        private final DoubleSupplier supplier;

        public Double(DoubleSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public double getDouble() {
            return supplier.getAsDouble();
        }
    }

    static final class Boolean implements ExposeValue.BooleanValue {
        private final BooleanSupplier supplier;

        public Boolean(BooleanSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean getBoolean() {
            return supplier.getAsBoolean();
        }
    }

    static final class Byte implements ExposeValue.ByteValue {
        private final ByteSupplier supplier;

        public Byte(ByteSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public byte getByte() {
            return supplier.getAsByte();
        }
    }

    static final class Short implements ExposeValue.ShortValue {
        private final ShortSupplier supplier;

        public Short(ShortSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public short getShort() {
            return supplier.getAsShort();
        }
    }


}
