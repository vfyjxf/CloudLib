package dev.vfyjxf.cloudlib.api.nodes;

public interface NodeFactory<N, A> {

    N create(NodeContext context, A state);

    interface NonContextFactory<N, S> extends NodeFactory<N, S> {
        @Override
        default N create(NodeContext context, S state) {
            return create(state);
        }

        N create(S state);
    }

    interface StatelessFactory<N> extends NodeFactory<N, Void> {
        @Override
        default N create(NodeContext context, Void state) {
            return create(context);
        }

        N create(NodeContext context);
    }

    interface NonParameterizedFactory<N> extends NodeFactory<N, Void> {
        @Override
        default N create(NodeContext context, Void state) {
            return create();
        }

        N create();
    }
}
