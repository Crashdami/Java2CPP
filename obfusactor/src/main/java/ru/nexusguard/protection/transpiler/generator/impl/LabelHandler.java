package ru.nexusguard.protection.transpiler.generator.impl;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import ru.nexusguard.protection.transpiler.generator.CodeWriter;
import ru.nexusguard.protection.transpiler.generator.MethodContext;
import ru.nexusguard.protection.transpiler.util.CppStringEscaper;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

import java.util.List;

public final class LabelHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof LabelNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            LabelNode label = (LabelNode) insn;
            out.line(context.label(label) + ":");
        }
    }
