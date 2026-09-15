package dev.vfyjxf.cloudlib.data.lang;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import net.minecraft.data.PackOutput;

/**
 * en_us entries owned by code (blocks, items, ...) rather than by the hand-written yaml
 * files. Output lands in {@code build/generated/datagen/lang/en_us/} and is merged over
 * {@code src/main/lang} by the cloudLang gradle plugin.
 */
public final class CloudLibLangProvider extends DistributedLangProvider {

    public CloudLibLangProvider(PackOutput output) {
        super(output, Constants.modId, "en_us");
    }

    @Override
    protected void addTranslations() {
        file("blocks").add(TestRegistry.testBlock.get(), "Test Block");
    }

}
