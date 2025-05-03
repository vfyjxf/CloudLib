package dev.vfyjxf.cloudlib.api.nodes;

@FunctionalInterface
public interface UpperNodeAppender<N> {
    boolean append(N node);
}
