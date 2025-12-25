package ru.nexusguard.protection.transpiler.generator;

import ru.nexusguard.protection.transpiler.model.MethodModel;
import ru.nexusguard.protection.transpiler.util.TypeMapper;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

public final class MethodContext {
    private final MethodModel method;
    private final TypeMapper typeMapper;
    private final LabelMapper labelMapper;
    private final TempVarAllocator temps;
    private boolean hasReturn;

    public MethodContext(MethodModel method, TypeMapper typeMapper, LabelMapper labelMapper, TempVarAllocator temps) {
        this.method = method;
        this.typeMapper = typeMapper;
        this.labelMapper = labelMapper;
        this.temps = temps;
    }

    public MethodModel method() {
        return method;
    }

    public TypeMapper typeMapper() {
        return typeMapper;
    }

    public String label(LabelNode label) {
        return labelMapper.name(label);
    }

    public String temp(String prefix) {
        return temps.next(prefix);
    }

    public void markReturn() {
        hasReturn = true;
    }

    public boolean hasReturn() {
        return hasReturn;
    }

    public Type returnType() {
        return Type.getReturnType(method.desc());
    }
}
