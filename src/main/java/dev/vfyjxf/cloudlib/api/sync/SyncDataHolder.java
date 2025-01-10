package dev.vfyjxf.cloudlib.api.sync;

import java.util.function.Consumer;

public interface SyncDataHolder {

    SyncDataContainer syncDataContainer();

    @SuppressWarnings("unchecked")
    default <O extends SyncDataHolder, T> void onDataReceive(SyncValue<O, T> syncValue, Consumer<T> dataConsumer, O... tokenCatcher) {
        checkType(tokenCatcher);
        SyncManager.onReceive(
                (O) this,
                syncValue,
                dataConsumer
        );
    }

    @SuppressWarnings("unchecked")
    default <O extends SyncDataHolder, T> void sendData(SyncValue<O, T> syncValue, T data, O... tokenCatcher) {
        checkType(tokenCatcher);
        SyncManager.sendData((O) this, syncValue, data);
    }

    @SuppressWarnings("unchecked")
    default <O extends SyncDataHolder, T> Consumer<T> dataSender(SyncValue<O, T> syncValue, O... tokenCatcher) {
        checkType(tokenCatcher);
        return SyncManager.sender((O) this, syncValue);
    }

    private void checkType(Object[] tokenCatcher) {
        if (tokenCatcher.length != 0) {
            throw new IllegalArgumentException("Token catcher should be empty");
        }
        System.out.println(tokenCatcher.getClass().getComponentType());
        if (!tokenCatcher.getClass().getComponentType().isInstance(this)) {
            throw new IllegalArgumentException("The receiver is not the same type as the bind type");
        }
    }

}
