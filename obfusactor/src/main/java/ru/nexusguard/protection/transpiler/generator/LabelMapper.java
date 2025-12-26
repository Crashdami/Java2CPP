package ru.nexusguard.protection.transpiler.generator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.HashMap;
import java.util.Map;

public final class LabelMapper {
    private final Map<LabelNode, String> names = new HashMap<>();

    public LabelMapper(InsnList instructions) {
        int index = 0;
        for (AbstractInsnNode node = instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof LabelNode label) {
                names.put(label, "L" + index++);
            }
        }
    }

    public String name(LabelNode label) {
        return names.get(label);
    }
}
