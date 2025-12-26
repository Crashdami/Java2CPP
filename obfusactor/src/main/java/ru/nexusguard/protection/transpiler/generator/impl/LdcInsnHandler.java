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

public final class LdcInsnHandler implements InstructionHandler {
        public LdcInsnHandler(TypeMapper typeMapper) {
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
                out.line("frame.stack.pushI64(ng::runtime::packFloat(" + value + "f));");
            } else if (cst instanceof Double value) {
                out.line("frame.stack.pushI64(ng::runtime::packDouble(" + value + "));");
            } else if (cst instanceof String value) {
                String escaped = CppStringEscaper.escape(value);
                String tmp = context.temp("str");
                out.line("jstring " + tmp + " = env->NewStringUTF(\"" + escaped + "\");");
                out.line("frame.stack.pushRef(" + tmp + ");");
            } else if (cst instanceof Type type) {
                switch (type.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY: {
                        String cls = context.temp("ldc_cls");
                        String clsLocal = context.temp("ldc_cls_local");
                        out.line("static jclass " + cls + " = nullptr;");
                        out.line("if (" + cls + " == nullptr) {");
                        out.indent();
                        String name = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
                        out.line("jclass " + clsLocal + " = env->FindClass(\"" + CppStringEscaper.escape(name) + "\");");
                        out.line(cls + " = static_cast<jclass>(env->NewGlobalRef(" + clsLocal + "));");
                        out.line("if (" + clsLocal + " != nullptr) env->DeleteLocalRef(" + clsLocal + ");");
                        out.outdent();
                        out.line("}");
                        String local = context.temp("ldc_local");
                        out.line("jobject " + local + " = " + cls + " == nullptr ? nullptr : env->NewLocalRef(" + cls + ");");
                        out.line("frame.stack.pushRef(" + local + ");");
                        break;
                    }
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                    case Type.LONG:
                    case Type.FLOAT:
                    case Type.DOUBLE:
                    case Type.VOID: {
                        char descriptor = type.getDescriptor().charAt(0);
                        String cls = context.temp("ldc_prim");
                        out.line("jclass " + cls + " = ng::runtime::GetPrimitiveClass(env, '" + descriptor + "');");
                        String local = context.temp("ldc_local");
                        out.line("jobject " + local + " = " + cls + " == nullptr ? nullptr : env->NewLocalRef(" + cls + ");");
                        out.line("frame.stack.pushRef(" + local + ");");
                        break;
                    }
                    case Type.METHOD: {
                        String mtCls = context.temp("ldc_mt_cls");
                        String mtLocal = context.temp("ldc_mt_local");
                        out.line("static jclass " + mtCls + " = nullptr;");
                        out.line("if (" + mtCls + " == nullptr) {");
                        out.indent();
                        out.line("jclass " + mtLocal + " = env->FindClass(\"java/lang/invoke/MethodType\");");
                        out.line(mtCls + " = static_cast<jclass>(env->NewGlobalRef(" + mtLocal + "));");
                        out.line("if (" + mtLocal + " != nullptr) env->DeleteLocalRef(" + mtLocal + ");");
                        out.outdent();
                        out.line("}");

                        String fromDesc = context.temp("ldc_mt_from");
                        out.line("static jmethodID " + fromDesc + " = nullptr;");
                        out.line("if (" + fromDesc + " == nullptr) {");
                        out.indent();
                        out.line(fromDesc + " = env->GetStaticMethodID(" + mtCls + ", \"fromMethodDescriptorString\", \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
                        out.outdent();
                        out.line("}");

                        String ownerCls = context.temp("ldc_owner_cls");
                        String ownerLocal = context.temp("ldc_owner_local");
                        out.line("static jclass " + ownerCls + " = nullptr;");
                        out.line("if (" + ownerCls + " == nullptr) {");
                        out.indent();
                        out.line("jclass " + ownerLocal + " = env->FindClass(\"" + context.method().ownerInternalName() + "\");");
                        out.line(ownerCls + " = static_cast<jclass>(env->NewGlobalRef(" + ownerLocal + "));");
                        out.line("if (" + ownerLocal + " != nullptr) env->DeleteLocalRef(" + ownerLocal + ");");
                        out.outdent();
                        out.line("}");

                        String classCls = context.temp("ldc_class_cls");
                        String classLocal = context.temp("ldc_class_local");
                        String getLoader = context.temp("ldc_class_loader_mid");
                        out.line("static jclass " + classCls + " = nullptr;");
                        out.line("static jmethodID " + getLoader + " = nullptr;");
                        out.line("if (" + classCls + " == nullptr) {");
                        out.indent();
                        out.line("jclass " + classLocal + " = env->FindClass(\"java/lang/Class\");");
                        out.line(classCls + " = static_cast<jclass>(env->NewGlobalRef(" + classLocal + "));");
                        out.line("if (" + classLocal + " != nullptr) env->DeleteLocalRef(" + classLocal + ");");
                        out.outdent();
                        out.line("}");
                        out.line("if (" + getLoader + " == nullptr) {");
                        out.indent();
                        out.line(getLoader + " = env->GetMethodID(" + classCls + ", \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
                        out.outdent();
                        out.line("}");

                        String loader = context.temp("ldc_loader");
                        out.line("jobject " + loader + " = env->CallObjectMethod(" + ownerCls + ", " + getLoader + ");");
                        InstructionUtils.emitExceptionReturn(context, out);

                        String descStr = context.temp("ldc_desc");
                        out.line("jstring " + descStr + " = env->NewStringUTF(\"" + CppStringEscaper.escape(type.getDescriptor()) + "\");");
                        String mt = context.temp("ldc_mt");
                        out.line("jobject " + mt + " = env->CallStaticObjectMethod(" + mtCls + ", " + fromDesc + ", " + descStr + ", " + loader + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                        out.line("if (" + descStr + " != nullptr) env->DeleteLocalRef(" + descStr + ");");
                        out.line("if (" + loader + " != nullptr) env->DeleteLocalRef(" + loader + ");");
                        out.line("frame.stack.pushRef(" + mt + ");");
                        break;
                    }
                    default:
                        out.line("// TODO: type constant not supported");
                        out.line("frame.stack.pushRef(nullptr);");
                        break;
                }
            } else {
                out.line("// TODO: unsupported ldc constant");
            }
        }
    }

