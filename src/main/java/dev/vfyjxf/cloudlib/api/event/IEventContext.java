package dev.vfyjxf.cloudlib.api.event;

public sealed interface IEventContext permits IEventContext.Common, IEventContext.Cancelable, IEventContext.Interruptible {

    IEventChannel<?> getManager();

    @SuppressWarnings("unchecked")
    default <T> T holder() {
        return (T) getManager().holder();
    }

    boolean isCancelled();

    boolean isInterrupted();

    final class Common implements IEventContext {

        private final IEventChannel<?> manager;
        private boolean cancelled = false;
        private boolean interrupted = false;

        public Common(IEventChannel<?> manager) {
            this.manager = manager;
        }

        @Override
        public IEventChannel<?> getManager() {
            return manager;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            cancelled = true;
            interrupted = true;
        }

        public void interrupt() {
            interrupted = true;
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }
    }

    final class Cancelable implements IEventContext {

        private final IEventChannel<?> manager;
        private boolean cancelled = false;

        public Cancelable(IEventChannel<?> manager) {
            this.manager = manager;
        }

        @Override
        public IEventChannel<?> getManager() {
            return manager;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isInterrupted() {
            return cancelled;
        }

    }

    final class Interruptible implements IEventContext {

        private final IEventChannel<?> manager;
        private boolean interrupted = false;

        public Interruptible(IEventChannel<?> manager) {
            this.manager = manager;
        }

        @Override
        public IEventChannel<?> getManager() {
            return manager;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        public void interrupt() {
            interrupted = true;
        }

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }
    }
}
