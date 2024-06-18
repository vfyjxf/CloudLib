package dev.vfyjxf.cloudlib.api.ui.layout;

import org.jetbrains.annotations.Nullable;

public interface IntrinsicLayoutable {

    @Nullable
    <T> T parentData();

    int minIntrinsicWidth(int height);

    int maxIntrinsicWidth(int height);

    int minIntrinsicHeight(int width);

    int maxIntrinsicHeight(int width);

}
