package dev.vfyjxf.cloudlib.api.performer;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
public class PerformerSystemTest {

    private static final CompositeScenario<TestPerformer> CHAINED_SCENARIO = new CompositeScenario<>(
            ResourceLocation.fromNamespaceAndPath("test", "mergeable"),
            TestPerformer.class,
            performers -> new TestPerformer() {
                @Override
                public void test() {
                    for (TestPerformer performer : performers) {
                        performer.test();
                    }
                }
            }
    );
    private MergeablePerformer<TestPerformer> mergeablePerformer;


    @BeforeEach
    void setup() {
        mergeablePerformer = new TestMergeablePerformer<>(CHAINED_SCENARIO);
        mergeablePerformer.put(() -> System.out.println("test1"));
        mergeablePerformer.put(() -> System.out.println("test2"));
        mergeablePerformer.put(() -> System.out.println("test high"), 100);
        mergeablePerformer.put(() -> System.out.println("test low"), -100);
    }

    @Test
    void test() {
        mergeablePerformer.performer().test();
    }

    interface TestPerformer {
        void test();
    }

    private static class TestMergeablePerformer<T> extends MergeablePerformer<T> {
        public TestMergeablePerformer(CompositeScenario<T> key) {
            super(key.merger());
        }
    }
}
