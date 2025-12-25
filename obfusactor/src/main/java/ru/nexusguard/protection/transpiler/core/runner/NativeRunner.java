package ru.nexusguard.protection.transpiler.core.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

public final class NativeRunner {
    private static volatile boolean initialized;

    private NativeRunner() {
    }

    public static void call(String className) {
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (initialized) {
            return;
        }
        synchronized (NativeRunner.class) {
            if (initialized) {
                return;
            }
            loadFromResource();
            initialized = true;
        }
    }

    private static void loadFromResource() {
        try (InputStream input = NativeRunner.class.getResourceAsStream("/ru/nexusguard/protection/native/library.dll")) {
            if (input == null) {
                throw new IllegalStateException("Native protection-library not found.");
            }
            Path tempDir = Files.createTempDirectory("ng-native");
            Path tempLib = tempDir.resolve("library" + new Random().nextInt() + ".dll");
            try (OutputStream output = Files.newOutputStream(
                    tempLib,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                input.transferTo(output);
            }
            tempLib.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            System.load(tempLib.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load native protection-library: ", e);
        }
    }

}
