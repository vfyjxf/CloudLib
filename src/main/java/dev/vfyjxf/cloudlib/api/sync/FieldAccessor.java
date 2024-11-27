package dev.vfyjxf.cloudlib.api.sync;

public interface FieldAccessor {

    int getInt();

    void setInt(int value);

    long getLong();

    void setLong(long value);

    float getFloat();

    void setFloat(float value);

    double getDouble();

    void setDouble(double value);

    boolean getBoolean();

    void setBoolean(boolean value);

    char getChar();

    void setChar(char value);

    byte getByte();

    void setByte(byte value);

    short getShort();

    void setShort(short value);

    <T> T get();

    <T> void setObject(T value);

}
