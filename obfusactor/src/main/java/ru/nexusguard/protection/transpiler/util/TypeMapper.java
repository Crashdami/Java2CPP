package ru.nexusguard.protection.transpiler.util;

import org.objectweb.asm.Type;

public final class TypeMapper {
    public enum StackKind {
        I64,
        REF
    }

    public String toJniType(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return "void";
            case Type.BOOLEAN:
                return "jboolean";
            case Type.BYTE:
                return "jbyte";
            case Type.CHAR:
                return "jchar";
            case Type.SHORT:
                return "jshort";
            case Type.INT:
                return "jint";
            case Type.FLOAT:
                return "jfloat";
            case Type.LONG:
                return "jlong";
            case Type.DOUBLE:
                return "jdouble";
            case Type.ARRAY:
                return "jobject";
            case Type.OBJECT:
                if ("java/lang/String".equals(type.getInternalName())) {
                    return "jstring";
                }
                return "jobject";
            default:
                return "jobject";
        }
    }

    public StackKind stackKind(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                return StackKind.REF;
            default:
                return StackKind.I64;
        }
    }

    public boolean isReference(Type type) {
        return stackKind(type) == StackKind.REF;
    }

    public String callMethod(Type type, boolean isStatic) {
        String prefix = isStatic ? "CallStatic" : "Call";
        switch (type.getSort()) {
            case Type.VOID:
                return prefix + "VoidMethod";
            case Type.BOOLEAN:
                return prefix + "BooleanMethod";
            case Type.BYTE:
                return prefix + "ByteMethod";
            case Type.CHAR:
                return prefix + "CharMethod";
            case Type.SHORT:
                return prefix + "ShortMethod";
            case Type.INT:
                return prefix + "IntMethod";
            case Type.FLOAT:
                return prefix + "FloatMethod";
            case Type.LONG:
                return prefix + "LongMethod";
            case Type.DOUBLE:
                return prefix + "DoubleMethod";
            case Type.OBJECT:
            case Type.ARRAY:
                return prefix + "ObjectMethod";
            default:
                return prefix + "ObjectMethod";
        }
    }

    public String callNonvirtualMethod(Type type) {
        String prefix = "CallNonvirtual";
        switch (type.getSort()) {
            case Type.VOID:
                return prefix + "VoidMethod";
            case Type.BOOLEAN:
                return prefix + "BooleanMethod";
            case Type.BYTE:
                return prefix + "ByteMethod";
            case Type.CHAR:
                return prefix + "CharMethod";
            case Type.SHORT:
                return prefix + "ShortMethod";
            case Type.INT:
                return prefix + "IntMethod";
            case Type.FLOAT:
                return prefix + "FloatMethod";
            case Type.LONG:
                return prefix + "LongMethod";
            case Type.DOUBLE:
                return prefix + "DoubleMethod";
            case Type.OBJECT:
            case Type.ARRAY:
                return prefix + "ObjectMethod";
            default:
                return prefix + "ObjectMethod";
        }
    }

    public String staticGetter(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return "GetStaticBooleanField";
            case Type.BYTE:
                return "GetStaticByteField";
            case Type.CHAR:
                return "GetStaticCharField";
            case Type.SHORT:
                return "GetStaticShortField";
            case Type.INT:
                return "GetStaticIntField";
            case Type.FLOAT:
                return "GetStaticFloatField";
            case Type.LONG:
                return "GetStaticLongField";
            case Type.DOUBLE:
                return "GetStaticDoubleField";
            case Type.OBJECT:
            case Type.ARRAY:
                return "GetStaticObjectField";
            default:
                return "GetStaticObjectField";
        }
    }

    public String staticSetter(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return "SetStaticBooleanField";
            case Type.BYTE:
                return "SetStaticByteField";
            case Type.CHAR:
                return "SetStaticCharField";
            case Type.SHORT:
                return "SetStaticShortField";
            case Type.INT:
                return "SetStaticIntField";
            case Type.FLOAT:
                return "SetStaticFloatField";
            case Type.LONG:
                return "SetStaticLongField";
            case Type.DOUBLE:
                return "SetStaticDoubleField";
            case Type.OBJECT:
            case Type.ARRAY:
                return "SetStaticObjectField";
            default:
                return "SetStaticObjectField";
        }
    }
}
