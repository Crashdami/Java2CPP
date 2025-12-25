package ru.nexusguard.protection.transpiler.cli;

import java.nio.file.Path;

public final class ArgumentsParser {
    private ArgumentsParser() {
    }

    public static Arguments parse(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected: <inputJar> <outDirectory> [-a]");
        }

        Path inputJar = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);

        boolean includeAll = false;
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if ("-a".equals(arg) || "--all".equals(arg)) {
                includeAll = true;
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                throw new IllegalArgumentException("Help requested.");
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }

        return new Arguments(inputJar, outputDir, includeAll);
    }

    public static void printUsage() {
        System.out.println("Usage: java -jar obfuscator.jar <inputJar> <outDirectory> [-a]");
        System.out.println("  -a, --all   scan all classes for @Native methods");
    }
}
