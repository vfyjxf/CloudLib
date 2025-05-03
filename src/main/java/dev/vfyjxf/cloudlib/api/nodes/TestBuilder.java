package dev.vfyjxf.cloudlib.api.nodes;

public final class TestBuilder {
    static void foo() {
        MetaNode<Node> metaNode = new MetaNode<>();

    }

    private static class Node {

    }

    private static class LeafNode extends Node {

    }

    private static class BranchNode<T extends Node> extends Node {

    }
}
