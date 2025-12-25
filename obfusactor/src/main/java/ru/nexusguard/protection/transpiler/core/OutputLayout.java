package ru.nexusguard.protection.transpiler.core;

import ru.nexusguard.protection.transpiler.util.NameUtil;

import java.nio.file.Path;

public final class OutputLayout {
    private final Path baseDir;
    private final Path cppDir;
    private final Path outputDir;

    public OutputLayout(Path baseDir) {
        this.baseDir = baseDir;
        this.cppDir = baseDir.resolve("cpp");
        this.outputDir = cppDir.resolve("output");
    }

    public Path outputDir() {
        return outputDir;
    }

    public Path cmakeFile() {
        return outputDir.resolve("CMakeLists.txt");
    }

    public Path runtimeHeader() {
        return outputDir.resolve("native_vm.hpp");
    }

    public Path jniOnLoadFile() {
        return outputDir.resolve("main.cpp");
    }

    public Path classFile(String internalName) {
        String fileName = NameUtil.toFileName(internalName) + "_jni.cpp";
        return outputDir.resolve(fileName);
    }
}
