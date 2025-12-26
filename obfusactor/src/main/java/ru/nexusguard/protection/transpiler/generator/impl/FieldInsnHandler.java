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

public final class FieldInsnHandler implements InstructionHandler {
        private final TypeMapper typeMapper;

        public FieldInsnHandler(TypeMapper typeMapper) {
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
            boolean isStatic = field.getOpcode() == Opcodes.GETSTATIC || field.getOpcode() == Opcodes.PUTSTATIC;
            boolean isGet = field.getOpcode() == Opcodes.GETSTATIC || field.getOpcode() == Opcodes.GETFIELD;

            String cls = context.temp("cls_cache");
            String clsLocal = context.temp("cls_local");
            out.line("static jclass " + cls + " = nullptr;");
            out.line("if (" + cls + " == nullptr) {");
            out.indent();
            out.line("jclass " + clsLocal + " = env->FindClass(\"" + field.owner + "\");");
            out.line(cls + " = static_cast<jclass>(env->NewGlobalRef(" + clsLocal + "));");
            out.line("if (" + clsLocal + " != nullptr) env->DeleteLocalRef(" + clsLocal + ");");
            out.outdent();
            out.line("}");

            String fid = context.temp("fid_cache");
            out.line("static jfieldID " + fid + " = nullptr;");
            out.line("if (" + fid + " == nullptr) {");
            out.indent();
            if (isStatic) {
                out.line(fid + " = env->GetStaticFieldID(" + cls + ", \"" + field.name + "\", \"" + field.desc + "\");");
            } else {
                out.line(fid + " = env->GetFieldID(" + cls + ", \"" + field.name + "\", \"" + field.desc + "\");");
            }
            out.outdent();
            out.line("}");

            if (isGet) {
                String getter = isStatic ? typeMapper.staticGetter(fieldType) : typeMapper.staticGetter(fieldType).replace("Static", "");
                if (isStatic) {
                    String val = context.temp("val");
                    String valType = typeMapper.isReference(fieldType) ? "jobject" : typeMapper.toJniType(fieldType);
                    out.line(valType + " " + val + " = env->" + getter + "(" + cls + ", " + fid + ");");
                    InstructionUtils.emitExceptionReturn(context, out);
                    if (typeMapper.isReference(fieldType)) {
                        out.line("frame.stack.pushRef(" + val + ");");
                    } else if (fieldType.getSort() == Type.FLOAT) {
                        out.line("frame.stack.pushI64(ng::runtime::packFloat(" + val + "));");
                    } else if (fieldType.getSort() == Type.DOUBLE) {
                        out.line("frame.stack.pushI64(ng::runtime::packDouble(" + val + "));");
                    } else {
                        out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
                    }
                } else {
                    String obj = context.temp("obj");
                    out.line("jobject " + obj + " = frame.stack.popRef();");
                    String val = context.temp("val");
                    String valType = typeMapper.isReference(fieldType) ? "jobject" : typeMapper.toJniType(fieldType);
                    out.line(valType + " " + val + " = env->" + getter + "(" + obj + ", " + fid + ");");
                    InstructionUtils.emitExceptionReturn(context, out);
                    if (typeMapper.isReference(fieldType)) {
                        out.line("frame.stack.pushRef(" + val + ");");
                    } else if (fieldType.getSort() == Type.FLOAT) {
                        out.line("frame.stack.pushI64(ng::runtime::packFloat(" + val + "));");
                    } else if (fieldType.getSort() == Type.DOUBLE) {
                        out.line("frame.stack.pushI64(ng::runtime::packDouble(" + val + "));");
                    } else {
                        out.line("frame.stack.pushI64(static_cast<int64_t>(" + val + "));");
                    }
                    out.line("if (" + obj + " != nullptr) env->DeleteLocalRef(" + obj + ");");
                }
            } else {
                String setter = isStatic ? typeMapper.staticSetter(fieldType) : typeMapper.staticSetter(fieldType).replace("Static", "");
                if (isStatic) {
                    if (typeMapper.isReference(fieldType)) {
                        String val = context.temp("val");
                        out.line("jobject " + val + " = frame.stack.popRef();");
                        out.line("env->" + setter + "(" + cls + ", " + fid + ", " + val + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                        out.line("if (" + val + " != nullptr) env->DeleteLocalRef(" + val + ");");
                    } else if (fieldType.getSort() == Type.FLOAT) {
                        String val = context.temp("val");
                        out.line("jfloat " + val + " = ng::runtime::unpackFloat(frame.stack.popI64());");
                        out.line("env->" + setter + "(" + cls + ", " + fid + ", " + val + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                    } else if (fieldType.getSort() == Type.DOUBLE) {
                        String val = context.temp("val");
                        out.line("jdouble " + val + " = ng::runtime::unpackDouble(frame.stack.popI64());");
                        out.line("env->" + setter + "(" + cls + ", " + fid + ", " + val + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                    } else {
                        String val = context.temp("val");
                        out.line("int64_t " + val + " = frame.stack.popI64();");
                        out.line("env->" + setter + "(" + cls + ", " + fid + ", static_cast<" + typeMapper.toJniType(fieldType) + ">(" + val + "));");
                        InstructionUtils.emitExceptionReturn(context, out);
                    }
                } else {
                    if (typeMapper.isReference(fieldType)) {
                        String val = context.temp("val");
                        String obj = context.temp("obj");
                        out.line("jobject " + val + " = frame.stack.popRef();");
                        out.line("jobject " + obj + " = frame.stack.popRef();");
                        out.line("env->" + setter + "(" + obj + ", " + fid + ", " + val + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                        out.line("if (" + val + " != nullptr) env->DeleteLocalRef(" + val + ");");
                        out.line("if (" + obj + " != nullptr) env->DeleteLocalRef(" + obj + ");");
                    } else if (fieldType.getSort() == Type.FLOAT) {
                        String val = context.temp("val");
                        String obj = context.temp("obj");
                        out.line("jfloat " + val + " = ng::runtime::unpackFloat(frame.stack.popI64());");
                        out.line("jobject " + obj + " = frame.stack.popRef();");
                        out.line("env->" + setter + "(" + obj + ", " + fid + ", " + val + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                        out.line("if (" + obj + " != nullptr) env->DeleteLocalRef(" + obj + ");");
                    } else if (fieldType.getSort() == Type.DOUBLE) {
                        String val = context.temp("val");
                        String obj = context.temp("obj");
                        out.line("jdouble " + val + " = ng::runtime::unpackDouble(frame.stack.popI64());");
                        out.line("jobject " + obj + " = frame.stack.popRef();");
                        out.line("env->" + setter + "(" + obj + ", " + fid + ", " + val + ");");
                        InstructionUtils.emitExceptionReturn(context, out);
                        out.line("if (" + obj + " != nullptr) env->DeleteLocalRef(" + obj + ");");
                    } else {
                        String val = context.temp("val");
                        String obj = context.temp("obj");
                        out.line("int64_t " + val + " = frame.stack.popI64();");
                        out.line("jobject " + obj + " = frame.stack.popRef();");
                        out.line("env->" + setter + "(" + obj + ", " + fid + ", static_cast<" + typeMapper.toJniType(fieldType) + ">(" + val + "));");
                        InstructionUtils.emitExceptionReturn(context, out);
                        out.line("if (" + obj + " != nullptr) env->DeleteLocalRef(" + obj + ");");
                    }
                }
            }
        }
    }


