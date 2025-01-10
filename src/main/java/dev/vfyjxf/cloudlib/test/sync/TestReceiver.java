package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.sync.SyncManager;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

public class TestReceiver {

    private int valueFrom;

    public TestReceiver(TestDataHolder bindTo) {
        SyncManager.onReceive(
                bindTo,
                TestDataHolder.manage,
                data -> {//请清理我
                    valueFrom = data;
                    System.out.println("Received data: " + valueFrom);
                }
        );
        var sender = SyncManager.sender(
                bindTo,
                TestDataHolder.manage
        );
        var button = Widget.create();
        button.onMouseClicked(((input, context) -> {
            sender.accept(valueFrom++);
            return true;
        }));
    }

}
