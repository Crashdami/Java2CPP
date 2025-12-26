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

public final class VarInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof VarInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            VarInsnNode var = (VarInsnNode) insn;
            switch (var.getOpcode()) {
                case Opcodes.ILOAD:
                case Opcodes.LLOAD:
                case Opcodes.FLOAD:
                case Opcodes.DLOAD:
                    out.line("frame.stack.pushI64(frame.locals.getI64(" + var.var + "));");
                    break;
                case Opcodes.ALOAD:
                    String ref = context.temp("ref");
                    out.line("jobject " + ref + " = frame.locals.getRef(" + var.var + ");");
                    out.line("if (" + ref + " != nullptr) " + ref + " = env->NewLocalRef(" + ref + ");");
                    out.line("frame.stack.pushRef(" + ref + ");");
                    break;
                case Opcodes.ISTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE:
                case Opcodes.DSTORE:
                    out.line("frame.locals.setI64(" + var.var + ", frame.stack.popI64());");
                    break;
                case Opcodes.ASTORE:
                    String newRef = context.temp("ref");
                    String oldRef = context.temp("old");
                    out.line("jobject " + newRef + " = frame.stack.popRef();");
                    out.line("jobject " + oldRef + " = frame.locals.getRef(" + var.var + ");");
                    out.line("if (" + oldRef + " != nullptr && " + oldRef + " != " + newRef + ") env->DeleteLocalRef(" + oldRef + ");");
                    out.line("frame.locals.setRef(" + var.var + ", " + newRef + ");");
                    break;
                default:
                    out.line("// TODO: unsupported var opcode " + var.getOpcode());
                    break;
            }
        }
    }
