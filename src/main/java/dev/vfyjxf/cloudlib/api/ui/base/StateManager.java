package dev.vfyjxf.cloudlib.api.ui.base;

//TODO:管理状态的变更，以及和组件的依赖关系

import org.eclipse.collections.api.list.MutableList;

/**
 * State management for {@link GroupSpec} and {@link WidgetSpec}
 */
final class StateManager {

    private final WidgetManager.GroupNode rootNode;

    public StateManager(WidgetManager.GroupNode rootNode) {
        this.rootNode = rootNode;
    }

//    MutableList<WidgetManager.SpecNode> nodesToRebuild(){
//
//    }

}
