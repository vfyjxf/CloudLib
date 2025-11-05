package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.Namespace;

public record PluginDependency(
    Namespace pluginId,
    Order order,
    Constraint constraint
) {

    public enum Order {
        BEFORE,
        AFTER,
        NONE
    }

    public enum Constraint {
        /**
         * The plugin dependency is required,when the dependency plugin is not found,the game will crash.
         */
        REQUIRED,
        /**
         * The plugin dependency is optional,when the dependency plugin is not found,the game will continue.
         */
        OPTIONAL,
        /**
         * The plugin dependency is optional,when the dependency plugin is not found,this plugin will be skipped.
         */
        OPTIONAL_REQUIRED
    }

    public boolean required() {
        return constraint == Constraint.REQUIRED;
    }

    public boolean optional() {
        return constraint == Constraint.OPTIONAL;
    }

    public boolean optionalRequired() {
        return constraint == Constraint.OPTIONAL_REQUIRED;
    }
}
