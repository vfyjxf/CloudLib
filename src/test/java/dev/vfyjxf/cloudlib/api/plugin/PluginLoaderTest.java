package dev.vfyjxf.cloudlib.api.plugin;

import com.google.auto.service.AutoService;
import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;


@NotNullByDefault
public class PluginLoaderTest {

    @Test
    void testSort() {
        PluginLoader.LoadingResult<ModPlugin> result = PluginLoader.load(AnnotationPluginLookup.of(ModPlugin.class));
        Assertions.assertSame(TestPlugin.class, result.plugins().getFirst().getClass());
        Assertions.assertEquals(4, result.plugins().size());
        Assertions.assertEquals(
            "plugin: cloudlib:test_plugin_e failed to load because: Missing required dependency: cloudlib:test_plugin_c,\n" +
            "plugin: cloudlib:test_plugin_d failed to load because: Missing optional dependency: cloudlib:test_plugin_c",
            result.failures().makeString("", ",\n", "")
        );
        Assertions.assertThrows(IllegalStateException.class, () -> PluginLoader.load(SpiPluginLookup.of(DuplicatePluginInterface.class)));
    }

	@PluginMarker
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

	@PluginMarker
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
                    PluginDependency.Order.after,
                    PluginDependency.Constraint.required
                )
            );
        }
    }

    //    @AutoService(ModPlugin.class)
//	@PluginMarker
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
                    PluginDependency.Order.after,
                    PluginDependency.Constraint.optionalRequired
                )
            );
        }
    }

	@PluginMarker
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
                    PluginDependency.Order.after,
                    PluginDependency.Constraint.optionalRequired
                )
            );
        }
    }

	@PluginMarker
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
                    PluginDependency.Order.after,
                    PluginDependency.Constraint.required
                )
            );
        }
    }

    interface DuplicatePluginInterface extends ModPlugin {

    }

    @AutoService(DuplicatePluginInterface.class)
    public static class DuplicatePlugin implements DuplicatePluginInterface {
        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_a");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of();
        }
    }

    @AutoService(DuplicatePluginInterface.class)
    public static class DuplicatePlugin2 implements DuplicatePluginInterface {
        @Override
        public Namespace pluginId() {
            return CloudNamespaces.ofMod("test_plugin_a");
        }

        @Override
        public Set<PluginDependency> dependencies() {
            return Set.of();
        }
    }

}



