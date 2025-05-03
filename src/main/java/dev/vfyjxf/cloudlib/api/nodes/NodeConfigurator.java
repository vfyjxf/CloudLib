package dev.vfyjxf.cloudlib.api.nodes;

@FunctionalInterface
public interface NodeConfigurator<N> {
    void configure(N node, NodeContext context);
}
