package ru.nexusguard.protection.transpiler.model;

import org.objectweb.asm.tree.MethodNode;

public record MethodModel(String ownerInternalName, MethodNode node) {
    public String name() {
        return node.name;
    }

    public String desc() {
        return node.desc;
    }

    public int access() {
        return node.access;
    }

    public int maxStack() {
        return node.maxStack;
    }

    public int maxLocals() {
        return node.maxLocals;
    }
}
