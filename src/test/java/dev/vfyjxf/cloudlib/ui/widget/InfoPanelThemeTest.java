package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped sheets against the info-panel widgets: every bundled theme paints
 * the bar's parts (each semantic segment class to a different ink), the header's
 * title and rule, the key/value columns and a slot-backed icon row's cells — and
 * leaves the stock (bare) icon row alone.
 * <p>
 * The corpus tests pin the sheets' skeleton and the widget tests pin the
 * selectors on inline sheets; this is the pass that joins the two — a shipped
 * theme that stops painting a component, or a widget whose part name drifts away
 * from the selector the sheets were written against, fails here.
 */
class InfoPanelThemeTest {

    /** The shipped sheets, by theme id — the same files {@code theme.json} declares. */
    private static final Map<String, String> sheets = shippedSheets();

    private static Map<String, String> shippedSheets() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("standard", "/assets/cloudlib/ui/themes/standard/base.css");
        out.put("ore", "/assets/cloudlib/ui/themes/ore/ore.css");
        out.put("mc", "/assets/cloudlib/ui/themes/mc/mc.css");
        return Map.copyOf(out);
    }

    @BeforeAll
    static void boot() {
        Bootstrap.bootStrap();
    }

    @Test
    void everyShippedSheetPaintsTheBarParts() {
        List<String> failures = new ArrayList<>();
        int painted = 0;
        for (Map.Entry<String, String> sheet : sheets.entrySet()) {
            Theme theme = theme(sheet);
            InfoBarWidget bar = InfoBarWidget.segmented(
                InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleDanger),
                InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk)
            );
            bar.mark(0.5);

            StyleValue<?> track = background(theme, part(bar, "track"));
            StyleValue<?> frame = background(theme, part(bar, "frame"));
            StyleValue<?> danger = background(theme, part(bar, "fill"));
            StyleValue<?> ok = background(theme, part(bar, "fill", 1));
            StyleValue<?> mark = background(theme, part(bar, "mark"));

            for (Map.Entry<String, StyleValue<?>> part : Map
                    .of("track", track, "frame", frame, ".danger fill", danger, ".ok fill", ok, "mark", mark)
                    .entrySet()) {
                if (part.getValue() == null) {
                    failures.add(sheet.getKey() + ": " + part.getKey());
                } else {
                    painted++;
                }
            }
            if (danger != null && ok != null && danger.value().equals(ok.value())) {
                failures.add(sheet.getKey() + ": .danger and .ok resolve to the same fill");
            }
        }
        assertEquals(List.of(), failures, "bar parts a sheet no longer paints");
        assertTrue(painted >= 15, "the corpus has shrunk: " + painted + " painted parts");
    }

    /**
     * The bar chrome every shipped sheet has to draw, as properties rather than as
     * literals: a stroked 1px ring on {@code ::part(frame)} — the width the widget
     * insets its fills by — a trough that is a translucent dark ink (it has to sit on
     * the light panels and the dark ones alike), and a threshold tick bright enough
     * to read against that trough. A sheet that hands back a flat colour where the
     * ring belongs, or an opaque trough, fails here instead of on screen.
     */
    @Test
    void everyShippedSheetFramesTheBarInARingOverADarkChannel() {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> sheet : sheets.entrySet()) {
            Theme theme = theme(sheet);
            InfoBarWidget bar = InfoBarWidget.of(0.5);
            bar.mark(0.5);
            String name = sheet.getKey();

            StyleValue<?> ring = background(theme, part(bar, "frame"));
            StyleValue<?> track = background(theme, part(bar, "track"));
            StyleValue<?> mark = background(theme, part(bar, "mark"));
            if (!(ring != null && ring.value() instanceof BorderTexture border)) {
                failures.add(name + ": ::part(frame) is not a stroked ring");
                continue;
            }
            if (border.thicknessTop() != InfoBarWidget.of(0.5).frameThickness() || border.thicknessLeft() != 1) {
                failures.add(name + ": the ring is " + border.thicknessTop() + "px, not the 1px the widget insets by");
            }
            if (!(track != null && track.value() instanceof ColorTexture channel)) {
                failures.add(name + ": the trough is not a flat ink: " + (track == null ? null : track.value()));
                continue;
            }
            if (!isDark(channel.color())) {
                failures.add(name + ": the trough is not a dark channel: " + Integer.toHexString(channel.color()));
            }
            if (alphaOf(channel.color()) == 0xFF) {
                failures.add(name + ": the trough is opaque — it has to read on light and dark panels alike");
            }
            if (!isDark(border.colorTop())) {
                failures.add(name + ": the ring is not darker than the trough it frames");
            }
            if (!(mark != null && mark.value() instanceof ColorTexture tick) || !isBright(tick.color())) {
                failures.add(name + ": the threshold tick is not bright enough to read on the trough");
            }
        }
        assertEquals(List.of(), failures, "bar chrome a shipped sheet no longer draws");
    }

    /** A channel-ink test: the trough stays near the bottom of the range, whatever hue it leans to. */
    private static boolean isDark(int argb) {
        return luminance(argb) < 0.2f;
    }

    /** The mark's side of the same test. */
    private static boolean isBright(int argb) {
        return luminance(argb) > 0.7f;
    }

    private static float luminance(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    private static int alphaOf(int argb) {
        return argb >>> 24;
    }

    @Test
    void everyShippedSheetPaintsTheHeaderAndTheRow() {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> sheet : sheets.entrySet()) {
            Theme theme = theme(sheet);

            SectionHeaderWidget header = SectionHeaderWidget.of("Damage");
            Widget title = part(header, "title");
            Widget rule = part(header, "rule");
            if (theme.resolve(title).get(Styles.color) == null) {
                failures.add(sheet.getKey() + ": section-header::part(title) color");
            }
            if (background(theme, rule) == null) {
                failures.add(sheet.getKey() + ": section-header::part(rule) background");
            }

            KVRowWidget row = KVRowWidget.of("Health", "20");
            StyleValue<?> label = theme.resolve(part(row, "label")).get(Styles.color);
            StyleValue<?> value = theme.resolve(part(row, "value")).get(Styles.color);
            if (label == null || value == null) {
                failures.add(sheet.getKey() + ": kv-row column colors");
            } else if (label.value().equals(value.value())) {
                failures.add(sheet.getKey() + ": the label and the value share an ink");
            }
        }
        assertEquals(List.of(), failures, "header/row slots a sheet no longer paints");
    }

    @Test
    void everyShippedSheetPaintsASlotBackedIconRowsCells() {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> sheet : sheets.entrySet()) {
            Theme theme = theme(sheet);
            IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 4).slotBacked(true);
            StyleValue<?> cell = background(theme, row.children().getFirst());
            if (cell == null) {
                failures.add(sheet.getKey() + ": icon row .slot");
            } else if (cell.value() == VisualTexture.empty) {
                failures.add(sheet.getKey() + ": the .slot bed is the empty texture");
            }
        }
        assertEquals(List.of(), failures, "the .slot bed a sheet no longer paints");
    }

    @Test
    void everyShippedSheetLeavesTheStockIconRowBare() {
        List<String> painted = new ArrayList<>();
        for (Map.Entry<String, String> sheet : sheets.entrySet()) {
            Theme theme = theme(sheet);
            IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 4);
            if (background(theme, row.children().getFirst()) != null) {
                painted.add(sheet.getKey());
            }
        }
        assertEquals(List.of(), painted, "a shipped sheet paints a bed the stock icon row does not want");
    }

    // region fixture

    private static Theme theme(String themeId, String path) {
        try (InputStream in = InfoPanelThemeTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing shipped sheet " + path);
            }
            Stylesheet sheet = CssParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return new Theme(ResourceLocation.fromNamespaceAndPath("cloudlib", themeId), sheet);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Theme theme(Map.Entry<String, String> sheet) {
        return theme(sheet.getKey(), sheet.getValue());
    }

    private static Widget part(Widget owner, String name) {
        return part(owner, name, 0);
    }

    private static Widget part(Widget owner, String name, int index) {
        List<Widget> matches = new ArrayList<>();
        for (Widget child : ((CompositeWidget<?>) owner).children()) {
            if (name.equals(child.stylePart())) {
                matches.add(child);
            }
        }
        if (index >= matches.size()) {
            throw new AssertionError("no ::part(" + name + ") #" + index + " on the widget");
        }
        return matches.get(index);
    }

    private static @Nullable StyleValue<?> background(Theme theme, Widget node) {
        var key = Styles.byId("background");
        return key == null ? null : theme.resolve(node).get(key);
    }

    // endregion
}
