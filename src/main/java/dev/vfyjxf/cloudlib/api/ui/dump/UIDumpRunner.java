package dev.vfyjxf.cloudlib.api.ui.dump;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discovers and executes all {@link UIDumpTest}-annotated methods across loaded mods.
 * <p>
 * Activation: set the system property {@code cloudlib.dump=true} (e.g. via a Gradle run config).
 * On the first client tick, all annotated methods are scanned, invoked, and the game is closed.
 * <p>
 * Output is written to {@code <gameDir>/dumps/<name>.png} by default.
 *
 * @see UIDumpTest
 * @see DumpContext
 */
public final class UIDumpRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger("CloudLib UIDumpRunner");
    private static final String SYSTEM_PROPERTY = "cloudlib.dump";

    private UIDumpRunner() {
    }

    /**
     * Registers the dump runner. Call this during mod initialization.
     * The runner only activates when {@code -Dcloudlib.dump=true} is set.
     */
    public static void register() {
        String dumpProp = System.getProperty(SYSTEM_PROPERTY);
        if (!"true".equals(dumpProp)) {
            return;
        }

        LOGGER.info("UI dump mode enabled - will execute dumps on first client tick");
        AtomicBoolean fired = new AtomicBoolean(false);

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (!fired.compareAndSet(false, true)) return;

            try {
                runAllDumps();
            } catch (Exception e) {
                LOGGER.error("Failed to run UI dumps", e);
            }
            Minecraft.getInstance().stop();
        });
    }

    private static void runAllDumps() {
        List<DumpEntry> entries = scanForDumps();
        if (entries.isEmpty()) {
            LOGGER.warn("No @UIDumpTest methods found");
            return;
        }

        LOGGER.info("Found {} dump test(s), executing...", entries.size());
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        int success = 0;
        int failed = 0;

        for (DumpEntry entry : entries) {
            Path outputDir = gameDir.resolve("dumps");
            DumpContext ctx = new DumpContext(entry.name, entry.width, entry.height, outputDir);
            try {
                entry.method.invoke(null, ctx);
                success++;
                LOGGER.info("  [OK] {}.{} ({})", entry.method.getDeclaringClass().getSimpleName(), entry.method.getName(), entry.name);
            } catch (Exception e) {
                failed++;
                LOGGER.error("  [FAIL] {}.{} ({})", entry.method.getDeclaringClass().getSimpleName(), entry.method.getName(), entry.name, e);
            }
        }

        LOGGER.info("Dump complete: {} succeeded, {} failed", success, failed);
    }

    private static List<DumpEntry> scanForDumps() {
        List<DumpEntry> result = new ArrayList<>();

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            scanData.getAnnotatedBy(UIDumpTest.class, ElementType.METHOD)
                    .forEach(annotationData -> {
                        String memberName = annotationData.memberName();
                        String className = annotationData.clazz().getClassName();

                        // Extract annotation attributes
                        var attrs = annotationData.annotationData();
                        String name = (String) attrs.getOrDefault("name", memberName);
                        int width = (int) attrs.getOrDefault("width", 320);
                        int height = (int) attrs.getOrDefault("height", 240);

                        try {
                            Class<?> clazz = Class.forName(className);
                            // memberName for methods is "methodName(Lparams;)Lreturn;"
                            // We need to find the method by name with DumpContext parameter
                            Method method = findMethod(clazz, memberName);
                            if (method == null) {
                                LOGGER.error("Could not find method {} in {}", memberName, className);
                                return;
                            }
                            if (!Modifier.isStatic(method.getModifiers())) {
                                LOGGER.error("@UIDumpTest method must be static: {}.{}", className, method.getName());
                                return;
                            }
                            result.add(new DumpEntry(name, width, height, method));
                        } catch (ClassNotFoundException e) {
                            LOGGER.error("Dump test class not found: {}", className, e);
                        }
                    });
        }

        return result;
    }

    private static Method findMethod(Class<?> clazz, String memberName) {
        // memberName format from ModFileScanData: "methodName" or "methodName(Larg;)V"
        String methodName = memberName.contains("(") ? memberName.substring(0, memberName.indexOf('(')) : memberName;
        try {
            return clazz.getDeclaredMethod(methodName, DumpContext.class);
        } catch (NoSuchMethodException e) {
            // Try all methods with matching name
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.isAnnotationPresent(UIDumpTest.class)) {
                    return m;
                }
            }
            return null;
        }
    }

    private record DumpEntry(String name, int width, int height, Method method) {
    }
}
