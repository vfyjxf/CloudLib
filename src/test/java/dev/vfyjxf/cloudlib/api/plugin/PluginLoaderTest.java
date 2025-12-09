package dev.vfyjxf.cloudlib.api.plugin;

import com.google.auto.service.AutoService;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;


public class PluginLoaderTest {

    @Test
    void testSort() {
        PluginLoader.LoadingResult<ModPlugin> result = PluginLoader.load(ModPlugin.class);
        Assertions.assertSame(TestPlugin.class, result.plugins().getFirst().getClass());
        Assertions.assertEquals(2, result.plugins().size());
        Assertions.assertEquals(
            "plugin: cloudlib:test_plugin_e failed to load because: Missing required dependency: cloudlib:test_plugin_c,\n" +
            "plugin: cloudlib:test_plugin_d failed to load because: Missing optional dependency: cloudlib:test_plugin_c",
            result.failures().makeString("", ",\n", "")
        );
    }

    @AutoService(ModPlugin.class)
    public static class TestPlugin implements ModPlugin {

        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_a");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of();
        }
    }

    @AutoService(ModPlugin.class)
    public static class TestPluginB implements ModPlugin {

        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_b");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of(
                new PluginDependency(
                    CloudNamespaces.ofMod("test_plugin_a"),
                    PluginDependency.Order.AFTER,
                    PluginDependency.Constraint.REQUIRED
                )
            );
        }
    }

    //    @AutoService(ModPlugin.class)
    public static class TestPluginC implements ModPlugin {

        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_c");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of(
                new PluginDependency(
                    CloudNamespaces.ofMod("test_plugin_b"),
                    PluginDependency.Order.AFTER,
                    PluginDependency.Constraint.OPTIONAL_REQUIRED
                )
            );
        }
    }

    @AutoService(ModPlugin.class)
    public static class TestPluginD implements ModPlugin {

        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_d");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of(
                new PluginDependency(
                    CloudNamespaces.ofMod("test_plugin_c"),
                    PluginDependency.Order.AFTER,
                    PluginDependency.Constraint.OPTIONAL_REQUIRED
                )
            );
        }
    }

    @AutoService(ModPlugin.class)
    public static class TestPluginE implements ModPlugin {

        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_e");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of(
                new PluginDependency(
                    CloudNamespaces.ofMod("test_plugin_c"),
                    PluginDependency.Order.AFTER,
                    PluginDependency.Constraint.REQUIRED
                )
            );
        }
    }

}



