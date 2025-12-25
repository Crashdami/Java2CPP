package ru.nexusguard.protection.transpiler.cli;

import java.nio.file.Path;

public record Arguments(Path inputJar, Path outputDir, boolean includeAll) {
}
