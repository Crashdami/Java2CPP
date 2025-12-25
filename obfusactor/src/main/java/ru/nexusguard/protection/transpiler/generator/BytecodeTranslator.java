package ru.nexusguard.protection.transpiler.generator;

import ru.nexusguard.protection.transpiler.util.CppStringEscaper;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

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

    private interface InstructionHandler {
        boolean supports(AbstractInsnNode insn);

        void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out);
    }

    private static final class LabelHandler implements InstructionHandler {
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

    private static final class LineNumberHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof LineNumberNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            // Ignore line numbers.
        }
    }

    private static final class FrameHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof FrameNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            // Ignore stack map frames.
        }
    }

    private static final class IntInsnHandler implements InstructionHandler {
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

    private static final class VarInsnHandler implements InstructionHandler {
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

    private static final class InsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        private InsnHandler(TypeMapper typeMapper) {
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
                case Opcodes.ISUB:
                case Opcodes.LSUB:
                    emitBinaryOp(context, out, "-");
                    break;
                case Opcodes.IMUL:
                case Opcodes.LMUL:
                    emitBinaryOp(context, out, "*");
                    break;
                case Opcodes.IDIV:
                case Opcodes.LDIV:
                    emitBinaryOp(context, out, "/");
                    break;
                case Opcodes.IREM:
                case Opcodes.LREM:
                    emitBinaryOp(context, out, "%");
                    break;
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                    emitPrimitiveReturn(context, out);
                    break;
                case Opcodes.ARETURN:
                    emitReferenceReturn(context, out);
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

        private void emitPrimitiveReturn(MethodContext context, CodeWriter out) {
            Type returnType = context.returnType();
            String jniType = typeMapper.toJniType(returnType);
            String temp = context.temp("ret");
            out.line(jniType + " " + temp + " = static_cast<" + jniType + ">(frame.stack.popI64());");
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
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
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
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
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
            out.line("jfloat " + val + " = static_cast<jfloat>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("env->SetFloatArrayRegion(static_cast<jfloatArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }

        private void emitDoubleArrayStore(MethodContext context, CodeWriter out) {
            String val = context.temp("val");
            String idx = context.temp("idx");
            String arr = context.temp("arr");
            out.line("jdouble " + val + " = static_cast<jdouble>(frame.stack.popI64());");
            out.line("jint " + idx + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + arr + " = frame.stack.popRef();");
            out.line("env->SetDoubleArrayRegion(static_cast<jdoubleArray>(" + arr + "), " + idx + ", 1, &" + val + ");");
            out.line("if (" + arr + " != nullptr) env->DeleteLocalRef(" + arr + ");");
        }
    }

    private static final class LdcInsnHandler implements InstructionHandler {
        private LdcInsnHandler(TypeMapper typeMapper) {
        }

        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof LdcInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            LdcInsnNode ldc = (LdcInsnNode) insn;
            Object cst = ldc.cst;
            if (cst instanceof Integer value) {
                out.line("frame.stack.pushI64(" + value + ");");
            } else if (cst instanceof Long value) {
                out.line("frame.stack.pushI64(" + value + "L);");
            } else if (cst instanceof Float value) {
                out.line("frame.stack.pushI64(static_cast<int64_t>(" + value + "f));");
            } else if (cst instanceof Double value) {
                out.line("frame.stack.pushI64(static_cast<int64_t>(" + value + "));");
            } else if (cst instanceof String value) {
                String escaped = CppStringEscaper.escape(value);
                String tmp = context.temp("str");
                out.line("jstring " + tmp + " = env->NewStringUTF(\"" + escaped + "\");");
                out.line("frame.stack.pushRef(" + tmp + ");");
            } else if (cst instanceof Type) {
                out.line("// TODO: type constant not supported");
                out.line("frame.stack.pushRef(nullptr);");
            } else {
                out.line("// TODO: unsupported ldc constant");
            }
        }
    }

    private static final class IincInsnHandler implements InstructionHandler {
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

    private static final class JumpInsnHandler implements InstructionHandler {
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

    private static final class FieldInsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        private FieldInsnHandler(TypeMapper typeMapper) {
            this.typeMapper = typeMapper;
        }

        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof FieldInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            FieldInsnNode field = (FieldInsnNode) insn;
            Type fieldType = Type.getType(field.desc);

            if (field.getOpcode() != Opcodes.GETSTATIC && field.getOpcode() != Opcodes.PUTSTATIC) {
                out.line("// TODO: only static fields are supported");
                return;
            }

            String cls = context.temp("cls");
            String fid = context.temp("fid");
            out.line("jclass " + cls + " = env->FindClass(\"" + field.owner + "\");");
            out.line("jfieldID " + fid + " = env->GetStaticFieldID(" + cls + ", \"" + field.name + "\", \"" + field.desc + "\");");

            if (field.getOpcode() == Opcodes.GETSTATIC) {
                String val = context.temp("val");
                String getter = typeMapper.staticGetter(fieldType);
                out.line(typeMapper.toJniType(fieldType) + " " + val + " = env->" + getter + "(" + cls + ", " + fid + ");");
                if (typeMapper.isReference(fieldType)) {
                    out.line("frame.stack.pushRef(" + val + ");");
                } else {
                    out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
                }
                out.line("if (" + cls + " != nullptr) env->DeleteLocalRef(" + cls + ");");
            } else {
                String setter = typeMapper.staticSetter(fieldType);
                if (typeMapper.isReference(fieldType)) {
                    String val = context.temp("val");
                    out.line("jobject " + val + " = frame.stack.popRef();");
                    out.line("env->" + setter + "(" + cls + ", " + fid + ", " + val + ");");
                    out.line("if (" + val + " != nullptr) env->DeleteLocalRef(" + val + ");");
                } else {
                    String val = context.temp("val");
                    out.line("int64_t " + val + " = frame.stack.popI64();");
                    out.line("env->" + setter + "(" + cls + ", " + fid + ", static_cast<" + typeMapper.toJniType(fieldType) + ">(" + val + "));");
                }
                out.line("if (" + cls + " != nullptr) env->DeleteLocalRef(" + cls + ");");
            }
        }
    }

    private static final class InvokeDynamicInsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        private InvokeDynamicInsnHandler(TypeMapper typeMapper) {
            this.typeMapper = typeMapper;
        }

        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof InvokeDynamicInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            InvokeDynamicInsnNode node = (InvokeDynamicInsnNode) insn;
            if (!isStringConcat(node)) {
                emitFallback(node, context, out);
                return;
            }

            Type returnType = Type.getReturnType(node.desc);
            if (!typeMapper.isReference(returnType)) {
                emitFallback(node, context, out);
                return;
            }

            emitStringConcat(node, context, out);
        }

        private boolean isStringConcat(InvokeDynamicInsnNode node) {
            if (node.bsm == null) {
                return false;
            }
            if (!"java/lang/invoke/StringConcatFactory".equals(node.bsm.getOwner())) {
                return false;
            }
            String name = node.bsm.getName();
            return "makeConcatWithConstants".equals(name) || "makeConcat".equals(name);
        }

        private void emitFallback(InvokeDynamicInsnNode node, MethodContext context, CodeWriter out) {
            Type returnType = Type.getReturnType(node.desc);
            if (typeMapper.isReference(returnType)) {
                out.line("frame.stack.pushRef(nullptr);");
            } else {
                out.line("frame.stack.pushI64(0);");
            }
            out.line("// TODO: unsupported invokedynamic " + node.name + node.desc);
        }

        private void emitStringConcat(InvokeDynamicInsnNode node, MethodContext context, CodeWriter out) {
            Type[] argTypes = Type.getArgumentTypes(node.desc);
            String[] argNames = new String[argTypes.length];
            for (int i = argTypes.length - 1; i >= 0; i--) {
                Type argType = argTypes[i];
                String argName = context.temp("arg");
                argNames[i] = argName;
                String jniType = typeMapper.toJniType(argType);
                if (typeMapper.isReference(argType)) {
                    out.line(jniType + " " + argName + " = static_cast<" + jniType + ">(frame.stack.popRef());");
                } else {
                    out.line(jniType + " " + argName + " = static_cast<" + jniType + ">(frame.stack.popI64());");
                }
            }

            String sbClass = context.temp("sb_cls");
            String sbLocal = context.temp("sb_local");
            out.line("static jclass " + sbClass + " = nullptr;");
            out.line("if (" + sbClass + " == nullptr) {");
            out.line("jclass " + sbLocal + " = env->FindClass(\"java/lang/StringBuilder\");");
            out.line(sbClass + " = static_cast<jclass>(env->NewGlobalRef(" + sbLocal + "));");
            out.line("if (" + sbLocal + " != nullptr) env->DeleteLocalRef(" + sbLocal + ");");
            out.line("}");

            String sbCtor = context.temp("sb_ctor");
            out.line("static jmethodID " + sbCtor + " = nullptr;");
            out.line("if (" + sbCtor + " == nullptr) {");
            out.line(sbCtor + " = env->GetMethodID(" + sbClass + ", \"<init>\", \"()V\");");
            out.line("}");

            String sbAppend = context.temp("sb_append");
            out.line("static jmethodID " + sbAppend + " = nullptr;");
            out.line("if (" + sbAppend + " == nullptr) {");
            out.line(sbAppend + " = env->GetMethodID(" + sbClass + ", \"append\", \"(Ljava/lang/String;)Ljava/lang/StringBuilder;\");");
            out.line("}");

            String sbToString = context.temp("sb_toString");
            out.line("static jmethodID " + sbToString + " = nullptr;");
            out.line("if (" + sbToString + " == nullptr) {");
            out.line(sbToString + " = env->GetMethodID(" + sbClass + ", \"toString\", \"()Ljava/lang/String;\");");
            out.line("}");

            String sb = context.temp("sb");
            out.line("jobject " + sb + " = env->NewObject(" + sbClass + ", " + sbCtor + ");");

            String strClass = context.temp("str_cls");
            String strLocal = context.temp("str_local");
            out.line("static jclass " + strClass + " = nullptr;");
            out.line("if (" + strClass + " == nullptr) {");
            out.line("jclass " + strLocal + " = env->FindClass(\"java/lang/String\");");
            out.line(strClass + " = static_cast<jclass>(env->NewGlobalRef(" + strLocal + "));");
            out.line("if (" + strLocal + " != nullptr) env->DeleteLocalRef(" + strLocal + ");");
            out.line("}");

            String recipe = null;
            if (node.bsmArgs != null && node.bsmArgs.length > 0 && node.bsmArgs[0] instanceof String value) {
                recipe = value;
            }

            if ("makeConcat".equals(node.bsm.getName()) || recipe == null) {
                for (int i = 0; i < argTypes.length; i++) {
                    emitAppendArgument(context, out, sb, sbAppend, strClass, argTypes[i], argNames[i]);
                }
            } else {
                String[] parts = recipe.split("\u0001", -1);
                int argIndex = 0;
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (!part.isEmpty()) {
                        emitAppendConstant(context, out, sb, sbAppend, part);
                    }
                    if (i < parts.length - 1 && argIndex < argNames.length) {
                        emitAppendArgument(context, out, sb, sbAppend, strClass, argTypes[argIndex], argNames[argIndex]);
                        argIndex++;
                    }
                }
                while (argIndex < argNames.length) {
                    emitAppendArgument(context, out, sb, sbAppend, strClass, argTypes[argIndex], argNames[argIndex]);
                    argIndex++;
                }
            }

            String result = context.temp("concat");
            out.line("jstring " + result + " = static_cast<jstring>(env->CallObjectMethod(" + sb + ", " + sbToString + "));");
            out.line("frame.stack.pushRef(" + result + ");");
            out.line("if (" + sb + " != nullptr) env->DeleteLocalRef(" + sb + ");");
        }

        private void emitAppendConstant(MethodContext context, CodeWriter out, String sb, String sbAppend, String value) {
            String escaped = CppStringEscaper.escape(value);
            String str = context.temp("cstr");
            out.line("jstring " + str + " = env->NewStringUTF(\"" + escaped + "\");");
            emitAppendString(context, out, sb, sbAppend, str);
            out.line("if (" + str + " != nullptr) env->DeleteLocalRef(" + str + ");");
        }

        private void emitAppendArgument(MethodContext context, CodeWriter out, String sb, String sbAppend, String strClass, Type argType, String argName) {
            String str = emitValueOf(context, out, strClass, argType, argName);
            emitAppendString(context, out, sb, sbAppend, str);
            out.line("if (" + str + " != nullptr) env->DeleteLocalRef(" + str + ");");
            if (typeMapper.isReference(argType)) {
                out.line("if (" + argName + " != nullptr) env->DeleteLocalRef(" + argName + ");");
            }
        }

        private void emitAppendString(MethodContext context, CodeWriter out, String sb, String sbAppend, String str) {
            String ret = context.temp("sb_ret");
            out.line("jobject " + ret + " = env->CallObjectMethod(" + sb + ", " + sbAppend + ", " + str + ");");
            out.line("if (" + ret + " != nullptr) env->DeleteLocalRef(" + ret + ");");
        }

        private String emitValueOf(MethodContext context, CodeWriter out, String strClass, Type argType, String argName) {
            String sig = valueOfSignature(argType);
            String mid = context.temp("valueOf");
            out.line("static jmethodID " + mid + " = nullptr;");
            out.line("if (" + mid + " == nullptr) {");
            out.line(mid + " = env->GetStaticMethodID(" + strClass + ", \"valueOf\", \"" + sig + "\");");
            out.line("}");
            String argExpr = valueOfArgument(argType, argName);
            String str = context.temp("str");
            out.line("jstring " + str + " = static_cast<jstring>(env->CallStaticObjectMethod(" + strClass + ", " + mid + ", " + argExpr + "));");
            return str;
        }

        private String valueOfSignature(Type type) {
            switch (type.getSort()) {
                case Type.BOOLEAN:
                    return "(Z)Ljava/lang/String;";
                case Type.CHAR:
                    return "(C)Ljava/lang/String;";
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    return "(I)Ljava/lang/String;";
                case Type.LONG:
                    return "(J)Ljava/lang/String;";
                case Type.FLOAT:
                    return "(F)Ljava/lang/String;";
                case Type.DOUBLE:
                    return "(D)Ljava/lang/String;";
                case Type.ARRAY:
                case Type.OBJECT:
                default:
                    return "(Ljava/lang/Object;)Ljava/lang/String;";
            }
        }

        private String valueOfArgument(Type type, String argName) {
            switch (type.getSort()) {
                case Type.BYTE:
                case Type.SHORT:
                    return "static_cast<jint>(" + argName + ")";
                case Type.ARRAY:
                case Type.OBJECT:
                    return "static_cast<jobject>(" + argName + ")";
                default:
                    return argName;
            }
        }
    }

    private static final class MethodInsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        private MethodInsnHandler(TypeMapper typeMapper) {
            this.typeMapper = typeMapper;
        }

        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof MethodInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            MethodInsnNode node = (MethodInsnNode) insn;
            int opcode = node.getOpcode();
            boolean isStatic = opcode == Opcodes.INVOKESTATIC;
            boolean isSpecial = opcode == Opcodes.INVOKESPECIAL;

            Type[] argTypes = Type.getArgumentTypes(node.desc);
            Type returnType = Type.getReturnType(node.desc);

            String[] argNames = new String[argTypes.length];
            for (int i = argTypes.length - 1; i >= 0; i--) {
                Type argType = argTypes[i];
                String argName = context.temp("arg");
                argNames[i] = argName;
                String jniType = typeMapper.toJniType(argType);
                if (typeMapper.isReference(argType)) {
                    out.line(jniType + " " + argName + " = static_cast<" + jniType + ">(frame.stack.popRef());");
                } else {
                    out.line(jniType + " " + argName + " = static_cast<" + jniType + ">(frame.stack.popI64());");
                }
            }

            String target = null;
            if (!isStatic) {
                target = context.temp("obj");
                out.line("jobject " + target + " = frame.stack.popRef();");
            }

            String cls = context.temp("cls_cache");
            String clsLocal = context.temp("cls_local");
            out.line("static jclass " + cls + " = nullptr;");
            out.line("if (" + cls + " == nullptr) {");
            out.line("jclass " + clsLocal + " = env->FindClass(\"" + node.owner + "\");");
            out.line(cls + " = static_cast<jclass>(env->NewGlobalRef(" + clsLocal + "));");
            out.line("if (" + clsLocal + " != nullptr) env->DeleteLocalRef(" + clsLocal + ");");
            out.line("}");

            String mid = context.temp("mid_cache");
            out.line("static jmethodID " + mid + " = nullptr;");
            if (isStatic) {
                out.line("if (" + mid + " == nullptr) {");
                out.line(mid + " = env->GetStaticMethodID(" + cls + ", \"" + node.name + "\", \"" + node.desc + "\");");
                out.line("}");
            } else {
                out.line("if (" + mid + " == nullptr) {");
                out.line(mid + " = env->GetMethodID(" + cls + ", \"" + node.name + "\", \"" + node.desc + "\");");
                out.line("}");
            }

            String callName;
            if (isStatic) {
                callName = typeMapper.callMethod(returnType, true);
            } else if (isSpecial) {
                callName = typeMapper.callNonvirtualMethod(returnType);
            } else {
                callName = typeMapper.callMethod(returnType, false);
            }

            StringBuilder call = new StringBuilder();
            if (isStatic) {
                call.append("env->").append(callName).append("(").append(cls).append(", ").append(mid);
            } else if (isSpecial) {
                call.append("env->").append(callName).append("(").append(target).append(", ").append(cls).append(", ").append(mid);
            } else {
                call.append("env->").append(callName).append("(").append(target).append(", ").append(mid);
            }
            for (String argName : argNames) {
                call.append(", ").append(argName);
            }
            call.append(")");

            if (returnType.getSort() == Type.VOID) {
                out.line(call + ";");
            } else {
                String jniType = typeMapper.toJniType(returnType);
                String ret = context.temp("ret");
                out.line(jniType + " " + ret + " = static_cast<" + jniType + ">(" + call + ");");
                if (typeMapper.isReference(returnType)) {
                    out.line("frame.stack.pushRef(" + ret + ");");
                } else {
                    out.line("frame.stack.pushI64(static_cast<int64_t>(" + ret + "));");
                }
            }

            for (int i = 0; i < argTypes.length; i++) {
                if (typeMapper.isReference(argTypes[i])) {
                    out.line("if (" + argNames[i] + " != nullptr) env->DeleteLocalRef(" + argNames[i] + ");");
                }
            }
            if (!isStatic) {
                out.line("if (" + target + " != nullptr) env->DeleteLocalRef(" + target + ");");
            }
        }
    }

    private static final class TypeInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof TypeInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            TypeInsnNode node = (TypeInsnNode) insn;
            switch (node.getOpcode()) {
                case Opcodes.NEW:
                    String cls = context.temp("new_cls");
                    String clsLocal = context.temp("new_cls_local");
                    out.line("static jclass " + cls + " = nullptr;");
                    out.line("if (" + cls + " == nullptr) {");
                    out.line("jclass " + clsLocal + " = env->FindClass(\"" + node.desc + "\");");
                    out.line(cls + " = static_cast<jclass>(env->NewGlobalRef(" + clsLocal + "));");
                    out.line("if (" + clsLocal + " != nullptr) env->DeleteLocalRef(" + clsLocal + ");");
                    out.line("}");
                    String obj = context.temp("obj");
                    out.line("jobject " + obj + " = env->AllocObject(" + cls + ");");
                    out.line("frame.stack.pushRef(" + obj + ");");
                    break;
                case Opcodes.ANEWARRAY:
                    String len = context.temp("arr_len");
                    out.line("jint " + len + " = static_cast<jint>(frame.stack.popI64());");
                    String arrCls = context.temp("arr_cls");
                    String arrClsLocal = context.temp("arr_cls_local");
                    out.line("static jclass " + arrCls + " = nullptr;");
                    out.line("if (" + arrCls + " == nullptr) {");
                    out.line("jclass " + arrClsLocal + " = env->FindClass(\"" + node.desc + "\");");
                    out.line(arrCls + " = static_cast<jclass>(env->NewGlobalRef(" + arrClsLocal + "));");
                    out.line("if (" + arrClsLocal + " != nullptr) env->DeleteLocalRef(" + arrClsLocal + ");");
                    out.line("}");
                    String arr = context.temp("arr");
                    out.line("jobjectArray " + arr + " = env->NewObjectArray(" + len + ", " + arrCls + ", nullptr);");
                    out.line("frame.stack.pushRef(" + arr + ");");
                    break;
                case Opcodes.CHECKCAST:
                    String castObj = context.temp("cc_obj");
                    out.line("jobject " + castObj + " = frame.stack.popRef();");
                    out.line("if (" + castObj + " == nullptr) {");
                    out.indent();
                    out.line("frame.stack.pushRef(nullptr);");
                    out.outdent();
                    out.line("} else {");
                    out.indent();
                    String castCls = context.temp("cc_cls");
                    String castClsLocal = context.temp("cc_cls_local");
                    out.line("static jclass " + castCls + " = nullptr;");
                    out.line("if (" + castCls + " == nullptr) {");
                    out.indent();
                    out.line("jclass " + castClsLocal + " = env->FindClass(\"" + node.desc + "\");");
                    out.line(castCls + " = static_cast<jclass>(env->NewGlobalRef(" + castClsLocal + "));");
                    out.line("if (" + castClsLocal + " != nullptr) env->DeleteLocalRef(" + castClsLocal + ");");
                    out.outdent();
                    out.line("}");
                    String castOk = context.temp("cc_ok");
                    out.line("bool " + castOk + " = env->IsInstanceOf(" + castObj + ", " + castCls + ") == JNI_TRUE;");
                    out.line("if (!" + castOk + ") {");
                    out.indent();
                    String exCls = context.temp("cc_ex_cls");
                    String exClsLocal = context.temp("cc_ex_local");
                    out.line("static jclass " + exCls + " = nullptr;");
                    out.line("if (" + exCls + " == nullptr) {");
                    out.indent();
                    out.line("jclass " + exClsLocal + " = env->FindClass(\"java/lang/ClassCastException\");");
                    out.line(exCls + " = static_cast<jclass>(env->NewGlobalRef(" + exClsLocal + "));");
                    out.line("if (" + exClsLocal + " != nullptr) env->DeleteLocalRef(" + exClsLocal + ");");
                    out.outdent();
                    out.line("}");
                    out.line("env->ThrowNew(" + exCls + ", \"Class cast failed\");");
                    out.line("if (" + castObj + " != nullptr) env->DeleteLocalRef(" + castObj + ");");
                    out.line("frame.stack.pushRef(nullptr);");
                    out.outdent();
                    out.line("} else {");
                    out.indent();
                    out.line("frame.stack.pushRef(" + castObj + ");");
                    out.outdent();
                    out.line("}");
                    out.outdent();
                    out.line("}");
                    break;
                case Opcodes.INSTANCEOF:
                    String instObj = context.temp("inst_obj");
                    out.line("jobject " + instObj + " = frame.stack.popRef();");
                    out.line("if (" + instObj + " == nullptr) {");
                    out.indent();
                    out.line("frame.stack.pushI64(0);");
                    out.outdent();
                    out.line("} else {");
                    out.indent();
                    String instCls = context.temp("inst_cls");
                    String instClsLocal = context.temp("inst_cls_local");
                    out.line("static jclass " + instCls + " = nullptr;");
                    out.line("if (" + instCls + " == nullptr) {");
                    out.indent();
                    out.line("jclass " + instClsLocal + " = env->FindClass(\"" + node.desc + "\");");
                    out.line(instCls + " = static_cast<jclass>(env->NewGlobalRef(" + instClsLocal + "));");
                    out.line("if (" + instClsLocal + " != nullptr) env->DeleteLocalRef(" + instClsLocal + ");");
                    out.outdent();
                    out.line("}");
                    String instOk = context.temp("inst_ok");
                    out.line("bool " + instOk + " = env->IsInstanceOf(" + instObj + ", " + instCls + ") == JNI_TRUE;");
                    out.line("frame.stack.pushI64(" + instOk + " ? 1 : 0);");
                    out.line("if (" + instObj + " != nullptr) env->DeleteLocalRef(" + instObj + ");");
                    out.outdent();
                    out.line("}");
                    break;
                default:
                    out.line("// TODO: type insn " + node.getOpcode() + " " + node.desc);
                    break;
            }
        }
    }
}
