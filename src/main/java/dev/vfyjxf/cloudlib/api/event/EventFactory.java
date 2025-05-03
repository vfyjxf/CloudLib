/*
 * MethodHandleLinker
 * Copyright (c) 2024 Burning_TNT<pangyl08@163.com>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.vfyjxf.cloudlib.api.event;

import dev.vfyjxf.cloudlib.utils.Checks;
import dev.vfyjxf.cloudlib.utils.ClassUtils;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * The EventFactory is a factory class for defining {@link EventDefinition}
 *
 * <p>
 * Some basic promises:
 * <p>
 * 1. The naming define events follows the lowercase hump naming
 * <p>
 * 2. Pre-phase events do not need to add the Pre suffix, but Post-phase events need to add the Post suffix.
 * <p>
 * 3. Usually, an event is defined in an interface with its listeners.
 * <p>
 * 4.All event listener must be a functional interface.
 */
public final class EventFactory {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final String FACTORY_CLASS = EventFactory.class.getName().replace('.', '/');
    private static final MutableMap<Class<?>, MethodHandle> WRAPPER_CONSTRUCTORS = Maps.mutable.empty();

    private EventFactory() {
    }

    public static <T> EventDefinition<T> define(Class<T> type, Function<List<T>, T> merger) {
        Checks.checkArgument(ClassUtils.isFunctionalInterface(type), "type must be a functional interface");
        return new EventDefinitionImpl<>(type, merger);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> EventDefinition<T> defineGeneric(Class<? super T> type, Function<List<? extends T>, ? extends T> merger) {
        Checks.checkArgument(ClassUtils.isFunctionalInterface(type), "type must be a functional interface");
        return new EventDefinitionImpl<>(type, (Function) merger);
    }

    @SafeVarargs
    public static <T> Event<T> createEvent(Function<List<T>, T> combiner, T... type) {
        Class<T> genericType = ClassUtils.getGenericType(type);
        return new EventImpl<>(genericType, combiner);
    }

    public static <T> SimpleEvent<T> createSimpleEvent() {
        return new SimpleEventImpl<>();
    }

    static final class EventDefinitionImpl<T> implements EventDefinition<T> {

        private final Class<T> type;
        private final Function<List<T>, T> merger;
        private final Event<T> global;

        private EventDefinitionImpl(Class<T> type, Function<List<T>, T> merger) {
            this.type = type;
            this.merger = merger;
            this.global = new EventImpl<>(type, merger);
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public Event<T> create() {
            return new EventImpl<>(type, merger);
        }

        @Override
        public Event<T> global() {
            return global;
        }

        @Override
        public String toString() {
            return "EventDefinitionImpl{" +
                    "type=" + type.getSimpleName() +
                    '}';
        }
    }

    static final class EventImpl<T> implements Event<T> {
        private final Class<T> type;
        private final MutableMap<T, BooleanSupplier> listenerLifetimeManage;
        private final Function<List<T>, T> merger;
        private final FastList<ListenerEntry<T>> listeners = FastList.newList();
        private T invoker;

        private EventImpl(Class<T> type, Function<List<T>, T> merger) {
            this.type = type;
            this.merger = merger;
            listenerLifetimeManage = Maps.mutable.withInitialCapacity(1);
            MethodHandle handle;
        }

        @Override
        public T invoker() {
            checkLifetime();
            if (invoker == null) {
                update();
            }
            return invoker;
        }

        @Override
        public T register(T listener, int priority) {
            Checks.checkRange(priority, 0, Integer.MAX_VALUE, "priority");
            Checks.checkNotNull(listener, "listener");
            Checks.checkArgument(!isRegistered(listener), "listener is already registered");

            listeners.add(new ListenerEntry<>(listener, priority));
            listeners.sort(ListenerEntry::compareTo);
            invoker = null;
            return listener;
        }

        @Override
        public T registerManaged(T listener, int lifetime) {
            Checks.checkNotNull(listener, "listener");
            Checks.checkArgument(lifetime > 0, "lifetime must be greater than 0");
            AtomicInteger counter = new AtomicInteger(lifetime);
            Method method = ClassUtils.findFunctionalMethod(type);
            assert method != null : "Functional interface must have a single abstract method";
            T wrapper = makeWrapper(type, method, listener, counter);
            counter.decrementAndGet();
            return registerManaged(wrapper, () -> counter.get() <= 0);
        }

        @Override
        public T registerManaged(T listener, BooleanSupplier condition) {
            Checks.checkNotNull(listener, "listener");
            Checks.checkNotNull(condition, "condition");
            register(listener);
            listenerLifetimeManage.put(listener, condition);
            return listener;
        }

        @Override
        public T registerManaged(T listener, Object reference) {
            Checks.checkNotNull(listener, "listener");
            Checks.checkNotNull(reference, "reference");
            WeakReference<Object> ref = new WeakReference<>(reference);
            return registerManaged(listener, () -> ref.get() == null);
        }

        @Override
        public void unregister(T listener) {
            listeners.removeIf(l -> l.listener == listener);
            listenerLifetimeManage.remove(listener);
            listeners.trimToSize();
            invoker = null;
        }

        @Override
        public boolean isRegistered(T listener) {
            return listeners.anySatisfy(l -> l.listener == listener);
        }

        @Override
        public void clearListeners() {
            listeners.clear();
            invoker = null;
        }

        private void update() {
            if (listeners.size() == 1) {
                invoker = listeners.getFirst().listener;
            } else {
                invoker = merger.apply(listeners.collect(ListenerEntry::listener));
            }
        }

        private void checkLifetime() {
            if (!listenerLifetimeManage.isEmpty()) {
                int size = listeners.size();
                for (Iterator<ListenerEntry<T>> iterator = listeners.iterator(); iterator.hasNext(); ) {
                    T listener = iterator.next().listener;
                    var manage = listenerLifetimeManage.get(listener);
                    if (manage != null && manage.getAsBoolean()) {
                        iterator.remove();
                        listenerLifetimeManage.remove(listener);
                    }
                }
                if (size != listeners.size()) {
                    invoker = null;
                }
            }
        }

        private record ListenerEntry<T>(T listener, int priority) implements Comparable<ListenerEntry<T>> {
            @Override
            public int compareTo(ListenerEntry<T> o) {
                return Integer.compare(o.priority, priority);
            }
        }

    }

    static final class SimpleEventImpl<T> implements SimpleEvent<T> {
        private final FastList<T> listeners = FastList.newList();

        @Override
        public void invoke(Consumer<T> invoker) {
            for (T listener : listeners) {
                invoker.accept(listener);
            }
        }

        @Override
        public T register(T listener) {
            if (isRegistered(listener)) {
                throw new IllegalArgumentException("Listener is already registered");
            }
            listeners.add(listener);
            return listener;
        }

        @Override
        public void unregister(T listener) {
            listeners.remove(listener);
            listeners.trimToSize();
        }

        @Override
        public boolean isRegistered(T listener) {
            return listeners.contains(listener);
        }

        @Override
        public void clearListeners() {
            listeners.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T makeWrapper(Class<T> interfaceClass, Method method, T listener, AtomicInteger counter) {
        try {
            return (T) WRAPPER_CONSTRUCTORS.getIfAbsentPut(interfaceClass, () -> {
                try {
                    byte[] bytes = makeWrapperClass(interfaceClass, method);
                    MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(bytes, true);
                    return lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class, interfaceClass, AtomicInteger.class));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create wrapper class", e);
                }
            }).invokeWithArguments(listener, counter);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] makeWrapperClass(Class<?> interfaceClass, Method method) {
        MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        String interfaceName = interfaceClass.getName().replace('.', '/');
        String typeName = FACTORY_CLASS + "$" + interfaceClass.getSimpleName() + "$" + method.getName() + "Wrapper";
        String interfaceDesc = "L" + interfaceName + ";";
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        cw.visit(V16, ACC_PUBLIC | ACC_SUPER, typeName, null, "java/lang/Object", new String[]{interfaceName});
        {
            fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, "delegate", interfaceDesc, null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, "counter", "Ljava/util/concurrent/atomic/AtomicInteger;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + interfaceDesc + "Ljava/util/concurrent/atomic/AtomicInteger;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, typeName, "delegate", interfaceDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(PUTFIELD, typeName, "counter", "Ljava/util/concurrent/atomic/AtomicInteger;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, method.getName(), methodType.toMethodDescriptorString(), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, typeName, "counter", "Ljava/util/concurrent/atomic/AtomicInteger;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "decrementAndGet", "()I", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, typeName, "delegate", interfaceDesc);
            int maxStack = 1, l = methodType.parameterCount();
            for (int i = 0, varIndex = 1; i < l; i++) {
                VMStackType type = VMStackType.of(methodType.parameterType(i));
                if (type == VMStackType.VOID) {
                    throw new AssertionError("Parameters should NOT be void.");
                }
                mv.visitVarInsn(type.loadingOpcode, varIndex);
                maxStack += type.stackWidth;
                varIndex += type.stackWidth;
            }
            mv.visitMethodInsn(INVOKEINTERFACE, interfaceName, method.getName(), methodType.toMethodDescriptorString(), true);
            VMStackType type = VMStackType.of(methodType.returnType());
            mv.visitInsn(type.returnOpcode);
            maxStack = Math.max(maxStack, 1 + type.stackWidth);
            mv.visitMaxs(maxStack, maxStack);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /**
     * <a href="https://gist.github.com/burningtnt/65e1d9bfb2000e69c852335b178692e8">code from</a>
     */
    private enum VMStackType {
        OBJECT(ALOAD, Opcodes.ARETURN, 1),
        NUMBER(Opcodes.ILOAD, Opcodes.IRETURN, 1),
        FLOAT(Opcodes.FLOAD, Opcodes.FRETURN, 1),
        DOUBLE(Opcodes.DLOAD, Opcodes.DRETURN, 2),
        LONG(Opcodes.LLOAD, Opcodes.LRETURN, 2),
        VOID(-1, Opcodes.RETURN, 0);

        private final int loadingOpcode, returnOpcode, stackWidth;

        VMStackType(int loadingOpcode, int returnOpcode, int stackWidth) {
            this.loadingOpcode = loadingOpcode;
            this.returnOpcode = returnOpcode;
            this.stackWidth = stackWidth;
        }

        public static VMStackType of(Class<?> clazz) {
            if (!clazz.isPrimitive()) {
                return OBJECT;
            } else if (clazz == int.class || clazz == short.class || clazz == char.class || clazz == byte.class || clazz == boolean.class) {
                return NUMBER;
            } else if (clazz == float.class) {
                return FLOAT;
            } else if (clazz == double.class) {
                return DOUBLE;
            } else if (clazz == long.class) {
                return LONG;
            } else {
                return VOID;
            }
        }
    }

}
