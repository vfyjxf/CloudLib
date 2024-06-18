package dev.vfyjxf.cloudlib.api.ui.animation;

public interface IAnimatable<T extends IAnimatable<T>> {

    T interpolate(T next, float delta);

}
