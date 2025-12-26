package ru.nexusguard.protection.transpiler.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public final class NativeClassRewriter {
    private static final String NATIVE_DESC = "Lru/nexusguard/protection/annotations/Native;";
    private static final String RUNNER_INTERNAL = "ru/nexusguard/protection/native/NativeRunner";
    private static final String RUNNER_DESC = "(Ljava/lang/String;)V";

    public ClassRewrite rewrite(byte[] classBytes) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        boolean changed = false;
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
            method.access |= Opcodes.ACC_NATIVE;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) {
                method.localVariables.clear();
            }
            method.maxStack = 0;
            method.maxLocals = 0;
            cleanAnnotation(method);
            changed = true;
        }

        if (!changed) {
            return null;
        }

        injectClinit(node);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        String entryName = node.name + ".class";
        return new ClassRewrite(entryName, writer.toByteArray());
    }

    private void injectClinit(ClassNode node) {
        MethodNode clinit = findClinit(node.methods);
        if (clinit == null) {
            MethodNode created = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            created.instructions.add(buildRunnerCall(node.name));
            created.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(created);
            return;
        }

        if (containsRunnerCall(clinit)) {
            return;
        }
        insertRunnerCall(clinit, node.name);
    }

    private MethodNode findClinit(List<MethodNode> methods) {
        for (MethodNode method : methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private boolean containsRunnerCall(MethodNode method) {
        for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call) {
                if (RUNNER_INTERNAL.equals(call.owner) && "call".equals(call.name) && RUNNER_DESC.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private InsnList buildRunnerCall(String internalName) {
        InsnList inject = new InsnList();
        inject.add(new LdcInsnNode(internalName));
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RUNNER_INTERNAL, "call", RUNNER_DESC, false));
        return inject;
    }

    private void insertRunnerCall(MethodNode clinit, String internalName) {
        AbstractInsnNode insertBefore = null;
        for (AbstractInsnNode node = clinit.instructions.getLast(); node != null; node = node.getPrevious()) {
            if (node.getOpcode() == Opcodes.RETURN) {
                insertBefore = node;
                break;
            }
        }

        InsnList call = buildRunnerCall(internalName);
        if (insertBefore != null) {
            clinit.instructions.insertBefore(insertBefore, call);
        } else {
            clinit.instructions.add(call);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        }
    }

    private boolean hasNativeAnnotation(MethodNode method) {
        return hasAnnotation(method.visibleAnnotations) || hasAnnotation(method.invisibleAnnotations);
    }

    private void cleanAnnotation(MethodNode method) {
        method.visibleAnnotations = removeNativeAnnotation(method.visibleAnnotations);
        method.invisibleAnnotations = removeNativeAnnotation(method.invisibleAnnotations);
    }

    private List<AnnotationNode> removeNativeAnnotation(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null;
        }
        annotations.removeIf(node -> NATIVE_DESC.equals(node.desc));
        return annotations.isEmpty() ? null : annotations;
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
