package ru.nexusguard.protection.transpiler.generator.impl;

import org.objectweb.asm.tree.AbstractInsnNode;

import ru.nexusguard.protection.transpiler.generator.CodeWriter;
import ru.nexusguard.protection.transpiler.generator.MethodContext;

public interface InstructionHandler {
    boolean supports(AbstractInsnNode insn);

    void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out);
}
