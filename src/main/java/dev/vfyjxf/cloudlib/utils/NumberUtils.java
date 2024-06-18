package dev.vfyjxf.cloudlib.utils;

public class NumberUtils {

    private static final char[] ENCODED_POSTFIXES = "KMGTPE".toCharArray();


    public static String toSuffix(long number) {
        String numberStr = Long.toString(number);
        if (numberStr.length() <= 3) {
            return numberStr;
        }

        long base = number;
        int exponent = -1;
        String reslut = "";
        while (Long.toString(base).length() > 3) {
            reslut = String.format("%.2f", base / 1000F);
            base = base / 1000;
            exponent++;
        }

        return reslut + ENCODED_POSTFIXES[exponent];
    }

}
