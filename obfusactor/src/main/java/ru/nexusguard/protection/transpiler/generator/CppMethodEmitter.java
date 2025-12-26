package ru.nexusguard.protection.transpiler.generator;

import ru.nexusguard.protection.transpiler.model.MethodModel;
import ru.nexusguard.protection.transpiler.util.NameUtil;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public final class CppMethodEmitter {
    private final TypeMapper typeMapper;
    private final BytecodeTranslator translator;

    public CppMethodEmitter() {
        TypeMapper mapper = new TypeMapper();
        this.typeMapper = mapper;
        this.translator = new BytecodeTranslator(mapper);
    }

    public CppMethodEmitter(TypeMapper typeMapper, BytecodeTranslator translator) {
        this.typeMapper = typeMapper;
        this.translator = translator;
    }

    public MethodOutput emit(MethodModel method) {
        boolean isStatic = (method.access() & Opcodes.ACC_STATIC) != 0;
        String functionName = NameUtil.methodFunctionName(method.ownerInternalName(), method.name(), method.desc());

        Type returnType = Type.getReturnType(method.desc());
        String jniReturnType = typeMapper.toJniType(returnType);

        Type[] argTypes = Type.getArgumentTypes(method.desc());
        List<Param> params = new ArrayList<>();
        for (int i = 0; i < argTypes.length; i++) {
            params.add(new Param("p" + i, argTypes[i]));
        }

        StringBuilder signature = new StringBuilder();
        if (isStatic) {
            signature.append("JNIEnv* env, jclass clazz");
        } else {
            signature.append("JNIEnv* env, jobject instance");
        }
        for (Param param : params) {
            signature.append(", ").append(typeMapper.toJniType(param.type)).append(' ').append(param.name);
        }

        CodeWriter writer = new CodeWriter();
        writer.line("static " + jniReturnType + " " + functionName + "(" + signature + ") {");
        writer.indent();
        writer.line("Frame frame(" + method.maxStack() + ", " + method.maxLocals() + ");");

        int localIndex = 0;
        if (!isStatic) {
            writer.line("frame.locals.setRef(0, instance);");
            localIndex = 1;
        }

        for (Param param : params) {
            if (typeMapper.isReference(param.type)) {
                writer.line("frame.locals.setRef(" + localIndex + ", " + param.name + ");");
            } else {
                writer.line("frame.locals.setI64(" + localIndex + ", static_cast<int64_t>(" + param.name + "));");
            }
            localIndex += param.type.getSize();
        }

        MethodContext context = new MethodContext(method, typeMapper, new LabelMapper(method.node().instructions), new TempVarAllocator());
        translator.emit(context, writer);

        if (!context.hasReturn()) {
            emitDefaultReturn(writer, returnType);
        }

        writer.outdent();
        writer.line("}");
        return new MethodOutput(functionName, writer.toString());
    }

    private void emitDefaultReturn(CodeWriter writer, Type returnType) {
        if (returnType.getSort() == Type.VOID) {
            writer.line("return;");
            return;
        }

        if (typeMapper.isReference(returnType)) {
            writer.line("return nullptr;");
            return;
        }

        writer.line("return static_cast<" + typeMapper.toJniType(returnType) + ">(0);");
    }

    private static final class Param {
        private final String name;
        private final Type type;

        private Param(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    }
}
