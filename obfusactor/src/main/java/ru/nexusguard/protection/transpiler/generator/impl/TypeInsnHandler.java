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

public final class TypeInsnHandler implements InstructionHandler {
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
