package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.Constraints;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

import java.util.List;

public interface LayoutSolution {

    LayoutResult layout(List<IWidget> widgets, Constraints constraints);

    int minIntrinsicWidth(List<IWidget> widgets, int height);

    int maxIntrinsicWidth(List<IWidget> widgets, int height);

    int minIntrinsicHeight(List<IWidget> widgets, int width);

    int maxIntrinsicHeight(List<IWidget> widgets, int width);

}
