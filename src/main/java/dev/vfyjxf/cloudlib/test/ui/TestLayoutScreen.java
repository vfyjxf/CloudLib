package dev.vfyjxf.cloudlib.test.ui;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.alignment.Alignment;
import dev.vfyjxf.cloudlib.api.ui.drag.DragConsumer;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.layout.ColumnResizer;
import dev.vfyjxf.cloudlib.api.ui.layout.GridResizer;
import dev.vfyjxf.cloudlib.api.ui.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widgets;
import dev.vfyjxf.cloudlib.helper.RenderHelper;
import dev.vfyjxf.cloudlib.ui.BaseScreen;

@TestScreen
public class TestLayoutScreen extends BaseScreen {

    private TestLayoutScreen() {
        //render background by event
        mainGroup.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
            RenderHelper.drawSolidRect(graphics, 0, 0, mainGroup.getWidth(), mainGroup.getHeight(), 0xff282c34);
        });
        var column = new WidgetGroup<>();
        {
            column.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
                RenderHelper.drawSolidRect(graphics, 0, 0, column.getWidth(), column.getHeight(), 0xff1a1a1a);
            });
            column.withModifier(
                    Modifier()
                            .fillMaxWidth(0.3)
                            .fillMaxHeight(1)
//                            .scale(0.5F)
                            .layoutWith(ColumnResizer::new, l -> {})
            );
            column.asChild(mainGroup);
            WidgetEvent.OnOverlayRender overlayRender = ((graphics, mouseX, mouseY, partialTicks, context) -> {
                Widget poster = context.poster();
                RenderHelper.drawSolidRect(graphics, 0, 0, poster.getWidth(), poster.getHeight(), 0xff3a3a3a);
            });
            for (int i = 0; i < 10; i++) {
                var label = Widgets.text("Label " + i)
                        .horizontalAlignment(Alignment.CenterHorizontally);
                label.mark("ColumnLabel" + i);
                label.onOverlayRender(overlayRender);
                label.withModifier(
                        Modifier()
                                .fillMaxWidth(1)
                                .heightIn(20, 40)
                                .heightFixed(20)
                                .padding(4)
                ).asChild(column);
            }
        }

        var grid = mainGroup().addWidget(new WidgetGroup<>());
        {
            grid.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
                RenderHelper.drawSolidRect(graphics, 0, 0, grid.getWidth(), grid.getHeight(), 0xff2a2a2a);
            });
            grid.withModifier(
                    Modifier()
                            .posRel(0.3, 0)
                            .fillMaxSize(0.7, 1)
                            .layoutWith(GridResizer::new, l -> l.items(10))
            );
            WidgetEvent.OnOverlayRender onRender = ((graphics, mouseX, mouseY, partialTicks, context) -> {
                Widget poster = context.poster();
                RenderHelper.drawSolidRect(graphics, 0, 0, poster.getWidth(), poster.getHeight(), 0xff3a3a3a);
            });
            var labelModifier = Modifier()
                    .size(10, 20)
                    .padding(4);
            for (int i = 0; i < 30; i++) {
                var label = Widgets.text("L" + i);
                label.mark("GridLabel" + i);
                label.onOverlayRender(onRender);
                label.withModifier(labelModifier).asChild(grid);
            }
        }

        var draggable = mainGroup().addWidget(Widget.create());
        {
            draggable.mark("draggable");
            draggable.setDraggable(true);
            var modifier = Modifier.builder()
                    .posRel(0.5, 0.5)
                    .size(30, 30);
            draggable.withModifier(
                    modifier
            );
            draggable.onRender(((graphics, mouseX, mouseY, partialTicks, context) -> {
                RenderHelper.drawSolidRect(graphics, 0, 0, draggable.getWidth(), draggable.getHeight(), 0xffdb1616);
            }));
        }
        var dragConsumerWidget = mainGroup().addWidget(new WidgetGroup<>());
        {
            dragConsumerWidget.onRender(((graphics, mouseX, mouseY, partialTicks, context) -> {
                RenderHelper.drawSolidRect(graphics, 0, 0, dragConsumerWidget.getWidth(), dragConsumerWidget.getHeight(), 0xff2a2a2a);
            }));
            dragConsumerWidget.setDraggable(true);
            dragConsumerWidget.addActor(
                    DragConsumer.ACTOR_KEY,
                    DragConsumer.forWidgetConsumer(
                            ((widget, context) -> widget.getId().startsWith("draggable") &&
                                    dragConsumerWidget.intersects(context.draggingBounds())),
                            (element, context) -> {
                                Widget value = element.value();
                                var parent = value.parent();
                                if (parent == null) return false;
                                Pos pos = context.relativePos(dragConsumerWidget);
                                parent.remove(value);
                                value.withModifier(
                                        Modifier.builder()
                                                .resetPos()
                                                .pos(pos.x, pos.y)
                                );
                                value.setPos(pos);
                                value.asChild(dragConsumerWidget);
                                return true;
                            }
                    )
            );

            dragConsumerWidget.withModifier(
                    Modifier.builder()
                            .posRel(0.6, 0.6)
                            .size(100, 100)
            );
        }


//        var row = mainGroup().addWidget(new WidgetGroup<>());
//        {
//            row.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
//                RenderHelper.drawSolidRect(graphics, 0, 0, row.getWidth(), row.getHeight(), 0xff2a2a2a);
//            });
//            row.withModifier(
//                    Modifier()
//                            .fillMaxWidth(1)
//                            .heightIn(20, 40)
//                            .layoutWith(RowResizer::new)
//            );
//            for (int i = 0; i < 5; i++) {
//                var label = Widgets.text("Label " + i);
//                label.mark("RowLabel" + i);
//                var font = Minecraft.getInstance().font;
//                int labelWidth = font.width(label.text());
//                var labelModifier = Modifier()
//                        .fillMaxHeight(1)
//                        .margin(3,0)
//                        .widthIn(labelWidth, 40)
//                        .offset(3, 2);
//                label.withModifier(labelModifier)
//                        .asChild(row);
//            }
//        }
//        var row2 = mainGroup().addWidget(new WidgetGroup<>());
//        {
//            row2.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
//                RenderHelper.drawSolidRect(graphics, 0, 0, row2.getWidth(), row2.getHeight(), 0xff2a2a2a);
//            });
//            row2.withModifier(
//                    Modifier()
//                            .posY(20)
//                            .fillMaxWidth(1)
//                            .heightIn(20, 40)
//                            .padding(3)
//                            .layoutWith(RowResizer::new)
//            );
//            for (int i = 0; i < 5; i++) {
//                var label = Widgets.text("Label " + i);
//                label.mark("Row2Label" + i);
//                var font = Minecraft.getInstance().font;
//                int labelWidth = font.width(label.text());
//                var labelModifier = Modifier()
//                        .fillMaxHeight(1)
//                        .margin(10,0)
//                        .widthIn(labelWidth, 40);
//                label.withModifier(labelModifier)
//                        .asChild(row2);
//            }
//        }
//
//        var grid = mainGroup.addWidget(new WidgetGroup<>());
//        {
//            grid.withModifier(
//                    Modifier()
//                            .posRel(0.3, 0.4)
//                            .fillMaxWidth(0.7)
//                            .fillMaxHeight(0.5)
//                            .layoutWith(GridResizer::new)
//            );
//            for (int i = 0; i < 20; i++) {
//                var label = Widgets.label("Label " + i);
//                var labelModifier = Modifier()
//                        .widthIn(10, 20)
//                        .heightFixed(20);
//                label.withModifier(labelModifier)
//                        .asChild(grid);
//            }
//        }
    }


}
