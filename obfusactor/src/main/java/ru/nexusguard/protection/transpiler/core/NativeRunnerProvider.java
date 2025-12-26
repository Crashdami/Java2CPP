package ru.nexusguard.protection.transpiler.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class NativeRunnerProvider {
    private static final String SOURCE_INTERNAL = "ru/nexusguard/protection/transpiler/core/runner/NativeRunner";
    private static final String TARGET_INTERNAL = "ru/nexusguard/protection/native/NativeRunner";

    public String entryName() {
        return TARGET_INTERNAL + ".class";
    }

    public byte[] loadBytes() throws IOException {
        String resource = SOURCE_INTERNAL + ".class";
        try (InputStream input = NativeRunnerProvider.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("NativeRunner class not found: " + resource);
            }
            byte[] data = input.readAllBytes();
            ClassReader reader = new ClassReader(data);
            ClassWriter writer = new ClassWriter(0);
            var remapper = new SimpleRemapper(Map.of(SOURCE_INTERNAL, TARGET_INTERNAL));
            ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
            reader.accept(classRemapper, 0);
            return writer.toByteArray();
        }
    }
}
