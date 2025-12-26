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

public final class JumpInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof JumpInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            JumpInsnNode jump = (JumpInsnNode) insn;
            String label = context.label(jump.label);
            switch (jump.getOpcode()) {
                case Opcodes.GOTO:
                    out.line("goto " + label + ";");
                    break;
                case Opcodes.IFEQ:
                    emitUnaryCompare(context, out, "==", "0", label);
                    break;
                case Opcodes.IFNE:
                    emitUnaryCompare(context, out, "!=", "0", label);
                    break;
                case Opcodes.IFLT:
                    emitUnaryCompare(context, out, "<", "0", label);
                    break;
                case Opcodes.IFGE:
                    emitUnaryCompare(context, out, ">=", "0", label);
                    break;
                case Opcodes.IFGT:
                    emitUnaryCompare(context, out, ">", "0", label);
                    break;
                case Opcodes.IFLE:
                    emitUnaryCompare(context, out, "<=", "0", label);
                    break;
                case Opcodes.IFNULL:
                    emitRefCompare(context, out, "==", "nullptr", label);
                    break;
                case Opcodes.IFNONNULL:
                    emitRefCompare(context, out, "!=", "nullptr", label);
                    break;
                case Opcodes.IF_ICMPEQ:
                    emitBinaryCompare(context, out, "==", label);
                    break;
                case Opcodes.IF_ICMPNE:
                    emitBinaryCompare(context, out, "!=", label);
                    break;
                case Opcodes.IF_ICMPLT:
                    emitBinaryCompare(context, out, "<", label);
                    break;
                case Opcodes.IF_ICMPGE:
                    emitBinaryCompare(context, out, ">=", label);
                    break;
                case Opcodes.IF_ICMPGT:
                    emitBinaryCompare(context, out, ">", label);
                    break;
                case Opcodes.IF_ICMPLE:
                    emitBinaryCompare(context, out, "<=", label);
                    break;
                case Opcodes.IF_ACMPEQ:
                    emitRefBinaryCompare(context, out, "==", label);
                    break;
                case Opcodes.IF_ACMPNE:
                    emitRefBinaryCompare(context, out, "!=", label);
                    break;
                default:
                    out.line("// TODO: unsupported jump opcode " + jump.getOpcode());
                    break;
            }
        }

        private void emitUnaryCompare(MethodContext context, CodeWriter out, String op, String rhs, String label) {
            String lhs = context.temp("cond");
            out.line("int64_t " + lhs + " = frame.stack.popI64();");
            out.line("if (" + lhs + " " + op + " " + rhs + ") goto " + label + ";");
        }

        private void emitBinaryCompare(MethodContext context, CodeWriter out, String op, String label) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int64_t " + rhs + " = frame.stack.popI64();");
            out.line("int64_t " + lhs + " = frame.stack.popI64();");
            out.line("if (" + lhs + " " + op + " " + rhs + ") goto " + label + ";");
        }

        private void emitRefCompare(MethodContext context, CodeWriter out, String op, String rhs, String label) {
            String lhs = context.temp("ref");
            String cond = context.temp("cond");
            out.line("jobject " + lhs + " = frame.stack.popRef();");
            out.line("bool " + cond + " = (" + lhs + " " + op + " " + rhs + ");");
            out.line("if (" + lhs + " != nullptr) env->DeleteLocalRef(" + lhs + ");");
            out.line("if (" + cond + ") goto " + label + ";");
        }

        private void emitRefBinaryCompare(MethodContext context, CodeWriter out, String op, String label) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            String cond = context.temp("cond");
            out.line("jobject " + rhs + " = frame.stack.popRef();");
            out.line("jobject " + lhs + " = frame.stack.popRef();");
            out.line("bool " + cond + " = (" + lhs + " " + op + " " + rhs + ");");
            out.line("if (" + lhs + " != nullptr) env->DeleteLocalRef(" + lhs + ");");
            out.line("if (" + rhs + " != nullptr) env->DeleteLocalRef(" + rhs + ");");
            out.line("if (" + cond + ") goto " + label + ";");
        }
    }
