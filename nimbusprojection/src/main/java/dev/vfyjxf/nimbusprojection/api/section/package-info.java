/**
 * Composable container sections — the Jade-plugin-style SPI that turns a
 * capability-bearing block into an ordered stack of panel content:
 * <ul>
 *   <li>{@link dev.vfyjxf.nimbusprojection.api.section.SectionProvider} —
 *       server-authoritative data collection (common plugin hook)</li>
 *   <li>{@link dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory} —
 *       client rendering (client plugin hook)</li>
 *   <li>{@link dev.vfyjxf.nimbusprojection.api.section.SectionType} — the
 *       phantom-typed kind token binding data, codec, provider and widget</li>
 * </ul>
 * Sections carry server-addressable ids ({@code "type/index"}) so ops
 * payloads never trust the client's panel model.
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.nimbusprojection.api.section;
