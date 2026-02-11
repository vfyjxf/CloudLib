/**
 * Layout properties that map to taffy style attributes.
 * <p>
 * This package contains layout property implementations that directly map to the
 * taffy-java layout engine's {@link dev.vfyjxf.taffy.style.TaffyStyle} class.
 * <p>
 * <h2>Key Types from taffy-java</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.taffy.style.TaffyDimension} - Size values supporting LENGTH, PERCENT, AUTO,
 *       MIN_CONTENT, MAX_CONTENT, FIT_CONTENT, STRETCH</li>
 *   <li>{@link dev.vfyjxf.taffy.style.LengthPercentage} - Values supporting LENGTH, PERCENT (for padding, border, gap)</li>
 *   <li>{@link dev.vfyjxf.taffy.style.LengthPercentageAuto} - Values supporting LENGTH, PERCENT, AUTO,
 *       MIN_CONTENT, MAX_CONTENT, FIT_CONTENT, STRETCH (for margin, inset)</li>
 * </ul>
 * <p>
 * <h2>Property Categories</h2>
 * <ul>
 *   <li><b>Size Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.SizeProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.SizeConstraintProperty}</li>
 *   <li><b>Spacing Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.PaddingProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.MarginProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.GapProperty}</li>
 *   <li><b>Flexbox Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexDirectionProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexWrapProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexGrowProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexShrinkProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexBasisProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexProperty}</li>
 *   <li><b>Alignment Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.AlignItemsProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.AlignSelfProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.AlignContentProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.JustifyContentProperty}</li>
 *   <li><b>Position Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.PositionTypeProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.InsetProperty}</li>
 *   <li><b>Grid Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.GridTemplateRowsProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.GridTemplateColumnsProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.GridAutoFlowProperty}, etc.</li>
 *   <li><b>Display Properties:</b> {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.DisplayProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.OverflowProperty},
 *       {@link dev.vfyjxf.cloudlib.api.ui.style.property.layout.BoxSizingProperty}</li>
 * </ul>
 *
 * @see dev.vfyjxf.taffy.style.TaffyStyle
 * @see dev.vfyjxf.cloudlib.api.ui.style.UIStyles
 */
@NotNullByDefault
package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
