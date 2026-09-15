package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * One world obstacle. {@code solid} shapes collide with physical panels
 * and leader world segments; {@code opaque} shapes additionally occlude
 * visibility and leaders. {@code material} is a free-form tag for hosts.
 */
public record Obstacle(String id, ObstacleShape shape, boolean solid, boolean opaque, String material) {}
