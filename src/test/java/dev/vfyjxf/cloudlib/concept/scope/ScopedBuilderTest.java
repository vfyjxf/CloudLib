package dev.vfyjxf.cloudlib.concept.scope;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Stack;

@ExtendWith(EphemeralTestServerProvider.class)
public class ScopedBuilderTest {

    @Test
    void build(MinecraftServer server) {
        try (var root = RootScope.create()) {
            try (var main = ChildScope.autoScope()) {
                try (var sub = ChildScope.autoScope()) {
                    Assertions.assertEquals(sub, Scope.current());
                }
                try (var sub2 = ChildScope.autoScope()) {
                    Assertions.assertEquals(sub2, Scope.current());
                }
                try (var sub3 = ChildScope.autoScope()) {
                    Assertions.assertEquals(sub3, Scope.current());
                }
                try (var sub4 = ChildScope.autoScope()) {
                    Assertions.assertEquals(sub4, Scope.current());
                }
                Assertions.assertEquals(main, Scope.current());
            }
            Assertions.assertEquals(root, Scope.current());
        }
        Assertions.assertThrows(IllegalStateException.class, Scope::current);
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