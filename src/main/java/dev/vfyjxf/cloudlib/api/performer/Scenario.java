package dev.vfyjxf.cloudlib.api.performer;

import net.minecraft.resources.ResourceLocation;

public record Scenario<T>(ResourceLocation id, Class<T> type) {
}
