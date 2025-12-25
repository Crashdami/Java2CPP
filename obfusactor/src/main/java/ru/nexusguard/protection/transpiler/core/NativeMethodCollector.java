package ru.nexusguard.protection.transpiler.core;

import ru.nexusguard.protection.transpiler.model.ClassModel;
import ru.nexusguard.protection.transpiler.model.MethodModel;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public final class NativeMethodCollector {
    private static final String NATIVE_DESC = "Lru/nexusguard/protection/annotations/Native;";

    public ClassModel collect(ClassNode node) {
        List<MethodModel> methods = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (!hasNativeAnnotation(method)) {
                continue;
            }

            if ((method.access & Opcodes.ACC_NATIVE) != 0) {
                continue;
            }

            if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
                continue;
            }

            methods.add(new MethodModel(node.name, method));
        }

        if (methods.isEmpty()) {
            return null;
        }
        return new ClassModel(node.name, methods);
    }

    private boolean hasNativeAnnotation(MethodNode method) {
        return hasAnnotation(method.visibleAnnotations) || hasAnnotation(method.invisibleAnnotations);
    }

    private boolean hasAnnotation(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode node : annotations) {
            if (NATIVE_DESC.equals(node.desc)) {
                return true;
            }
        }
        return false;
    }
}
