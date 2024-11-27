package dev.vfyjxf.cloudlib.api.ui.animation;

public interface Animatable<T extends Animatable<T>> {

    T interpolate(T next, float delta);

}
