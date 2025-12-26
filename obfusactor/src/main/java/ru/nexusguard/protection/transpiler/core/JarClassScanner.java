package ru.nexusguard.protection.transpiler.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

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
                    inlineSubroutines(node);
                    classes.add(new ClassSource(node.name, bytes, node));
                }
            }
        }
        return classes;
    }

    private void inlineSubroutines(ClassNode node) {
        if (node.methods == null || node.methods.isEmpty()) {
            return;
        }
        for (int i = 0; i < node.methods.size(); i++) {
            MethodNode method = node.methods.get(i);
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            String[] exceptions = method.exceptions == null
                    ? null
                    : method.exceptions.toArray(new String[0]);
            MethodNode out = new MethodNode(Opcodes.ASM9, method.access, method.name, method.desc, method.signature, exceptions);
            JSRInlinerAdapter adapter = new JSRInlinerAdapter(out, method.access, method.name, method.desc, method.signature, exceptions);
            method.accept(adapter);
            node.methods.set(i, out);
        }
    }
}
