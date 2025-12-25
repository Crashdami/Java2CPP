package ru.nexusguard.protection.transpiler.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeClassProvider {
    private static final String RUNNER_SOURCE = "ru/nexusguard/protection/transpiler/core/runner/NativeRunner";
    private static final String RUNNER_TARGET = "ru/nexusguard/protection/native/NativeRunner";
    private static final String DYNAMIC_SOURCE = "ru/nexusguard/protection/transpiler/core/runner/InvokeDynamicHelper";
    private static final String DYNAMIC_TARGET = "ru/nexusguard/protection/native/InvokeDynamicHelper";

    public Map<String, byte[]> loadAll() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(entryName(RUNNER_TARGET), loadRemapped(RUNNER_SOURCE, RUNNER_TARGET));
        entries.put(entryName(DYNAMIC_TARGET), loadRemapped(DYNAMIC_SOURCE, DYNAMIC_TARGET));
        return entries;
    }

    private String entryName(String internalName) {
        return internalName + ".class";
    }

    private byte[] loadRemapped(String sourceInternal, String targetInternal) throws IOException {
        String resource = sourceInternal + ".class";
        try (InputStream input = RuntimeClassProvider.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Runtime class not found: " + resource);
            }
            byte[] data = input.readAllBytes();
            ClassReader reader = new ClassReader(data);
            ClassWriter writer = new ClassWriter(0);
            var remapper = new SimpleRemapper(Map.of(sourceInternal, targetInternal));
            ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
            reader.accept(classRemapper, 0);
            return writer.toByteArray();
        }
    }
}
