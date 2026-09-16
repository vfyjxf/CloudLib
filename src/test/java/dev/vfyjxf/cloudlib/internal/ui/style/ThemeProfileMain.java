package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone profiling workload — run with the JProfiler agent attached.
 * Not a test; lives in the test sourceset for classpath access.
 * {@code java -agentpath:... -cp <test-cp> ...ThemeProfileMain [iterations]}
 */
public final class ThemeProfileMain {

    public static void main(String[] args) {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 800;

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
        Theme theme = new Theme(ResourceLocation.fromNamespaceAndPath("test", "perf"), CssParser.parse(css.toString()));

        // 40 containers × 25 children = 1040 nodes
        ThemePerfTest.Probe root = new ThemePerfTest.Probe("panel");
        for (int i = 0; i < 40; i++) {
            ThemePerfTest.Probe p = root.child(new ThemePerfTest.Probe("panel"));
            for (int k = 0; k < 25; k++) {
                p.child(new ThemePerfTest.Probe(k % 3 == 0 ? "button" : "tag" + (k % 50)));
            }
        }
        List<ThemePerfTest.Probe> nodes = new ArrayList<>();
        collect(root, nodes);

        long start = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            Cascade.ResolveContext ctx = new Cascade.ResolveContext(theme);
            for (var n : nodes) {
                theme.resolveShared(n, ctx);
            }
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[theme-profile] " + iterations + " iters × " + nodes.size() + " nodes = " + ms + "ms ("
                + (double) ms / iterations + "ms/pass)");
    }

    private static void collect(ThemePerfTest.Probe n, List<ThemePerfTest.Probe> out) {
        out.add(n);
        for (var c : n.children()) {
            if (c instanceof ThemePerfTest.Probe p) {
                collect(p, out);
            }
        }
    }
}
