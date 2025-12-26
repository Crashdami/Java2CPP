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

public final class InsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        public InsnHandler(TypeMapper typeMapper) {
            this.typeMapper = typeMapper;
        }

        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof InsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            switch (insn.getOpcode()) {
                case Opcodes.NOP:
                    break;
                case Opcodes.ICONST_M1:
                    out.line("frame.stack.pushI64(-1);");
                    break;
                case Opcodes.ICONST_0:
                    out.line("frame.stack.pushI64(0);");
                    break;
                case Opcodes.ICONST_1:
                    out.line("frame.stack.pushI64(1);");
                    break;
                case Opcodes.ICONST_2:
                    out.line("frame.stack.pushI64(2);");
                    break;
                case Opcodes.ICONST_3:
                    out.line("frame.stack.pushI64(3);");
                    break;
                case Opcodes.ICONST_4:
                    out.line("frame.stack.pushI64(4);");
                    break;
                case Opcodes.ICONST_5:
                    out.line("frame.stack.pushI64(5);");
                    break;
                case Opcodes.LCONST_0:
                    out.line("frame.stack.pushI64(0);");
                    break;
                case Opcodes.LCONST_1:
                    out.line("frame.stack.pushI64(1);");
                    break;
                case Opcodes.FCONST_0:
                    out.line("frame.stack.pushI64(ng::runtime::packFloat(0.0f));");
                    break;
                case Opcodes.FCONST_1:
                    out.line("frame.stack.pushI64(ng::runtime::packFloat(1.0f));");
                    break;
                case Opcodes.FCONST_2:
                    out.line("frame.stack.pushI64(ng::runtime::packFloat(2.0f));");
                    break;
                case Opcodes.DCONST_0:
                    out.line("frame.stack.pushI64(ng::runtime::packDouble(0.0));");
                    break;
                case Opcodes.DCONST_1:
                    out.line("frame.stack.pushI64(ng::runtime::packDouble(1.0));");
                    break;
                case Opcodes.ACONST_NULL:
                    out.line("frame.stack.pushRef(nullptr);");
                    break;
                case Opcodes.DUP:
                    out.line("frame.stack.dup(env);");
                    break;
                case Opcodes.POP:
                    out.line("frame.stack.popDiscard(env);");
                    break;
                case Opcodes.IADD:
                case Opcodes.LADD:
                    emitBinaryOp(context, out, "+");
                    break;
                case Opcodes.FADD:
                    emitBinaryOpFloat(context, out, "+");
                    break;
                case Opcodes.DADD:
                    emitBinaryOpDouble(context, out, "+");
                    break;
                case Opcodes.ISUB:
                case Opcodes.LSUB:
                    emitBinaryOp(context, out, "-");
                    break;
                case Opcodes.FSUB:
                    emitBinaryOpFloat(context, out, "-");
                    break;
                case Opcodes.DSUB:
                    emitBinaryOpDouble(context, out, "-");
                    break;
                case Opcodes.IMUL:
                case Opcodes.LMUL:
                    emitBinaryOp(context, out, "*");
                    break;
                case Opcodes.FMUL:
                    emitBinaryOpFloat(context, out, "*");
                    break;
                case Opcodes.DMUL:
                    emitBinaryOpDouble(context, out, "*");
                    break;
                case Opcodes.IDIV:
                case Opcodes.LDIV:
                    emitBinaryOp(context, out, "/");
                    break;
                case Opcodes.FDIV:
                    emitBinaryOpFloat(context, out, "/");
                    break;
                case Opcodes.DDIV:
                    emitBinaryOpDouble(context, out, "/");
                    break;
                case Opcodes.IREM:
                case Opcodes.LREM:
                    emitBinaryOp(context, out, "%");
                    break;
                case Opcodes.FREM:
                    emitBinaryOpFloat(context, out, "%");
                    break;
                case Opcodes.DREM:
                    emitBinaryOpDouble(context, out, "%");
                    break;
                case Opcodes.IAND:
                    emitBitwiseInt(context, out, "&");
                    break;
                case Opcodes.IOR:
                    emitBitwiseInt(context, out, "|");
                    break;
                case Opcodes.IXOR:
                    emitBitwiseInt(context, out, "^");
                    break;
                case Opcodes.LAND:
                    emitBitwiseLong(context, out, "&");
                    break;
                case Opcodes.LOR:
                    emitBitwiseLong(context, out, "|");
                    break;
                case Opcodes.LXOR:
                    emitBitwiseLong(context, out, "^");
                    break;
                case Opcodes.ISHL:
                    emitShiftInt(context, out, "<<");
                    break;
                case Opcodes.ISHR:
                    emitShiftInt(context, out, ">>");
                    break;
                case Opcodes.IUSHR:
                    emitShiftIntUnsigned(context, out);
                    break;
                case Opcodes.LSHL:
                    emitShiftLong(context, out, "<<");
                    break;
                case Opcodes.LSHR:
                    emitShiftLong(context, out, ">>");
                    break;
                case Opcodes.LUSHR:
                    emitShiftLongUnsigned(context, out);
                    break;
                case Opcodes.INEG:
                    emitNegInt(context, out);
                    break;
                case Opcodes.LNEG:
                    emitNegLong(context, out);
                    break;
                case Opcodes.FNEG:
                    emitNegFloat(context, out);
                    break;
                case Opcodes.DNEG:
                    emitNegDouble(context, out);
                    break;
                case Opcodes.I2L:
                    emitI2L(context, out);
                    break;
                case Opcodes.I2F:
                    emitI2F(context, out);
                    break;
                case Opcodes.I2D:
                    emitI2D(context, out);
                    break;
                case Opcodes.L2I:
                    emitL2I(context, out);
                    break;
                case Opcodes.L2F:
                    emitL2F(context, out);
                    break;
                case Opcodes.L2D:
                    emitL2D(context, out);
                    break;
                case Opcodes.I2B:
                    emitI2B(context, out);
                    break;
                case Opcodes.I2C:
                    emitI2C(context, out);
                    break;
                case Opcodes.I2S:
                    emitI2S(context, out);
                    break;
                case Opcodes.F2I:
                    emitF2I(context, out);
                    break;
                case Opcodes.F2L:
                    emitF2L(context, out);
                    break;
                case Opcodes.F2D:
                    emitF2D(context, out);
                    break;
                case Opcodes.D2I:
                    emitD2I(context, out);
                    break;
                case Opcodes.D2L:
                    emitD2L(context, out);
                    break;
                case Opcodes.D2F:
                    emitD2F(context, out);
                    break;
                case Opcodes.LCMP:
                    emitLongCompare(context, out);
                    break;
                case Opcodes.FCMPL:
                    emitFloatCompare(context, out, false);
                    break;
                case Opcodes.FCMPG:
                    emitFloatCompare(context, out, true);
                    break;
                case Opcodes.DCMPL:
                    emitDoubleCompare(context, out, false);
                    break;
                case Opcodes.DCMPG:
                    emitDoubleCompare(context, out, true);
                    break;
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                    emitPrimitiveReturn(context, out);
                    break;
                case Opcodes.ARETURN:
                    emitReferenceReturn(context, out);
                    break;
                case Opcodes.ATHROW:
                    emitAthrow(context, out);
                    break;
                case Opcodes.BALOAD:
                    emitByteArrayLoad(context, out);
                    break;
                case Opcodes.CALOAD:
                    emitCharArrayLoad(context, out);
                    break;
                case Opcodes.SALOAD:
                    emitShortArrayLoad(context, out);
                    break;
                case Opcodes.IALOAD:
                    emitIntArrayLoad(context, out);
                    break;
                case Opcodes.LALOAD:
                    emitLongArrayLoad(context, out);
                    break;
                case Opcodes.FALOAD:
                    emitFloatArrayLoad(context, out);
                    break;
                case Opcodes.DALOAD:
                    emitDoubleArrayLoad(context, out);
                    break;
                case Opcodes.AALOAD:
                    String idx = context.temp("idx");
                    String arr = context.temp("arr");
                    String elem = context.temp("elem");
                    out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
                    out.line("jobject " + arr + " = frame.stack.popRef();");
                    out.line("jobject " + elem + " = env->GetObjectArrayElement(static_cast<jobjectArray>(" + arr + "), " + idx + ");");
                    out.line("frame.stack.pushRef(" + elem + ");");
                    out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
                    break;
                case Opcodes.BASTORE:
                    emitByteArrayStore(context, out);
                    break;
                case Opcodes.CASTORE:
                    emitCharArrayStore(context, out);
                    break;
                case Opcodes.SASTORE:
                    emitShortArrayStore(context, out);
                    break;
                case Opcodes.IASTORE:
                    emitIntArrayStore(context, out);
                    break;
                case Opcodes.LASTORE:
                    emitLongArrayStore(context, out);
                    break;
                case Opcodes.FASTORE:
                    emitFloatArrayStore(context, out);
                    break;
                case Opcodes.DASTORE:
                    emitDoubleArrayStore(context, out);
                    break;
                case Opcodes.AASTORE:
                    String val = context.temp("val");
                    String idx2 = context.temp("idx");
                    String arr2 = context.temp("arr");
                    out.line("jobject " + val + " = frame.stack.popRef();");
                    out.line("jint " + idx2 + " = static_cast<jint>(frame.stack.popI64());");
                    out.line("jobject " + arr2 + " = frame.stack.popRef();");
                    out.line("env->SetObjectArrayElement(static_cast<jobjectArray>(" + arr2 + "), " + idx2 + ", " + val + ");");
                    out.line("if (" + val + " != nullptr) env->DeleteLocalRef(" + val + ");");
                    out.line("if (" + arr2 + " != nullptr) env->DeleteLocalRef(" + arr2 + ");");
                    break;
                case Opcodes.ARRAYLENGTH:
                    String arrLen = context.temp("arr");
                    String len = context.temp("len");
                    out.line("jobject " + arrLen + " = frame.stack.popRef();");
                    out.line("jsize " + len + " = env->GetArrayLength(static_cast<jarray>(" + arrLen + "));");
                    out.line("frame.stack.pushI64(static_cast<int64_t>(" + len + "));");
                    out.line("if (" + arrLen + " != nullptr) env->DeleteLocalRef(" + arrLen + ");");
                    break;
                case Opcodes.MONITORENTER:
                    String monEnter = context.temp("mon_enter");
                    out.line("jobject " + monEnter + " = frame.stack.popRef();");
                    out.line("env->MonitorEnter(" + monEnter + ");");
                    out.line("if (" + monEnter + " != nullptr) env->DeleteLocalRef(" + monEnter + ");");
                    InstructionUtils.emitExceptionReturn(context, out);
                    break;
                case Opcodes.MONITOREXIT:
                    String monExit = context.temp("mon_exit");
                    out.line("jobject " + monExit + " = frame.stack.popRef();");
                    out.line("env->MonitorExit(" + monExit + ");");
                    out.line("if (" + monExit + " != nullptr) env->DeleteLocalRef(" + monExit + ");");
                    InstructionUtils.emitExceptionReturn(context, out);
                    break;
                case Opcodes.RETURN:
                    out.line("return;");
                    context.markReturn();
                    break;
                default:
                    out.line("// TODO: unsupported insn opcode " + insn.getOpcode());
                    break;
            }
        }

        private void emitBinaryOp(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int64_t " + rhs + " = frame.stack.popI64();");
            out.line("int64_t " + lhs + " = frame.stack.popI64();");
            out.line("frame.stack.pushI64(" + lhs + " " + op + " " + rhs + ");");
        }

        private void emitBinaryOpFloat(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("float " + rhs + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            out.line("float " + lhs + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packFloat(" + lhs + " " + op + " " + rhs + "));");
        }

        private void emitBinaryOpDouble(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("double " + rhs + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            out.line("double " + lhs + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packDouble(" + lhs + " " + op + " " + rhs + "));");
        }

        private void emitBitwiseInt(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int32_t " + rhs + " = static_cast<int32_t>(frame.stack.popI64());");
            out.line("int32_t " + lhs + " = static_cast<int32_t>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + lhs + " " + op + " " + rhs + "));");
        }

        private void emitBitwiseLong(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int64_t " + rhs + " = frame.stack.popI64();");
            out.line("int64_t " + lhs + " = frame.stack.popI64();");
            out.line("frame.stack.pushI64(" + lhs + " " + op + " " + rhs + ");");
        }

        private void emitShiftInt(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int32_t " + rhs + " = static_cast<int32_t>(frame.stack.popI64()) & 31;");
            out.line("int32_t " + lhs + " = static_cast<int32_t>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + lhs + " " + op + " " + rhs + "));");
        }

        private void emitShiftIntUnsigned(MethodContext context, CodeWriter out) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            String res = context.temp("res");
            out.line("int32_t " + rhs + " = static_cast<int32_t>(frame.stack.popI64()) & 31;");
            out.line("uint32_t " + lhs + " = static_cast<uint32_t>(static_cast<int32_t>(frame.stack.popI64()));");
            out.line("uint32_t " + res + " = " + lhs + " >> " + rhs + ";");
            out.line("frame.stack.pushI64(static_cast<int64_t>(static_cast<int32_t>(" + res + ")));");
        }

        private void emitShiftLong(MethodContext context, CodeWriter out, String op) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int32_t " + rhs + " = static_cast<int32_t>(frame.stack.popI64()) & 63;");
            out.line("int64_t " + lhs + " = frame.stack.popI64();");
            out.line("frame.stack.pushI64(" + lhs + " " + op + " " + rhs + ");");
        }

        private void emitShiftLongUnsigned(MethodContext context, CodeWriter out) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            out.line("int32_t " + rhs + " = static_cast<int32_t>(frame.stack.popI64()) & 63;");
            out.line("uint64_t " + lhs + " = static_cast<uint64_t>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + lhs + " >> " + rhs + "));");
        }

        private void emitNegInt(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int32_t " + val + " = static_cast<int32_t>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(-" + val + "));");
        }

        private void emitNegLong(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int64_t " + val + " = frame.stack.popI64();");
            out.line("frame.stack.pushI64(-" + val + ");");
        }

        private void emitNegFloat(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("float " + val + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packFloat(-" + val + "));");
        }

        private void emitNegDouble(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("double " + val + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packDouble(-" + val + "));");
        }

        private void emitI2F(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("float " + val + " = static_cast<float>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packFloat(" + val + "));");
        }

        private void emitI2L(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int32_t " + val + " = static_cast<int32_t>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitI2D(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("double " + val + " = static_cast<double>(static_cast<int32_t>(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(ng::runtime::packDouble(" + val + "));");
        }

        private void emitL2I(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int32_t " + val + " = static_cast<int32_t>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitL2F(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("float " + val + " = static_cast<float>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packFloat(" + val + "));");
        }

        private void emitL2D(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("double " + val + " = static_cast<double>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(ng::runtime::packDouble(" + val + "));");
        }

        private void emitF2I(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int32_t " + val + " = static_cast<int32_t>(ng::runtime::unpackFloat(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitF2L(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int64_t " + val + " = static_cast<int64_t>(ng::runtime::unpackFloat(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(" + val + ");");
        }

        private void emitF2D(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("double " + val + " = static_cast<double>(ng::runtime::unpackFloat(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(ng::runtime::packDouble(" + val + "));");
        }

        private void emitD2I(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int32_t " + val + " = static_cast<int32_t>(ng::runtime::unpackDouble(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitD2L(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("int64_t " + val + " = static_cast<int64_t>(ng::runtime::unpackDouble(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(" + val + ");");
        }

        private void emitD2F(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("float " + val + " = static_cast<float>(ng::runtime::unpackDouble(frame.stack.popI64()));");
            out.line("frame.stack.pushI64(ng::runtime::packFloat(" + val + "));");
        }

        private void emitI2B(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("jbyte " + val + " = static_cast<jbyte>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitI2C(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("jchar " + val + " = static_cast<jchar>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitI2S(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            out.line("jshort " + val + " = static_cast<jshort>(frame.stack.popI64());");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
        }

        private void emitFloatCompare(MethodContext context, CodeWriter out, boolean nanHigh) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            String cmp = context.temp("cmp");
            out.line("float " + rhs + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            out.line("float " + lhs + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            out.line("int " + cmp + " = 0;");
            out.line("if ((" + lhs + " != " + lhs + ") || (" + rhs + " != " + rhs + ")) {");
            out.indent();
            out.line(cmp + " = " + (nanHigh ? 1 : -1) + ";");
            out.outdent();
            out.line("} else if (" + lhs + " > " + rhs + ") {");
            out.indent();
            out.line(cmp + " = 1;");
            out.outdent();
            out.line("} else if (" + lhs + " < " + rhs + ") {");
            out.indent();
            out.line(cmp + " = -1;");
            out.outdent();
            out.line("}");
            out.line("frame.stack.pushI64(" + cmp + ");");
        }

        private void emitLongCompare(MethodContext context, CodeWriter out) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            String cmp = context.temp("cmp");
            out.line("int64_t " + rhs + " = frame.stack.popI64();");
            out.line("int64_t " + lhs + " = frame.stack.popI64();");
            out.line("int " + cmp + " = 0;");
            out.line("if (" + lhs + " > " + rhs + ") {");
            out.indent();
            out.line(cmp + " = 1;");
            out.outdent();
            out.line("} else if (" + lhs + " < " + rhs + ") {");
            out.indent();
            out.line(cmp + " = -1;");
            out.outdent();
            out.line("}");
            out.line("frame.stack.pushI64(" + cmp + ");");
        }

        private void emitDoubleCompare(MethodContext context, CodeWriter out, boolean nanHigh) {
            String rhs = context.temp("rhs");
            String lhs = context.temp("lhs");
            String cmp = context.temp("cmp");
            out.line("double " + rhs + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            out.line("double " + lhs + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            out.line("int " + cmp + " = 0;");
            out.line("if ((" + lhs + " != " + lhs + ") || (" + rhs + " != " + rhs + ")) {");
            out.indent();
            out.line(cmp + " = " + (nanHigh ? 1 : -1) + ";");
            out.outdent();
            out.line("} else if (" + lhs + " > " + rhs + ") {");
            out.indent();
            out.line(cmp + " = 1;");
            out.outdent();
            out.line("} else if (" + lhs + " < " + rhs + ") {");
            out.indent();
            out.line(cmp + " = -1;");
            out.outdent();
            out.line("}");
            out.line("frame.stack.pushI64(" + cmp + ");");
        }

        private void emitPrimitiveReturn(MethodContext context, CodeWriter out) {
            Type returnType = context.returnType();
            String jniType = typeMapper.toJniType(returnType);
            String temp = context.temp("ret");
            if (returnType.getSort() == Type.FLOAT) {
                out.line(jniType + " " + temp + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            } else if (returnType.getSort() == Type.DOUBLE) {
                out.line(jniType + " " + temp + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            } else {
                out.line(jniType + " " + temp + " = static_cast<" + jniType + ">(frame.stack.popI64());");
            }
            out.line("return " + temp + ";");
            context.markReturn();
        }

        private void emitReferenceReturn(MethodContext context, CodeWriter out) {
            Type returnType = context.returnType();
            String jniType = typeMapper.toJniType(returnType);
            String temp = context.temp("ret");
            out.line(jniType + " " + temp + " = static_cast<" + jniType + ">(frame.stack.popRef());");
            out.line("return " + temp + ";");
            context.markReturn();
        }

        private void emitAthrow(MethodContext context, CodeWriter out) {
            String thr = context.temp("thr");
            out.line("jobject " + thr + " = frame.stack.popRef();");
            out.line("if (" + thr + " == nullptr) {");
            out.indent();
            String npeCls = context.temp("npe_cls");
            String npeLocal = context.temp("npe_local");
            out.line("static jclass " + npeCls + " = nullptr;");
            out.line("if (" + npeCls + " == nullptr) {");
            out.indent();
            out.line("jclass " + npeLocal + " = env->FindClass(\"java/lang/NullPointerException\");");
            out.line(npeCls + " = static_cast<jclass>(env->NewGlobalRef(" + npeLocal + "));");
            out.line("if (" + npeLocal + " != nullptr) env->DeleteLocalRef(" + npeLocal + ");");
            out.outdent();
            out.line("}");
            out.line("env->ThrowNew(" + npeCls + ", \"\");");
            out.outdent();
            out.line("} else {");
            out.indent();
            out.line("env->Throw(static_cast<jthrowable>(" + thr + "));");
            out.outdent();
            out.line("}");
            out.line("if (" + thr + " != nullptr) env->DeleteLocalRef(" + thr + ");");
            emitThrowReturn(context, out);
        }

        private void emitThrowReturn(MethodContext context, CodeWriter out) {
            Type returnType = context.returnType();
            if (returnType.getSort() == Type.VOID) {
                out.line("return;");
            } else if (typeMapper.isReference(returnType)) {
                out.line("return nullptr;");
            } else {
                out.line("return static_cast<" + typeMapper.toJniType(returnType) + ">(0);");
            }
            context.markReturn();
        }

        private void emitByteArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String boolCls = context.temp("bool_arr_cls");
            String boolLocal = context.temp("bool_arr_local");
            String isBool = context.temp("is_bool");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("static jclass " + boolCls + " = nullptr;");
            out.line("if (" + boolCls + " == nullptr) {");
            out.indent();
            out.line("jclass " + boolLocal + " = env->FindClass(\"[Z\");");
            out.line(boolCls + " = static_cast<jclass>(env->NewGlobalRef(" + boolLocal + "));");
            out.line("if (" + boolLocal + " != nullptr) env->DeleteLocalRef(" + boolLocal + ");");
            out.outdent();
            out.line("}");
            out.line("jboolean " + isBool + " = env->IsInstanceOf(" + arr + ", " + boolCls + ") ? JNI_TRUE : JNI_FALSE;");
            out.line("if (" + isBool + ") {");
            out.indent();
            String valBool = context.temp("val_bool");
            out.line("jboolean " + valBool + " = JNI_FALSE;");
            out.line("env->GetBooleanArrayRegion(static_cast<jbooleanArray>(" + arr + "), " + idx + ", 1, &" + valBool + ");");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + valBool + "));");
            out.outdent();
            out.line("} else {");
            out.indent();
            String valByte = context.temp("val_byte");
            out.line("jbyte " + valByte + " = 0;");
            out.line("env->GetByteArrayRegion(static_cast<jbyteArray>(" + arr + "), " + idx + ", 1, &" + valByte + ");");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + valByte + "));");
            out.outdent();
            out.line("}");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitCharArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String val = context.temp("val");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("jchar " + val + " = 0;");
            out.line("env->GetCharArrayRegion(static_cast<jcharArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitShortArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String val = context.temp("val");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("jshort " + val + " = 0;");
            out.line("env->GetShortArrayRegion(static_cast<jshortArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitIntArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String val = context.temp("val");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("jint " + val + " = 0;");
            out.line("env->GetIntArrayRegion(static_cast<jintArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitLongArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String val = context.temp("val");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("jlong " + val + " = 0;");
            out.line("env->GetLongArrayRegion(static_cast<jlongArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitFloatArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String val = context.temp("val");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("jfloat " + val + " = 0.0f;");
            out.line("env->GetFloatArrayRegion(static_cast<jfloatArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("frame.stack.pushI64(ng::runtime::packFloat(" + val + "));");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitDoubleArrayLoad(MethodContext context, CodeWriter out) {
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String val = context.temp("val");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("jdouble " + val + " = 0.0;");
            out.line("env->GetDoubleArrayRegion(static_cast<jdoubleArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("frame.stack.pushI64(ng::runtime::packDouble(" + val + "));");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitByteArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            String boolCls = context.temp("bool_arr_cls");
            String boolLocal = context.temp("bool_arr_local");
            String isBool = context.temp("is_bool");
            out.line("jint " + val + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("static jclass " + boolCls + " = nullptr;");
            out.line("if (" + boolCls + " == nullptr) {");
            out.indent();
            out.line("jclass " + boolLocal + " = env->FindClass(\"[Z\");");
            out.line(boolCls + " = static_cast<jclass>(env->NewGlobalRef(" + boolLocal + "));");
            out.line("if (" + boolLocal + " != nullptr) env->DeleteLocalRef(" + boolLocal + ");");
            out.outdent();
            out.line("}");
            out.line("jboolean " + isBool + " = env->IsInstanceOf(" + arr + ", " + boolCls + ") ? JNI_TRUE : JNI_FALSE;");
            out.line("if (" + isBool + ") {");
            out.indent();
            String boolVal = context.temp("bool_val");
            out.line("jboolean " + boolVal + " = static_cast<jboolean>(" + val + " != 0);");
            out.line("env->SetBooleanArrayRegion(static_cast<jbooleanArray>(" + arr + "), " + idx + ", 1, &" + boolVal + ");");
            out.outdent();
            out.line("} else {");
            out.indent();
            String byteVal = context.temp("byte_val");
            out.line("jbyte " + byteVal + " = static_cast<jbyte>(" + val + ");");
            out.line("env->SetByteArrayRegion(static_cast<jbyteArray>(" + arr + "), " + idx + ", 1, &" + byteVal + ");");
            out.outdent();
            out.line("}");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitCharArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jint " + val + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            String tmp = context.temp("char_val");
            out.line("jchar " + tmp + " = static_cast<jchar>(" + val + ");");
            out.line("env->SetCharArrayRegion(static_cast<jcharArray>(" + arr + "), " + idx + ", 1, &" + tmp + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitShortArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jint " + val + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            String tmp = context.temp("short_val");
            out.line("jshort " + tmp + " = static_cast<jshort>(" + val + ");");
            out.line("env->SetShortArrayRegion(static_cast<jshortArray>(" + arr + "), " + idx + ", 1, &" + tmp + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitIntArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jint " + val + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("env->SetIntArrayRegion(static_cast<jintArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitLongArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jlong " + val + " = static_cast<jlong>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("env->SetLongArrayRegion(static_cast<jlongArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitFloatArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jfloat " + val + " = ng::runtime::unpackFloat(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("env->SetFloatArrayRegion(static_cast<jfloatArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitDoubleArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jdouble " + val + " = ng::runtime::unpackDouble(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("env->SetDoubleArrayRegion(static_cast<jdoubleArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }
    }


