package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import dev.vfyjxf.cloudlib.api.ui.theme.Themeable;
import dev.vfyjxf.cloudlib.internal.css.CssParser;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Overhead checks for the theme pipeline — deterministic accessor-call bounds
 * (no wall-clock asserts on CI) plus one generously-bounded wall-clock smoke.
 */
class ThemePerfTest {

    /** Instrumented fixture — counts every accessor call the matcher makes. */
    static final class Probe implements Themeable {
        static final AtomicInteger tagCalls = new AtomicInteger();
        static final AtomicInteger stateCalls = new AtomicInteger();
        static final AtomicInteger siblingCalls = new AtomicInteger();
        static final AtomicInteger childCalls = new AtomicInteger();

        final String tag;
        final List<String> classes = new ArrayList<>();
        final Set<String> states = new HashSet<>();
        Probe parent;
        final List<Probe> children = new ArrayList<>();

        Probe(String tag) {
            this.tag = tag;
        }

        Probe child(Probe c) {
            c.parent = this;
            children.add(c);
            return c;
        }

        static void reset() {
            tagCalls.set(0);
            stateCalls.set(0);
            siblingCalls.set(0);
            childCalls.set(0);
        }

        @Override
        public String themeTag() {
            tagCalls.incrementAndGet();
            return tag;
        }

        @Override
        public List<String> themeClasses() {
            return classes;
        }

        @Override
        public Set<String> themeStates() {
            stateCalls.incrementAndGet();
            return states;
        }

        @Override
        public @Nullable Themeable themeParent() {
            return parent;
        }

        @Override
        public List<? extends Themeable> themeSiblings() {
            siblingCalls.incrementAndGet();
            return parent != null ? new ArrayList<>(parent.children) : List.of(this);
        }

        @Override
        public List<? extends Themeable> themeChildren() {
            childCalls.incrementAndGet();
            return new ArrayList<>(children);
        }
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "perf"), CssParser.parse(css));
    }

    /** A deep chain of {@code depth} panels ending in a leaf. */
    private static Probe chain(int depth) {
        Probe root = new Probe("panel");
        Probe cur = root;
        for (int i = 1; i < depth; i++) {
            cur = cur.child(new Probe("panel"));
        }
        cur.child(new Probe("button"));
        return root;
    }

    private static List<Probe> collect(Probe root) {
        List<Probe> out = new ArrayList<>();
        out.add(root);
        for (Probe c : root.children) {
            out.addAll(collect(c));
        }
        return out;
    }

    // ------------------------------------------------------------------ rule bucketing

    @Test
    void ruleIndexSkipsUnrelatedRules() {
        // 200 rules for <other>, 1 for <button>
        StringBuilder css = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            css.append("other").append(i).append(" { padding: ").append(i).append("px }\n");
        }
        css.append("button { padding: 1px }");
        Theme t = theme(css.toString());
        Probe button = new Probe("button");
        // the button only sees its own rule + the always bucket (empty here)
        assertEquals(1, t.rulesFor(button).size());
        Probe other = new Probe("other7");
        assertEquals(1, t.rulesFor(other).size());
    }

    // ------------------------------------------------------------------ memoization

    @Test
    void ancestorResolutionIsMemoizedPerPass() {
        Theme t = theme("panel { color: #FFF } button { padding: 4px }");
        Probe root = chain(32);
        List<Probe> nodes = collect(root);
        Probe.reset();
        // one shared context for the whole tree — what applyTree does
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        for (Probe n : nodes) {
            ctx.resolve(n);
        }
        // each node's states/siblings fetched once, not once per rule or per descendant
        int s = Probe.stateCalls.get();
        assertTrue(s <= nodes.size() * 2, "state fetches " + s + " for " + nodes.size() + " nodes");
        int b = Probe.siblingCalls.get();
        assertTrue(b <= nodes.size() * 2, "sibling fetches " + b + " for " + nodes.size() + " nodes");
    }

    @Test
    void deepTreeResolveStaysLinear() {
        Theme t = theme("panel { color: #FFF } panel panel { padding: 1px } button { margin: 2px }");
        Probe shallow = chain(8);
        Probe deep = chain(32);
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        Probe.reset();
        for (Probe n : collect(shallow)) ctx.resolve(n);
        int s8 = Probe.tagCalls.get();
        Probe.reset();
        for (Probe n : collect(deep)) ctx = new Cascade.ResolveContext(t);
        for (Probe n : collect(deep)) ctx.resolve(n);
        int s32 = Probe.tagCalls.get();
        // 4× nodes must not explode — descendant matching walks ancestors but
        // the ancestor walk is bounded by depth, and each node matches once
        assertTrue(s32 < s8 * 16, "tag calls " + s8 + " → " + s32);
    }

    // ------------------------------------------------------------------ wall-clock smoke

    @Test
    void largeTreeResolvesWithinReason() {
        // 50-tag theme × 1000-node tree — wide margin; catches pathological blowups
        StringBuilder css = new StringBuilder(":root { --c: #FFF }\n");
        for (int i = 0; i < 50; i++) {
            css.append("tag")
                    .append(i)
                    .append(" { color: var(--c); padding: ")
                    .append(i % 8 + 1)
                    .append("px }\n");
        }
        css.append("button:hovered { margin: 2px }");
        Theme t = theme(css.toString());
        Probe root = new Probe("tag0");
        Probe cur = root;
        for (int i = 0; i < 999; i++) {
            cur = cur.child(new Probe("tag" + (i % 50)));
        }
        List<Probe> nodes = collect(root);
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        long start = System.nanoTime();
        for (Probe n : nodes) {
            ctx.resolve(n);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[theme-perf] 1000-node resolve: " + elapsedMs + "ms ("
                + Probe.tagCalls.get() + " tag lookups, "
                + Probe.stateCalls.get() + " state snapshots)");
        // hugely generous bound — just guards quadratic blowups
        assertTrue(elapsedMs < 10_000, "resolve took " + elapsedMs + "ms for 1000 nodes");
    }

    // ------------------------------------------------------------------ engine-level

    @Test
    void resolvedStylesAreStable() {
        Theme t = theme("panel { color: #FFF } button { padding: 4px }");
        Probe root = chain(4);
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        Map<String, List<dev.vfyjxf.cloudlib.internal.css.ComponentValue>> first =
                ctx.resolve(collect(root).get(collect(root).size() - 1));
        assertNotNull(first.get("color")); // inherited through 4 levels
        assertNotNull(first.get("padding"));
    }
}
