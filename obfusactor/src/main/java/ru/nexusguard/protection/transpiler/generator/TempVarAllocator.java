package ru.nexusguard.protection.transpiler.generator;

public final class TempVarAllocator {
    private int counter;

    public String next(String prefix) {
        return prefix + counter++;
    }
}
