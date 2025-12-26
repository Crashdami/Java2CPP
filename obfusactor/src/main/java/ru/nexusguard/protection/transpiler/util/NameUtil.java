package ru.nexusguard.protection.transpiler.util;

public final class NameUtil {
    private NameUtil() {
    }

    public static String toIdentifier(String internalName) {
        String base = internalName.replace('/', '_').replace('$', '_');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public static String toFileName(String internalName) {
        return toIdentifier(internalName);
    }

    public static String sanitizeMethodName(String name) {
        return name.replace('<', '_').replace('>', '_');
    }

    public static String methodFunctionName(String internalName, String methodName, String desc) {
        String name = sanitizeMethodName(methodName);
        String hash = Integer.toHexString(desc.hashCode());
        return "m_" + toIdentifier(internalName) + "_" + name + "_" + hash;
    }

    public static String registerFunctionName(String internalName) {
        return "Register_" + toIdentifier(internalName);
    }

    public static int classId(String internalName) {
        return internalName.hashCode() & 0x7fffffff;
    }

    public static String nativeMethodsArrayName(String internalName) {
        return "__ngen_methods_" + toIdentifier(internalName);
    }

    public static String nativeMethodsCountName(String internalName) {
        return "__ngen_methods_count_" + toIdentifier(internalName);
    }

    public static String nativeClassNameVar(String internalName) {
        return "__ngen_class_name_" + toIdentifier(internalName);
    }

    public static String nativeClassIdVar(String internalName) {
        return "__ngen_class_id_" + toIdentifier(internalName);
    }
}
