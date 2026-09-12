package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import net.minecraft.resources.ResourceLocation;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Overhead checks for the theme pipeline — deterministic accessor-call bounds
 * (no wall-clock asserts on CI) plus one generously-bounded wall-clock smoke.
 * <p>
 * The fixture is a real {@link CompositeWidget} tree; {@link Probe} counts the
 * selector-surface accessors the matcher touches ({@code styleTag()},
 * {@code styleStates()}, {@code children()} — the latter backs both the
 * sibling and the child lookups).
 */
class ThemePerfTest {

    /** Instrumented composite — counts every selector-surface accessor call. */
    static class Probe extends CompositeWidget<Widget> {
        static final AtomicInteger tagCalls = new AtomicInteger();
        static final AtomicInteger stateCalls = new AtomicInteger();
        static final AtomicInteger childrenCalls = new AtomicInteger();

        private final String tag;

        Probe(String tag) {
            this.tag = tag;
        }

        <W extends Widget> W child(W w) {
            addWidget(w);
            return w;
        }

        static void reset() {
            tagCalls.set(0);
            stateCalls.set(0);
            childrenCalls.set(0);
        }

        @Override
        public String styleTag() {
            tagCalls.incrementAndGet();
            return tag;
        }

        @Override
        public Set<String> styleStates() {
            stateCalls.incrementAndGet();
            return super.styleStates();
        }

        @Override
        public @Unmodifiable MutableList<Widget> children() {
            childrenCalls.incrementAndGet();
            return super.children();
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

    private static List<Widget> collect(Probe root) {
        List<Widget> out = new ArrayList<>();
        out.add(root);
        for (Widget c : root.children()) {
            if (c instanceof Probe p) {
                out.addAll(collect(p));
            }
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
        List<Widget> nodes = collect(root);
        Probe.reset();
        // one shared context for the whole tree — what applyTree does
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        for (Widget n : nodes) {
            ctx.resolve(n);
        }
        // each node's states/siblings fetched once, not once per rule or per descendant
        int s = Probe.stateCalls.get();
        assertTrue(s <= nodes.size() * 2, "state fetches " + s + " for " + nodes.size() + " nodes");
        int b = Probe.childrenCalls.get();
        assertTrue(b <= nodes.size() * 2, "children fetches " + b + " for " + nodes.size() + " nodes");
    }

    @Test
    void deepTreeResolveStaysLinear() {
        Theme t = theme("panel { color: #FFF } panel panel { padding: 1px } button { margin: 2px }");
        Probe shallow = chain(8);
        Probe deep = chain(32);
        Probe.reset();
        for (Widget n : collect(shallow)) new Cascade.ResolveContext(t).resolve(n);
        int s8 = Probe.tagCalls.get();
        Probe.reset();
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        for (Widget n : collect(deep)) ctx.resolve(n);
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
        List<Widget> nodes = collect(root);
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        long start = System.nanoTime();
        for (Widget n : nodes) {
            ctx.resolve(n);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[theme-perf] 1000-node resolve: " + elapsedMs + "ms ("
                + Probe.tagCalls.get() + " tag lookups, "
                + Probe.stateCalls.get() + " state snapshots)");
        // hugely generous bound — just guards quadratic blowups
        assertTrue(elapsedMs < 10_000, "resolve took " + elapsedMs + "ms for 1000 nodes");
    }

    /**
     * Profiling workload — only runs with {@code -Dcloudlib.theme.profile=1}.
     * Loops the cascade over a realistic tree long enough for CPU sampling.
     */
    @Test
    void profilingWorkload() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                Boolean.getBoolean("cloudlib.theme.profile"),
                "profiling workload — enable with -Dcloudlib.theme.profile=1");
        StringBuilder css = new StringBuilder(":root { --c: #FFF; --pad: 4px }\n");
        for (int i = 0; i < 50; i++) {
            css.append("tag")
                    .append(i)
                    .append(" { color: var(--c); padding: var(--pad); margin: ")
                    .append(i % 4 + 1)
                    .append("px }\n");
        }
        css.append("button:hovered { margin: 2px }")
                .append("panel .slot { gap: 3px }")
                .append("panel > button { z-index: 1 }")
                .append("*:nth-child(2n) { flex-grow: 1 }");
        Theme t = theme(css.toString());
        // realistic tree: 40 containers × 25 children = 1040 nodes
        Probe root = new Probe("panel");
        for (int i = 0; i < 40; i++) {
            Probe p = root.child(new Probe("panel"));
            for (int k = 0; k < 25; k++) {
                p.child(new Probe(k % 3 == 0 ? "button" : "tag" + (k % 50)));
            }
        }
        List<Widget> nodes = collect(root);
        for (int iter = 0; iter < 500; iter++) {
            Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
            for (Widget n : nodes) {
                ctx.resolve(n);
            }
        }
    }

    // ------------------------------------------------------------------ engine-level

    @Test
    void resolvedStylesAreStable() {
        Theme t = theme("panel { color: #FFF } button { padding: 4px }");
        Probe root = chain(4);
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(t);
        List<Widget> nodes = collect(root);
        Map<String, Cascade.ResolvedDecl> first = ctx.resolve(nodes.get(nodes.size() - 1));
        assertNotNull(first.get("color")); // inherited through 4 levels
        assertNotNull(first.get("padding"));
    }
}
