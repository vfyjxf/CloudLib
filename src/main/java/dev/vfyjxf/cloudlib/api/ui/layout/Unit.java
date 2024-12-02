package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.jetbrains.annotations.Range;

public class Unit {

    /**
     * The factor of the unit.e.g. 0.5 width of parent
     */
    private float factor;
    /**
     * The direct value of the unit.e.g. 10px
     */
    private int value;
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
    private int min;
    /**
     * The max value of the unit.
     */
    private int max;

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
        if (max > 0) {
            return Math.min(max, Math.max(min, value));
        } else {
            return Math.max(min, value);
        }
    }
}
