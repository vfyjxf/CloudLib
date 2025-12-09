package dev.vfyjxf.cloudlib.concept.scope;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ScopedBuilderTest {

    @Test
    void build() {
        try (var root = RootScope.create()) {
            try (var main = ChildScope.autoScope()) {
                try (var sub = ChildScope.autoScope()) {
                    System.out.println(Scope.current());
                    Assertions.assertEquals(sub, Scope.current());
                }
                Assertions.assertEquals(main, Scope.current());
            }
            Assertions.assertEquals(root, Scope.current());
        }
        Assertions.assertThrows(IllegalStateException.class, Scope::current);
    }

    public static class Node {
        String name;

        public Node(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class GroupNode extends Node {
        List<Node> children = new ArrayList<>();

        public GroupNode(String name) {
            super(name);
        }

        void addChild(Node child) {
            children.add(child);
        }
    }

}


sealed interface Scope extends AutoCloseable {

    static Scope current() {
        RootScope rootScope = RootScope.threadLocal.get();
        if (rootScope == null) throw new IllegalStateException("No RootScope");
        if (rootScope.scopeStack.isEmpty()) throw new IllegalStateException("No Scope");
        return rootScope.scopeStack.peek();
    }

    RootScope root();

    void enter();

    void exit();

    default void close() {
        exit();
    }
}

final class RootScope implements Scope {

    Stack<Scope> scopeStack = new Stack<>();
    static ThreadLocal<RootScope> threadLocal = new ThreadLocal<>();

    public static RootScope create() {
        var root = new RootScope();
        root.enter();
        return root;
    }

    private RootScope() {}

    @Override
    public RootScope root() {
        return this;
    }

    @Override
    public void enter() {
        threadLocal.set(this);
        scopeStack.push(this);
    }

    @Override
    public void exit() {
        if (threadLocal.get() == this) threadLocal.remove();
        scopeStack.pop();
    }
}

final class ChildScope implements Scope {
    private final Scope parent;

    public static ChildScope autoScope() {
        RootScope rootScope = RootScope.threadLocal.get();
        if (rootScope == null) throw new IllegalStateException("No RootScope");
        var parent = rootScope.scopeStack.peek();
        var childScope = new ChildScope(parent);
        childScope.enter();
        return childScope;
    }

    private ChildScope(Scope parent) {
        this.parent = parent;
    }

    @Override
    public RootScope root() {
        return parent.root();
    }

    @Override
    public void enter() {
        root().scopeStack.push(this);
    }

    @Override
    public void exit() {
        root().scopeStack.pop();
    }
}