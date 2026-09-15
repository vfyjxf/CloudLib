package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/** Samples a UI source (a block entity, entity, machine...) once per frame. */
@FunctionalInterface
public interface SourceProvider {

    SourceSnapshot sample(SampleContext context);
}
