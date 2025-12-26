package ru.nexusguard.protection.transpiler.generator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import ru.nexusguard.protection.transpiler.generator.impl.*;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

import java.util.ArrayList;
import java.util.List;

public final class BytecodeTranslator {
    private final List<InstructionHandler> handlers;

    public BytecodeTranslator(TypeMapper typeMapper) {
        handlers = new ArrayList<>();
        handlers.add(new LabelHandler());
        handlers.add(new FrameHandler());
        handlers.add(new LineNumberHandler());
        handlers.add(new IntInsnHandler());
        handlers.add(new VarInsnHandler());
        handlers.add(new InsnHandler(typeMapper));
        handlers.add(new LdcInsnHandler(typeMapper));
        handlers.add(new IincInsnHandler());
        handlers.add(new JumpInsnHandler());
        handlers.add(new TableSwitchInsnHandler());
        handlers.add(new LookupSwitchInsnHandler());
        handlers.add(new MultiANewArrayInsnHandler());
        handlers.add(new FieldInsnHandler(typeMapper));
        handlers.add(new InvokeDynamicInsnHandler(typeMapper));
        handlers.add(new MethodInsnHandler(typeMapper));
        handlers.add(new TypeInsnHandler());
    }

    public void emit(MethodContext context, CodeWriter out) {
        InsnList instructions = context.method().node().instructions;
        for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
            boolean handled = false;
            for (InstructionHandler handler : handlers) {
                if (handler.supports(insn)) {
                    handler.emit(insn, context, out);
                    handled = true;
                    break;
                }
            }
            if (!handled) {
                out.line("// TODO: unsupported opcode " + insn.getOpcode());
            }
        }
    }
}
