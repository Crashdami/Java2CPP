package ru.nexusguard.protection.transpiler.generator;

import ru.nexusguard.protection.transpiler.core.OutputLayout;
import ru.nexusguard.protection.transpiler.model.ClassModel;
import ru.nexusguard.protection.transpiler.model.MethodModel;
import ru.nexusguard.protection.transpiler.template.Template;
import ru.nexusguard.protection.transpiler.util.NameUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CppClassGenerator {
    private final CppMethodEmitter methodEmitter;

    public CppClassGenerator() {
        this(new CppMethodEmitter());
    }

    public CppClassGenerator(CppMethodEmitter methodEmitter) {
        this.methodEmitter = methodEmitter;
    }

    public CppClassOutput generate(OutputLayout layout, ClassModel model, Template classTemplate) throws IOException {
        StringBuilder methodsBlock = new StringBuilder();
        List<NativeBinding> bindings = new ArrayList<>();

        for (MethodModel method : model.methods()) {
            MethodOutput output = methodEmitter.emit(method);
            methodsBlock.append(output.code()).append('\n');
            bindings.add(new NativeBinding(method.name(), method.desc(), output.functionName()));
        }

        String registration = buildRegistry(model.internalName(), bindings);

        Map<String, String> values = new HashMap<>();
        values.put("METHODS", methodsBlock.toString());
        values.put("REGISTRATION", registration);

        String content = classTemplate.render(values);
        var filePath = layout.classFile(model.internalName());
        Files.writeString(filePath, content, StandardCharsets.US_ASCII);

        return new CppClassOutput(
                model.internalName(),
                filePath.getFileName().toString());
    }

    private String buildRegistry(String internalName, List<NativeBinding> bindings) {
        CodeWriter writer = new CodeWriter();
        String methodsArray = NameUtil.nativeMethodsArrayName(internalName);
        String methodsCount = NameUtil.nativeMethodsCountName(internalName);
        String classNameVar = NameUtil.nativeClassNameVar(internalName);
        String classIdVar = NameUtil.nativeClassIdVar(internalName);

        writer.line("const char* " + classNameVar + " = \"" + internalName + "\";");
        writer.line("int " + classIdVar + " = " + NameUtil.classId(internalName) + ";");
        writer.line("JNINativeMethod " + methodsArray + "[] = {");
        writer.indent();
        for (NativeBinding binding : bindings) {
            writer.line("{\"" + binding.javaName() + "\", \"" + binding.descriptor() + "\", reinterpret_cast<void*>(&" + binding.functionName() + ")},");
        }
        writer.outdent();
        writer.line("};");
        writer.line("jint " + methodsCount + " = static_cast<jint>(sizeof(" + methodsArray + ") / sizeof(" + methodsArray + "[0]));");
        return writer.toString();
    }
}
