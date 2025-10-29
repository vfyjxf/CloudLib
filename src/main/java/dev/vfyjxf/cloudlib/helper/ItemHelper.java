package dev.vfyjxf.cloudlib.helper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import static net.minecraft.world.item.ItemStack.ITEM_NON_AIR_CODEC;

public final class ItemHelper {

    private ItemHelper() {}

    /**
     * Codec without count size limitation
     */
    public static final Codec<ItemStack> codec = Codec.lazyInitialized(
        () -> RecordCodecBuilder.create(
            instance -> instance.group(
                ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                DataComponentPatch.CODEC
                    .optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(ItemStack::getComponentsPatch)
            ).apply(instance, ItemStack::new)
        )
    );

    /**
     * Optional codec without count size limitation
     */
    public static final Codec<ItemStack> optionalCodec =
        ExtraCodecs.optionalEmptyMap(codec)
                   .xmap(op -> op.orElse(ItemStack.EMPTY),
                       stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack)
                   );

}

