package ru.nexusguard.protection.transpiler.model;

import java.util.List;

public record ClassModel(String internalName, List<MethodModel> methods) {
}
