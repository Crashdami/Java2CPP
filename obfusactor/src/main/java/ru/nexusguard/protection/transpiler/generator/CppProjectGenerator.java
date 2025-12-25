package ru.nexusguard.protection.transpiler.generator;

import ru.nexusguard.protection.transpiler.core.OutputLayout;
import ru.nexusguard.protection.transpiler.model.ClassModel;
import ru.nexusguard.protection.transpiler.template.Template;
import ru.nexusguard.protection.transpiler.template.TemplateLoader;
import ru.nexusguard.protection.transpiler.util.NameUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CppProjectGenerator {
    private final TemplateLoader templateLoader;
    private final CppClassGenerator classGenerator;

    public CppProjectGenerator() {
        this(new TemplateLoader(), new CppClassGenerator());
    }

    public CppProjectGenerator(TemplateLoader templateLoader, CppClassGenerator classGenerator) {
        this.templateLoader = templateLoader;
        this.classGenerator = classGenerator;
    }

    public void generate(OutputLayout layout, List<ClassModel> classes) throws IOException {
        Files.createDirectories(layout.outputDir());

        Template runtimeTemplate = templateLoader.load("templates/native_vm.hpp.tpl");
        Template classTemplate = templateLoader.load("templates/class.cpp.tpl");
        Template jniTemplate = templateLoader.load("templates/main.cpp.tpl");
        Template cmakeTemplate = templateLoader.load("templates/CMakeLists.txt.tpl");

        Files.writeString(layout.runtimeHeader(), runtimeTemplate.render(Map.of()), StandardCharsets.US_ASCII);

        List<CppClassOutput> classOutputs = new ArrayList<>();
        for (ClassModel model : classes) {
            classOutputs.add(classGenerator.generate(layout, model, classTemplate));
        }

        writeJniOnLoad(layout, classOutputs, jniTemplate);
        writeCmake(layout, classOutputs, cmakeTemplate);
    }

    private void writeJniOnLoad(OutputLayout layout, List<CppClassOutput> outputs, Template template) throws IOException {
        StringBuilder declarations = new StringBuilder();
        StringBuilder entries = new StringBuilder();

        for (CppClassOutput output : outputs) {
            String internalName = output.internalName();
            declarations.append("extern JNINativeMethod ").append(NameUtil.nativeMethodsArrayName(internalName)).append("[];\n");
            declarations.append("extern jint ").append(NameUtil.nativeMethodsCountName(internalName)).append(";\n");
            declarations.append("extern const char* ").append(NameUtil.nativeClassNameVar(internalName)).append(";\n");
            declarations.append("extern int ").append(NameUtil.nativeClassIdVar(internalName)).append(";\n");
        }
        if (!outputs.isEmpty()) {
            declarations.append('\n');
        }

        for (CppClassOutput output : outputs) {
            String internalName = output.internalName();
            entries.append("    { ")
                    .append(NameUtil.nativeClassIdVar(internalName))
                    .append(", ")
                    .append(NameUtil.nativeClassNameVar(internalName))
                    .append(", ")
                    .append(NameUtil.nativeMethodsArrayName(internalName))
                    .append(", ")
                    .append(NameUtil.nativeMethodsCountName(internalName))
                    .append(" },\n");
        }

        Map<String, String> values = new HashMap<>();
        values.put("DECLARATIONS", declarations.toString());
        values.put("ENTRIES", entries.toString());

        String content = template.render(values);
        Files.writeString(layout.jniOnLoadFile(), content, StandardCharsets.US_ASCII);
    }

    private void writeCmake(OutputLayout layout, List<CppClassOutput> outputs, Template template) throws IOException {
        StringBuilder sources = new StringBuilder();
        sources.append("    main.cpp\n");
        for (CppClassOutput output : outputs) {
            sources.append("    ").append(output.fileName()).append('\n');
        }

        Map<String, String> values = new HashMap<>();
        values.put("SOURCES", sources.toString());

        String content = template.render(values);
        Files.writeString(layout.cmakeFile(), content, StandardCharsets.US_ASCII);
    }
}
