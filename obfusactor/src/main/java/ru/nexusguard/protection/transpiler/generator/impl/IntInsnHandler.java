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

public final class IntInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof IntInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            IntInsnNode node = (IntInsnNode) insn;
            switch (node.getOpcode()) {
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH:
                    out.line("frame.stack.pushI64(" + node.operand + ");");
                    break;
                case Opcodes.NEWARRAY:
                    String len = context.temp("len");
                    out.line("jint " + len + " = static_cast<jint>(frame.stack.popI64());");
                    String arr = context.temp("arr");
                    out.line("jarray " + arr + " = nullptr;");
                    switch (node.operand) {
                        case Opcodes.T_BOOLEAN:
                            out.line(arr + " = env->NewBooleanArray(" + len + ");");
                            break;
                        case Opcodes.T_CHAR:
                            out.line(arr + " = env->NewCharArray(" + len + ");");
                            break;
                        case Opcodes.T_FLOAT:
                            out.line(arr + " = env->NewFloatArray(" + len + ");");
                            break;
                        case Opcodes.T_DOUBLE:
                            out.line(arr + " = env->NewDoubleArray(" + len + ");");
                            break;
                        case Opcodes.T_BYTE:
                            out.line(arr + " = env->NewByteArray(" + len + ");");
                            break;
                        case Opcodes.T_SHORT:
                            out.line(arr + " = env->NewShortArray(" + len + ");");
                            break;
                        case Opcodes.T_INT:
                            out.line(arr + " = env->NewIntArray(" + len + ");");
                            break;
                        case Opcodes.T_LONG:
                            out.line(arr + " = env->NewLongArray(" + len + ");");
                            break;
                        default:
                            out.line("// TODO: unsupported newarray type " + node.operand);
                            break;
                    }
                    out.line("frame.stack.pushRef(" + arr + ");");
                    break;
                default:
                    out.line("// TODO: unsupported int insn opcode " + node.getOpcode());
                    break;
            }
        }
    }
