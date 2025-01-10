package dev.vfyjxf.cloudlib.api.actor;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
public class ActorSystemTest {

    private static final MergeableActorKey<TestActor> mergeableActorKey = new MergeableActorKey<>(
            ResourceLocation.fromNamespaceAndPath("test", "mergeable"),
            TestActor.class,
            actors -> new TestActor() {
                @Override
                public void test() {
                    for (TestActor actor : actors) {
                        actor.test();
                    }
                }
            }
    );
    private MergeableActor<TestActor> mergeableActor;


    @BeforeEach
    void setup() {
        mergeableActor = new TestMergeableActor<>(mergeableActorKey);
        mergeableActor.put(() -> System.out.println("test1"));
        mergeableActor.put(() -> System.out.println("test2"));
        mergeableActor.put(() -> System.out.println("test high"), 100);
        mergeableActor.put(() -> System.out.println("test low"), -100);
    }

    @Test
    void test() {
        mergeableActor.actor().test();
    }

    interface TestActor {
        void test();
    }

    private static class TestMergeableActor<T> extends MergeableActor<T> {
        public TestMergeableActor(MergeableActorKey<T> key) {
            super(key.merger());
        }
    }
}
