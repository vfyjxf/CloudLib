package dev.vfyjxf.cloudlib.api.data.handle;

/** Removes the listener it was created for. Returned by {@link Handle#onChange}. */
@FunctionalInterface
public interface Subscription {
    void unsubscribe();
}
