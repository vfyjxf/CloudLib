package dev.vfyjxf.cloudlib.api.ui.element;

/**
 * Lifecycle states of an Element.
 */
public enum ElementLifecycle {
    /**
     * Element has been created but not yet mounted.
     */
    CREATED,
    
    /**
     * Element is mounted and active in the tree.
     */
    MOUNTED,
    
    /**
     * Element has been unmounted and is no longer active.
     */
    UNMOUNTED
}
