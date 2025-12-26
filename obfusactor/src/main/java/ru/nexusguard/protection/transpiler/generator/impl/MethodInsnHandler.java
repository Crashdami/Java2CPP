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

public final class MethodInsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        public MethodInsnHandler(TypeMapper typeMapper) {
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
                } else if (argType.getSort() == Type.FLOAT) {
                    out.line(jniType + " " + argName + " = ng::runtime::unpackFloat(frame.stack.popI64());");
                } else if (argType.getSort() == Type.DOUBLE) {
                    out.line(jniType + " " + argName + " = ng::runtime::unpackDouble(frame.stack.popI64());");
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
                InstructionUtils.emitExceptionReturn(context, out);
            } else {
                String jniType = typeMapper.toJniType(returnType);
                String ret = context.temp("ret");
                out.line(jniType + " " + ret + " = static_cast<" + jniType + ">(" + call + ");");
                InstructionUtils.emitExceptionReturn(context, out);
                if (typeMapper.isReference(returnType)) {
                    out.line("frame.stack.pushRef(" + ret + ");");
                } else if (returnType.getSort() == Type.FLOAT) {
                    out.line("frame.stack.pushI64(ng::runtime::packFloat(" + ret + "));");
                } else if (returnType.getSort() == Type.DOUBLE) {
                    out.line("frame.stack.pushI64(ng::runtime::packDouble(" + ret + "));");
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


