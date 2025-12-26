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

public final class IincInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof IincInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            IincInsnNode node = (IincInsnNode) insn;
            out.line("frame.locals.setI64(" + node.var + ", frame.locals.getI64(" + node.var + ") + " + node.incr + ");");
        }
    }
