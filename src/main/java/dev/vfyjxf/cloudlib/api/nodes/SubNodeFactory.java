package dev.vfyjxf.cloudlib.api.nodes;

public interface SubNodeFactory<N, P> {

    /**
     * Creates a new node with the given context and parent.
     *
     * @param context The context to use for creating the node.
     * @param parent  The parent of the node.
     * @return The created node.
     */
    N create(NodeContext context, P parent);

    interface NonContextFactory<N, P> extends SubNodeFactory<N, P> {
        @Override
        N create(NodeContext context, P parent);

        N create(P parent);
    }

}
