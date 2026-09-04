package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.Namespace;

public record PluginDependency(
        Namespace pluginId,
        Order order,
        Constraint constraint
) {

    public enum Order {
        before,
        after,
        none
    }

    public enum Constraint {
        /**
         * The plugin dependency is required,when the dependency plugin is not found,the game will crash.
         */
        required,
        /**
         * The plugin dependency is optional,when the dependency plugin is not found,the game will continue.
         */
        optional,
        /**
         * The plugin dependency is optional,when the dependency plugin is not found,this plugin will be skipped.
         */
        optionalRequired
    }

    public boolean required() {
        return constraint == Constraint.required;
    }

    public boolean optional() {
        return constraint == Constraint.optional;
    }

    public boolean optionalRequired() {
        return constraint == Constraint.optionalRequired;
    }
}
