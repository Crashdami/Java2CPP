package ru.nexusguard.protection.transpiler.core;

import ru.nexusguard.protection.transpiler.cli.Arguments;
import ru.nexusguard.protection.transpiler.generator.CppProjectGenerator;
import ru.nexusguard.protection.transpiler.model.ClassModel;

import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TranspilerApp {
    private final JarClassScanner classScanner;
    private final NativeMethodCollector methodCollector;
    private final CppProjectGenerator projectGenerator;
    private final JarTransformer jarTransformer;

    public TranspilerApp() {
        this(new JarClassScanner(),
                new NativeMethodCollector(),
                new CppProjectGenerator(),
                new JarTransformer(new NativeClassRewriter(), new NativeRunnerProvider()));
    }

    public TranspilerApp(JarClassScanner classScanner,
                         NativeMethodCollector methodCollector,
                         CppProjectGenerator projectGenerator,
                         JarTransformer jarTransformer) {
        this.classScanner = classScanner;
        this.methodCollector = methodCollector;
        this.projectGenerator = projectGenerator;
        this.jarTransformer = jarTransformer;
    }

    public void run(Arguments args) throws Exception {
        if (!Files.exists(args.inputJar())) {
            throw new IllegalArgumentException("Input jar not found: " + args.inputJar());
        }
        Files.createDirectories(args.outputDir());

        List<ClassSource> sources = classScanner.scan(args.inputJar());
        List<ClassModel> models = new ArrayList<>();
        for (ClassSource source : sources) {
            ClassNode node = source.node();
            ClassModel model = methodCollector.collect(node);
            if (model != null) {
                models.add(model);
            }
        }

        OutputLayout layout = new OutputLayout(args.outputDir());
        projectGenerator.generate(layout, models);

        Path outputJar = args.outputDir().resolve(args.inputJar().getFileName());
        jarTransformer.transform(args.inputJar(), outputJar);

        System.out.println("C++ output written to " + layout.outputDir());
        System.out.println("Rewritten jar written to " + outputJar);
    }
}
