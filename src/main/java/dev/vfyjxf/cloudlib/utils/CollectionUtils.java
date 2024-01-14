package dev.vfyjxf.cloudlib.utils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class CollectionUtils {


    public static <T, R> List<R> castAndMap(Iterable<T> list, Class<R> castClass) {
        List<R> l = new ArrayList<>();
        for (T t : list) {
            if (castClass.isAssignableFrom(t.getClass())) {
                l.add((R) t);
            }
        }
        return l;
    }


    public static <T, R> List<R> map(Iterable<T> list, Function<T, R> function) {
        List<R> l = new ArrayList<>();
        for (T t : list) {
            l.add(function.apply(t));
        }
        return l;
    }


    public static <T, R> List<R> map(T[] list, Function<T, R> mapper) {
        List<R> next = new ArrayList<>(list.length + 1);
        for (T entry : list) {
            next.add(mapper.apply(entry));
        }
        return next;
    }


    public static <T, R> List<R> filterAndMap(Iterable<T> list, Predicate<T> predicate, Function<T, R> function) {
        List<R> l = null;
        for (T t : list) {
            if (predicate.test(t)) {
                if (l == null)
                    l = new ArrayList<>();
                l.add(function.apply(t));
            }
        }
        return l == null ? Collections.emptyList() : l;
    }

    public static <T, R> List<R> mapAndFilter(Iterable<T> list, Predicate<R> predicate, Function<T, R> function) {
        List<R> l = null;
        for (T t : list) {
            R r = function.apply(t);
            if (predicate.test(r)) {
                if (l == null)
                    l = new ArrayList<>();
                l.add(r);
            }
        }
        return l == null ? Collections.emptyList() : l;
    }

}
