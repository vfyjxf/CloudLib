package dev.vfyjxf.cloudlib.api.ui.concept;

/**
 * 标记Widget(组件)身份的对象，组件的身份只通过它的类型(?)和Marker来区分。
 */
public abstract class Marker {

    public abstract boolean equals(Object o);

    public abstract int hashCode();


}
