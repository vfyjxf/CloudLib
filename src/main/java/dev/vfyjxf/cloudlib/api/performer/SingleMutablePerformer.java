package dev.vfyjxf.cloudlib.api.performer;

class SingleMutablePerformer<T> implements MutablePerformer<T> {
    private T performer;

    public SingleMutablePerformer(T performer) {
        this.performer = performer;
    }

    @Override
    public void put(T performer) {
        this.performer = performer;
    }

    @Override
    public void remove(T performer) {
        if (this.performer == performer) {
            this.performer = null;
        }
    }

    @Override
    public T performer() {
        return performer;
    }
}
