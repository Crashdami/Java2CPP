package ru.nexusguard.protection.transpiler.generator.impl;

import org.objectweb.asm.Type;

import ru.nexusguard.protection.transpiler.generator.CodeWriter;
import ru.nexusguard.protection.transpiler.generator.MethodContext;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

public final class InstructionUtils {
    private InstructionUtils() {
    }

    public static void emitExceptionReturn(MethodContext context, CodeWriter out) {
        out.line("if (env->ExceptionCheck()) {");
        out.indent();
        Type returnType = context.returnType();
        TypeMapper typeMapper = context.typeMapper();
        if (returnType.getSort() == Type.VOID) {
            out.line("return;");
        } else if (typeMapper.isReference(returnType)) {
            out.line("return nullptr;");
        } else {
            out.line("return static_cast<" + typeMapper.toJniType(returnType) + ">(0);");
        }
        out.outdent();
        out.line("}");
    }
}

