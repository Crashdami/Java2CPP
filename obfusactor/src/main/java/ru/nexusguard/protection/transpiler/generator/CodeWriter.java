package ru.nexusguard.protection.transpiler.generator;

public final class CodeWriter {
    private final StringBuilder builder = new StringBuilder();
    private int indent;

    public void indent() {
        indent++;
    }

    public void outdent() {
        if (indent > 0) {
            indent--;
        }
    }

    public void line(String text) {
        for (int i = 0; i < indent; i++) {
            builder.append("    ");
        }
        builder.append(text).append('\n');
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
