package ru.nexusguard.protection.transpiler.core.runner;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public final class InvokeDynamicHelper {
    private InvokeDynamicHelper() {
    }

    public static Runnable createRunnable(String ownerInternal, String name, String desc) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String className = ownerInternal.replace('/', '.');
            Class<?> owner = Class.forName(className, true, loader);
            MethodType methodType = MethodType.fromMethodDescriptorString(desc, owner.getClassLoader());
            Method method = owner.getDeclaredMethod(name, methodType.parameterArray());
            method.setAccessible(true);
            return () -> {
                try {
                    method.invoke(null);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            };
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
