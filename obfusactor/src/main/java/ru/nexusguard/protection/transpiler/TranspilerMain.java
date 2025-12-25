package ru.nexusguard.protection.transpiler;

import ru.nexusguard.protection.transpiler.cli.Arguments;
import ru.nexusguard.protection.transpiler.cli.ArgumentsParser;
import ru.nexusguard.protection.transpiler.core.TranspilerApp;

public final class TranspilerMain {
    private TranspilerMain() {
    }

    public static void main(String[] args) {
        Arguments parsed;
        try {
            parsed = ArgumentsParser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            ArgumentsParser.printUsage();
            System.exit(1);
            return;
        }

        try {
            new TranspilerApp().run(parsed);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
