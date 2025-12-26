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

public final class MultiANewArrayInsnHandler implements InstructionHandler {
        @Override
        public boolean supports(AbstractInsnNode insn) {
            return insn instanceof MultiANewArrayInsnNode;
        }

        @Override
        public void emit(AbstractInsnNode insn, MethodContext context, CodeWriter out) {
            MultiANewArrayInsnNode node = (MultiANewArrayInsnNode) insn;
            int dims = node.dims;
            String sizes = context.temp("ma_sizes");
            out.line("jint " + sizes + "[" + dims + "];");
            for (int i = dims - 1; i >= 0; i--) {
                out.line(sizes + "[" + i + "] = static_cast<jint>(frame.stack.popI64());");
            }
            String dimsArr = context.temp("ma_dims");
            out.line("jintArray " + dimsArr + " = env->NewIntArray(" + dims + ");");
            out.line("env->SetIntArrayRegion(" + dimsArr + ", 0, " + dims + ", " + sizes + ");");

            Type element = Type.getType(node.desc);
            while (element.getSort() == Type.ARRAY) {
                element = element.getElementType();
            }

            String compCls = context.temp("ma_comp");
            if (element.getSort() == Type.OBJECT) {
                String compLocal = context.temp("ma_comp_local");
                out.line("static jclass " + compCls + " = nullptr;");
                out.line("if (" + compCls + " == nullptr) {");
                out.indent();
                out.line("jclass " + compLocal + " = env->FindClass(\"" + CppStringEscaper.escape(element.getInternalName()) + "\");");
                out.line(compCls + " = static_cast<jclass>(env->NewGlobalRef(" + compLocal + "));");
                out.line("if (" + compLocal + " != nullptr) env->DeleteLocalRef(" + compLocal + ");");
                out.outdent();
                out.line("}");
            } else {
                char descriptor = element.getDescriptor().charAt(0);
                out.line("jclass " + compCls + " = ng::runtime::GetPrimitiveClass(env, '" + descriptor + "');");
            }

            String arrayCls = context.temp("ma_array_cls");
            String arrayLocal = context.temp("ma_array_local");
            out.line("static jclass " + arrayCls + " = nullptr;");
            out.line("if (" + arrayCls + " == nullptr) {");
            out.indent();
            out.line("jclass " + arrayLocal + " = env->FindClass(\"java/lang/reflect/Array\");");
            out.line(arrayCls + " = static_cast<jclass>(env->NewGlobalRef(" + arrayLocal + "));");
            out.line("if (" + arrayLocal + " != nullptr) env->DeleteLocalRef(" + arrayLocal + ");");
            out.outdent();
            out.line("}");
            String arrayMid = context.temp("ma_array_mid");
            out.line("static jmethodID " + arrayMid + " = nullptr;");
            out.line("if (" + arrayMid + " == nullptr) {");
            out.indent();
            out.line(arrayMid + " = env->GetStaticMethodID(" + arrayCls + ", \"newInstance\", \"(Ljava/lang/Class;[I)Ljava/lang/Object;\");");
            out.outdent();
            out.line("}");
            String result = context.temp("ma_obj");
            out.line("jobject " + result + " = env->CallStaticObjectMethod(" + arrayCls + ", " + arrayMid + ", " + compCls + ", " + dimsArr + ");");
            InstructionUtils.emitExceptionReturn(context, out);
            out.line("frame.stack.pushRef(" + result + ");");
            out.line("if (" + dimsArr + " != nullptr) env->DeleteLocalRef(" + dimsArr + ");");
        }
    }

