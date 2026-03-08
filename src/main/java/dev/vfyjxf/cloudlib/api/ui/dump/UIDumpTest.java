package dev.vfyjxf.cloudlib.api.ui.dump;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method as a UI dump test.
 * <p>
 * Annotated methods must be {@code static} and accept a single {@link DumpContext} parameter.
 * When dump mode is activated (via {@code -Dcloudlib.dump=true}), the {@link UIDumpRunner}
 * will discover and invoke all annotated methods across all loaded mods.
 * <p>
 * Example:
 * <pre>{@code
 * @UIDumpTest(name = "basic_scene", width = 320, height = 240)
 * public static void dumpBasic(DumpContext ctx) throws IOException {
 *     Scene scene = buildScene(ctx.width(), ctx.height());
 *     ctx.capture(scene, Path.of("basic.png"));
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIDumpTest {

    /**
     * Name of the dump test. Used as the output filename (without extension).
     * Must be unique within a mod.
     */
    String name();

    /**
     * Logical width for the dump capture.
     */
    int width() default 320;

    /**
     * Logical height for the dump capture.
     */
    int height() default 240;
}
