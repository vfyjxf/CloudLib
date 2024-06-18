package dev.vfyjxf.cloudlib.helper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.world.item.ItemStack.ITEM_NON_AIR_CODEC;

public final class ItemHelper {

    private ItemHelper() {
    }

    /**
     * Codec without count size limitation
     */
    public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(
            () -> RecordCodecBuilder.create(
                    instance -> instance.group(
                                    ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                                    ExtraCodecs.intRange(1, Integer.MAX_VALUE).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
                            )
                            .apply(instance, ItemStack::new)
            )
    );

    public static CompoundTag save(ItemStack stack) {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow();
    }

    public static ItemStack read(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

}

