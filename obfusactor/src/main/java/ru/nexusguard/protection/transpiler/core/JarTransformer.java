package ru.nexusguard.protection.transpiler.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class JarTransformer {
    private final NativeClassRewriter classRewriter;
    private final NativeRunnerProvider runnerProvider;
    private static final String NATIVE_LIB_ENTRY = "ru/nexusguard/protection/native/library.dll";

    public JarTransformer(NativeClassRewriter classRewriter, NativeRunnerProvider runnerProvider) {
        this.classRewriter = classRewriter;
        this.runnerProvider = runnerProvider;
    }

    public void transform(Path inputJar, Path outputJar) throws IOException {
        Files.createDirectories(outputJar.getParent());
        String runnerEntryName = runnerProvider.entryName();

        Set<String> written = new HashSet<>();
        try (JarFile jarFile = new JarFile(inputJar.toFile());
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(outputJar))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (runnerEntryName.equals(name)) {
                    continue;
                }

                if (entry.isDirectory()) {
                    if (written.add(name)) {
                        JarEntry dir = new JarEntry(name);
                        output.putNextEntry(dir);
                        output.closeEntry();
                    }
                    continue;
                }

                try (InputStream input = jarFile.getInputStream(entry)) {
                    byte[] data = input.readAllBytes();
                    if (name.endsWith(".class")) {
                        ClassRewrite rewrite = classRewriter.rewrite(data);
                        if (rewrite != null) {
                            name = rewrite.entryName();
                            data = rewrite.bytecode();
                        }
                    }

                    if (!written.add(name)) {
                        continue;
                    }
                    JarEntry outEntry = new JarEntry(name);
                    output.putNextEntry(outEntry);
                    output.write(data);
                    output.closeEntry();
                }
            }

            byte[] runnerBytes = runnerProvider.loadBytes();
            if (written.add(runnerEntryName)) {
                JarEntry runnerEntry = new JarEntry(runnerEntryName);
                output.putNextEntry(runnerEntry);
                output.write(runnerBytes);
                output.closeEntry();
            }
        }

        attachNativeLibrary(outputJar);
    }

    private void attachNativeLibrary(Path outputJar) throws IOException {
        Path parent = outputJar.getParent();
        if (parent == null) {
            return;
        }
        Path cppOutput = parent.resolve("cpp").resolve("output");
        Path releaseLib = cppOutput.resolve("Release").resolve("library.dll");
        Path rootLib = cppOutput.resolve("library.dll");
        Path libPath = Files.exists(releaseLib) ? releaseLib : (Files.exists(rootLib) ? rootLib : null);
        if (libPath == null) {
            return;
        }

        Path tempJar = outputJar.resolveSibling(outputJar.getFileName().toString() + ".tmp");
        try (JarFile jarFile = new JarFile(outputJar.toFile());
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(tempJar))) {
            Set<String> written = new HashSet<>();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (NATIVE_LIB_ENTRY.equals(name)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    if (written.add(name)) {
                        output.putNextEntry(new JarEntry(name));
                        output.closeEntry();
                    }
                    continue;
                }
                try (InputStream input = jarFile.getInputStream(entry)) {
                    if (written.add(name)) {
                        output.putNextEntry(new JarEntry(name));
                        input.transferTo(output);
                        output.closeEntry();
                    }
                }
            }

            JarEntry libEntry = new JarEntry(NATIVE_LIB_ENTRY);
            output.putNextEntry(libEntry);
            try (InputStream input = Files.newInputStream(libPath)) {
                input.transferTo(output);
            }
            output.closeEntry();
        }
        Files.move(tempJar, outputJar, StandardCopyOption.REPLACE_EXISTING);
    }
}
