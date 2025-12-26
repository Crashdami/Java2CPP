package ru.nexusguard.protection.transpiler.core;

import org.objectweb.asm.tree.ClassNode;

public record ClassSource(String internalName, byte[] bytecode, ClassNode node) {
}
