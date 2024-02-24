package dev.vfyjxf.cloudlib.api.ui.widgets;

import org.jetbrains.annotations.Range;

public interface ILayer {


    @Range(from = 0, to = 100)
    int getLayer();


    void setLayer(@Range(from = 0, to = 100) int layer);

}
