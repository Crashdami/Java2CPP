package ru.nexusguard.protection.transpiler.core.runner;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class InvokeDynamicHelper {
    private static final ConcurrentHashMap<String, MethodHandle> HANDLE_CACHE = new ConcurrentHashMap<>();

    private InvokeDynamicHelper() {
    }

    public static Runnable createRunnable(String ownerInternal, String name, String desc) {
        try {
            MethodHandle handle = resolveHandle(ownerInternal, name, desc);
            MethodHandle bound = handle;
            if (bound.type().parameterCount() > 0) {
                throw new IllegalStateException("Runnable expects zero parameters.");
            }
            MethodHandle typed = bound.asType(MethodType.methodType(void.class));
            return () -> {
                try {
                    typed.invokeExact();
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            };
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Supplier<?> createSupplier(String ownerInternal, String name, String desc, Object captured) {
        try {
            MethodHandle handle = resolveHandle(ownerInternal, name, desc);
            int params = handle.type().parameterCount();
            MethodHandle bound = handle;
            if (params == 1) {
                bound = handle.bindTo(captured);
            } else if (params != 0) {
                throw new IllegalStateException("Supplier expects zero or one parameter.");
            }
            MethodHandle typed = bound.asType(MethodType.methodType(Object.class));
            return () -> {
                try {
                    return typed.invokeExact();
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            };
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static MethodHandle resolveHandle(String ownerInternal, String name, String desc) throws Throwable {
        String key = ownerInternal + "#" + name + desc;
        MethodHandle cached = HANDLE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        MethodHandle handle = resolveHandleSlow(ownerInternal, name, desc);
        MethodHandle existing = HANDLE_CACHE.putIfAbsent(key, handle);
        return existing == null ? handle : existing;
    }

    private static MethodHandle resolveHandleSlow(String ownerInternal, String name, String desc) throws Throwable {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String className = ownerInternal.replace('/', '.');
        Class<?> owner = Class.forName(className, true, loader);
        MethodType methodType = MethodType.fromMethodDescriptorString(desc, owner.getClassLoader());
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        try {
            return lookup.findStatic(owner, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        try {
            return lookup.findVirtual(owner, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        try {
            return lookup.findSpecial(owner, name, methodType, owner);
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        Method method = owner.getDeclaredMethod(name, methodType.parameterArray());
        method.setAccessible(true);
        return lookup.unreflect(method);
    }
}
