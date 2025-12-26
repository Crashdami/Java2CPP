package ru.nexusguard.protection.transpiler.generator.impl;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import ru.nexusguard.protection.transpiler.generator.CodeWriter;
import ru.nexusguard.protection.transpiler.generator.MethodContext;
import ru.nexusguard.protection.transpiler.util.CppStringEscaper;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

import java.util.ArrayList;
import java.util.List;

public final class InvokeDynamicInsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        public InvokeDynamicInsnHandler(TypeMapper typeMapper) {
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
                if (isRunnableLambda(node)) {
                    emitRunnableLambda(node, context, out);
                } else if (isSupplierLambda(node)) {
                    emitSupplierLambda(node, context, out);
                } else if (isTypeSwitch(node)) {
                    emitTypeSwitch(node, context, out);
                } else {
                    emitFallback(node, context, out);
                }
                return;
            }

            Type returnType = Type.getReturnType(node.desc);
            if (!typeMapper.isReference(returnType)) {
                emitFallback(node, context, out);
                return;
            }

            emitStringConcat(node, context, out);
        }

        private boolean isRunnableLambda(InvokeDynamicInsnNode node) {
            if (!"()Ljava/lang/Runnable;".equals(node.desc)) {
                return false;
            }
            if (node.bsm == null) {
                return false;
            }
            if (!"java/lang/invoke/LambdaMetafactory".equals(node.bsm.getOwner())) {
                return false;
            }
            String name = node.bsm.getName();
            return "metafactory".equals(name) || "altMetafactory".equals(name);
        }

        private boolean isSupplierLambda(InvokeDynamicInsnNode node) {
            Type returnType = Type.getReturnType(node.desc);
            if (returnType.getSort() != Type.OBJECT || !"java/util/function/Supplier".equals(returnType.getInternalName())) {
                return false;
            }
            if (!"get".equals(node.name)) {
                return false;
            }
            if (node.bsm == null) {
                return false;
            }
            if (!"java/lang/invoke/LambdaMetafactory".equals(node.bsm.getOwner())) {
                return false;
            }
            String name = node.bsm.getName();
            if (!"metafactory".equals(name) && !"altMetafactory".equals(name)) {
                return false;
            }
            Type[] args = Type.getArgumentTypes(node.desc);
            return args.length == 1;
        }

        private boolean isTypeSwitch(InvokeDynamicInsnNode node) {
            if (node.bsm == null) {
                return false;
            }
            if (!"java/lang/runtime/SwitchBootstraps".equals(node.bsm.getOwner())) {
                return false;
            }
            if (!"typeSwitch".equals(node.bsm.getName())) {
                return false;
            }
            Type returnType = Type.getReturnType(node.desc);
            if (returnType.getSort() != Type.INT) {
                return false;
            }
            Type[] args = Type.getArgumentTypes(node.desc);
            if (args.length != 2) {
                return false;
            }
            if (args[0].getSort() != Type.OBJECT && args[0].getSort() != Type.ARRAY) {
                return false;
            }
            return args[1].getSort() == Type.INT;
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

        private void emitRunnableLambda(InvokeDynamicInsnNode node, MethodContext context, CodeWriter out) {
            if (node.bsmArgs == null || node.bsmArgs.length < 2 || !(node.bsmArgs[1] instanceof Handle handle)) {
                emitFallback(node, context, out);
                return;
            }

            String owner = handle.getOwner();
            String name = handle.getName();
            String desc = handle.getDesc();

            String helperCls = context.temp("dyn_cls");
            String helperLocal = context.temp("dyn_cls_local");
            out.line("static jclass " + helperCls + " = nullptr;");
            out.line("if (" + helperCls + " == nullptr) {");
            out.indent();
            out.line("jclass " + helperLocal + " = env->FindClass(\"ru/nexusguard/protection/native/InvokeDynamicHelper\");");
            out.line(helperCls + " = static_cast<jclass>(env->NewGlobalRef(" + helperLocal + "));");
            out.line("if (" + helperLocal + " != nullptr) env->DeleteLocalRef(" + helperLocal + ");");
            out.outdent();
            out.line("}");

            String helperMid = context.temp("dyn_mid");
            out.line("static jmethodID " + helperMid + " = nullptr;");
            out.line("if (" + helperMid + " == nullptr) {");
            out.indent();
            out.line(helperMid + " = env->GetStaticMethodID(" + helperCls + ", \"createRunnable\", \"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Runnable;\");");
            out.outdent();
            out.line("}");

            String ownerStr = context.temp("dyn_owner");
            String ownerLocal = context.temp("dyn_owner_local");
            String nameStr = context.temp("dyn_name");
            String nameLocal = context.temp("dyn_name_local");
            String descStr = context.temp("dyn_desc");
            String descLocal = context.temp("dyn_desc_local");
            out.line("static jstring " + ownerStr + " = nullptr;");
            out.line("if (" + ownerStr + " == nullptr) {");
            out.indent();
            out.line("jstring " + ownerLocal + " = env->NewStringUTF(\"" + CppStringEscaper.escape(owner) + "\");");
            out.line(ownerStr + " = static_cast<jstring>(env->NewGlobalRef(" + ownerLocal + "));");
            out.line("if (" + ownerLocal + " != nullptr) env->DeleteLocalRef(" + ownerLocal + ");");
            out.outdent();
            out.line("}");
            out.line("static jstring " + nameStr + " = nullptr;");
            out.line("if (" + nameStr + " == nullptr) {");
            out.indent();
            out.line("jstring " + nameLocal + " = env->NewStringUTF(\"" + CppStringEscaper.escape(name) + "\");");
            out.line(nameStr + " = static_cast<jstring>(env->NewGlobalRef(" + nameLocal + "));");
            out.line("if (" + nameLocal + " != nullptr) env->DeleteLocalRef(" + nameLocal + ");");
            out.outdent();
            out.line("}");
            out.line("static jstring " + descStr + " = nullptr;");
            out.line("if (" + descStr + " == nullptr) {");
            out.indent();
            out.line("jstring " + descLocal + " = env->NewStringUTF(\"" + CppStringEscaper.escape(desc) + "\");");
            out.line(descStr + " = static_cast<jstring>(env->NewGlobalRef(" + descLocal + "));");
            out.line("if (" + descLocal + " != nullptr) env->DeleteLocalRef(" + descLocal + ");");
            out.outdent();
            out.line("}");

            String runnable = context.temp("dyn_runnable");
            out.line("jobject " + runnable + " = env->CallStaticObjectMethod(" + helperCls + ", " + helperMid + ", " + ownerStr + ", " + nameStr + ", " + descStr + ");");
            InstructionUtils.emitExceptionReturn(context, out);
            out.line("frame.stack.pushRef(" + runnable + ");");
        }

        private void emitTypeSwitch(InvokeDynamicInsnNode node, MethodContext context, CodeWriter out) {
            if (node.bsmArgs == null || node.bsmArgs.length == 0) {
                emitFallback(node, context, out);
                return;
            }

            List<Type> types = new ArrayList<>();
            for (Object arg : node.bsmArgs) {
                if (arg instanceof Type type) {
                    types.add(type);
                } else {
                    emitFallback(node, context, out);
                    return;
                }
            }

            List<String> clsVars = new ArrayList<>();
            for (int i = 0; i < types.size(); i++) {
                Type type = types.get(i);
                String cls = context.temp("ts_cls");
                clsVars.add(cls);
                String clsLocal = context.temp("ts_cls_local");
                out.line("static jclass " + cls + " = nullptr;");
                out.line("if (" + cls + " == nullptr) {");
                out.indent();
                String name = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
                out.line("jclass " + clsLocal + " = env->FindClass(\"" + CppStringEscaper.escape(name) + "\");");
                out.line(cls + " = static_cast<jclass>(env->NewGlobalRef(" + clsLocal + "));");
                out.line("if (" + clsLocal + " != nullptr) env->DeleteLocalRef(" + clsLocal + ");");
                out.outdent();
                out.line("}");
            }

            String start = context.temp("ts_start");
            String obj = context.temp("ts_obj");
            String result = context.temp("ts_res");
            out.line("jint " + start + " = static_cast<jint>(frame.stack.popI64());");
            out.line("jobject " + obj + " = frame.stack.popRef();");
            out.line("jint " + result + " = " + types.size() + ";");
            out.line("if (" + start + " < 0) " + start + " = 0;");
            out.line("if (" + obj + " != nullptr && " + start + " < " + types.size() + ") {");
            out.indent();
            for (int i = 0; i < types.size(); i++) {
                String clsName = clsVars.get(i);
                if (i == 0) {
                    out.line("if (" + start + " <= " + i + " && env->IsInstanceOf(" + obj + ", " + clsName + ") == JNI_TRUE) {");
                } else {
                    out.line("else if (" + start + " <= " + i + " && env->IsInstanceOf(" + obj + ", " + clsName + ") == JNI_TRUE) {");
                }
                out.indent();
                out.line(result + " = " + i + ";");
                out.outdent();
                out.line("}");
            }
            out.outdent();
            out.line("}");
            out.line("frame.stack.pushI64(static_cast<int64_t>(" + result + "));");
            out.line("if (" + obj + " != nullptr) env->DeleteLocalRef(" + obj + ");");
        }

        private void emitSupplierLambda(InvokeDynamicInsnNode node, MethodContext context, CodeWriter out) {
            if (node.bsmArgs == null || node.bsmArgs.length < 2 || !(node.bsmArgs[1] instanceof Handle handle)) {
                emitFallback(node, context, out);
                return;
            }

            String owner = handle.getOwner();
            String name = handle.getName();
            String desc = handle.getDesc();

            String captured = context.temp("dyn_cap");
            out.line("jobject " + captured + " = frame.stack.popRef();");

            String helperCls = context.temp("dyn_cls");
            String helperLocal = context.temp("dyn_cls_local");
            out.line("static jclass " + helperCls + " = nullptr;");
            out.line("if (" + helperCls + " == nullptr) {");
            out.indent();
            out.line("jclass " + helperLocal + " = env->FindClass(\"ru/nexusguard/protection/native/InvokeDynamicHelper\");");
            out.line(helperCls + " = static_cast<jclass>(env->NewGlobalRef(" + helperLocal + "));");
            out.line("if (" + helperLocal + " != nullptr) env->DeleteLocalRef(" + helperLocal + ");");
            out.outdent();
            out.line("}");

            String helperMid = context.temp("dyn_mid");
            out.line("static jmethodID " + helperMid + " = nullptr;");
            out.line("if (" + helperMid + " == nullptr) {");
            out.indent();
            out.line(helperMid + " = env->GetStaticMethodID(" + helperCls + ", \"createSupplier\", \"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)Ljava/util/function/Supplier;\");");
            out.outdent();
            out.line("}");

            String ownerStr = context.temp("dyn_owner");
            String ownerLocal = context.temp("dyn_owner_local");
            String nameStr = context.temp("dyn_name");
            String nameLocal = context.temp("dyn_name_local");
            String descStr = context.temp("dyn_desc");
            String descLocal = context.temp("dyn_desc_local");
            out.line("static jstring " + ownerStr + " = nullptr;");
            out.line("if (" + ownerStr + " == nullptr) {");
            out.indent();
            out.line("jstring " + ownerLocal + " = env->NewStringUTF(\"" + CppStringEscaper.escape(owner) + "\");");
            out.line(ownerStr + " = static_cast<jstring>(env->NewGlobalRef(" + ownerLocal + "));");
            out.line("if (" + ownerLocal + " != nullptr) env->DeleteLocalRef(" + ownerLocal + ");");
            out.outdent();
            out.line("}");
            out.line("static jstring " + nameStr + " = nullptr;");
            out.line("if (" + nameStr + " == nullptr) {");
            out.indent();
            out.line("jstring " + nameLocal + " = env->NewStringUTF(\"" + CppStringEscaper.escape(name) + "\");");
            out.line(nameStr + " = static_cast<jstring>(env->NewGlobalRef(" + nameLocal + "));");
            out.line("if (" + nameLocal + " != nullptr) env->DeleteLocalRef(" + nameLocal + ");");
            out.outdent();
            out.line("}");
            out.line("static jstring " + descStr + " = nullptr;");
            out.line("if (" + descStr + " == nullptr) {");
            out.indent();
            out.line("jstring " + descLocal + " = env->NewStringUTF(\"" + CppStringEscaper.escape(desc) + "\");");
            out.line(descStr + " = static_cast<jstring>(env->NewGlobalRef(" + descLocal + "));");
            out.line("if (" + descLocal + " != nullptr) env->DeleteLocalRef(" + descLocal + ");");
            out.outdent();
            out.line("}");

            String supplier = context.temp("dyn_supplier");
            out.line("jobject " + supplier + " = env->CallStaticObjectMethod(" + helperCls + ", " + helperMid + ", " + ownerStr + ", " + nameStr + ", " + descStr + ", " + captured + ");");
            InstructionUtils.emitExceptionReturn(context, out);
            out.line("frame.stack.pushRef(" + supplier + ");");
            out.line("if (" + captured + " != nullptr) env->DeleteLocalRef(" + captured + ");");
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
                } else if (argType.getSort() == Type.FLOAT) {
                    out.line(jniType + " " + argName + " = ng::runtime::unpackFloat(frame.stack.popI64());");
                } else if (argType.getSort() == Type.DOUBLE) {
                    out.line(jniType + " " + argName + " = ng::runtime::unpackDouble(frame.stack.popI64());");
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
            InstructionUtils.emitExceptionReturn(context, out);
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
            InstructionUtils.emitExceptionReturn(context, out);
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
            InstructionUtils.emitExceptionReturn(context, out);
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


