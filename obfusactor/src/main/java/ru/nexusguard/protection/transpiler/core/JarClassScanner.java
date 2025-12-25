package ru.nexusguard.protection.transpiler.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarClassScanner {
    public List<ClassSource> scan(Path jarPath) throws IOException {
        List<ClassSource> classes = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                try (InputStream input = jarFile.getInputStream(entry)) {
                    byte[] bytes = input.readAllBytes();
                    ClassReader reader = new ClassReader(bytes);
                    ClassNode node = new ClassNode();
                    reader.accept(node, ClassReader.SKIP_DEBUG);
                    classes.add(new ClassSource(node.name, bytes, node));
                }
            }
        }
        return classes;
    }
}
