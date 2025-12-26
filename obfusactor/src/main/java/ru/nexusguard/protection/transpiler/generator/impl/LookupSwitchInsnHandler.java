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

public final class LookupSwitchInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof LookupSwitchInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            LookupSwitchInsnNode node = (LookupSwitchInsnNode) insn;
            String key = context.temp("ls_key");
            out.line("int64_t " + key + " = frame.stack.popI64();");
            out.line("switch (" + key + ") {");
            for (int i = 0; i < node.keys.size(); i++) {
                Integer value = node.keys.get(i);
                out.line("case " + value + ": goto " + context.label(node.labels.get(i)) + ";");
            }
            out.line("default: goto " + context.label(node.dflt) + ";");
            out.line("}");
        }
    }
