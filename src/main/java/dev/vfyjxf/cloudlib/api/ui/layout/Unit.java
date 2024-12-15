package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.jetbrains.annotations.Range;

public class Unit {

    /**
     * A special value for the unit.
     * When the value is Unspecified, it represents the value will be overridden by the parent or layout itself.
     */
    public static final int UNDEFINED = Integer.MIN_VALUE;
    /**
     * A special value for the unit.
     */
    public static final int INFINITY = Integer.MAX_VALUE;

    /**
     * The factor of the unit.e.g. 0.5 width of parent
     */
    private float factor;
    /**
     * The direct value of the unit.e.g. 10px
     */
    private int value = UNDEFINED;
    /**
     * The relative resizer of the unit.
     * As default, it will be the parent of the widget.
     * if relative is null, the value will be the width itself.
     */
    private Resizer relative;
    /**
     * The anchor of the unit.
     */
    private float anchor;
    /**
     * The min value of the unit.
     */
    private int min = UNDEFINED;
    /**
     * The max value of the unit.
     */
    private int max = UNDEFINED;
    private boolean enforced;

    public void reset() {
        this.factor = 0;
        this.value = 0;
        this.relative = null;
        this.anchor = 0;
        this.min = 0;
        this.max = 0;
    }

    public float factor() {
        return factor;
    }

    public int value() {
        return value;
    }

    public boolean isUndefined() {
        return value == UNDEFINED && factor == 0;
    }

    public boolean rangeDefined() {
        return min != UNDEFINED || max != UNDEFINED;
    }

    public Resizer relative() {
        return relative;
    }

    public float anchor() {
        return anchor;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public boolean enforced() {
        return enforced;
    }

    public Unit setRelative(Resizer relative) {
        this.relative = relative;
        return this;
    }

    public Unit setFactor(float factor) {
        this.factor = factor;
        return this;
    }

    public Unit setValue(int value) {
        this.value = value;
        return this;
    }

    public Unit setAnchor(float anchor) {
        this.anchor = anchor;
        return this;
    }

    public Unit setEnforced(boolean enforced) {
        this.enforced = enforced;
        return this;
    }

    public Unit setMin(@Range(from = 0, to = Integer.MAX_VALUE) int min) {
        Checks.checkRange(min, 0, Integer.MAX_VALUE);
        this.min = min;
        return this;
    }

    public Unit setMax(@Range(from = 0, to = Integer.MAX_VALUE) int max) {
        Checks.checkRange(max, 0, Integer.MAX_VALUE);
        this.max = max;
        return this;
    }

    public int normalizeSize(int value) {
        if (max == UNDEFINED) {
            return min == UNDEFINED ? value : Math.max(min, value);
        } else {
            return min == UNDEFINED ? Math.min(max, value) : Math.clamp(value, min, max);
        }
    }
}
