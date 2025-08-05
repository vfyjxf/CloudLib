package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.ui.base.*;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import org.appliedenergistics.yoga.YogaFlexDirection;

@TestScreen
public class TestSpecScreen extends BasicSpecScreen {

    //region stateless
    protected TestSpecScreen(PlanFragment<Widget> mainGroup, PlanFragment<Widget> overlay) {
        super(mainGroup, overlay);
        mainGroup.extend(mainPlan -> mainPlan.group(GroupDef.of(plan ->
        {
            for (int i = 0; i < 20; i++) {
                int finalI = i;
                plan.include(new Widget() {{
                    id = "test_widget_" + finalI;
                    withModifier(Modifier.builder()
                            .size(50, 50)
                            .flexGrow(1)
                            .flexShrink(1)
                            .flexBasisPercent(0.5f)
                    );
                    int color = finalI % 2 == 0 ? 0xFF00FF00 : 0xFFFF0000;
                    onRender((graphics, mouseX, mouseY, partialTicks, self, context) -> {
                        graphics.fill(0, 0, 50, 50, color);
                    });
                }});
            }

            plan.group(GroupDef.of(column -> {

                for (int i = 0; i < 6; i++) {
                    int finalI = i;
                    column.include(new Widget() {{
                        id = "test_widget_column_" + finalI;
                        withModifier(Modifier.builder()
                                .size(50, 30)
                                .flexGrow(1)
                                .flexShrink(1)
                                .flexBasisPercent(0.5f)
                        );
                        int color = finalI % 2 == 0 ? 0xFF00FF00 : 0xFFFF0000;
                        onRender((graphics, mouseX, mouseY, partialTicks, self, context) -> {
                            graphics.fill(0, 0, 50, 50, color);
                        });
                    }});
                }

                return new WidgetGroup<>() {{
                    id = "column";
                    withModifier(Modifier.builder()
                            .size(50, 260)
                            .flexDirection(YogaFlexDirection.COLUMN)
                    );
                }};
            }));

            return new WidgetGroup<>() {{
                id = "test_group0";
                withModifier(Modifier.builder()
//                        .size(200, 200)
                                .flexDirection(YogaFlexDirection.ROW)
                );
            }};
        })));
    }
    //endregion


//    protected TestSpecScreen(PlanFragment<Widget> mainGroup, PlanFragment<Widget> overlay) {
//        super(mainGroup, overlay);
//        mainGroup.extend(mainPlan -> {
//
//        });
//    }
}