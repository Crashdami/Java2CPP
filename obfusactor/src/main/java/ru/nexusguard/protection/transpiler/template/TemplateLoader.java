package ru.nexusguard.protection.transpiler.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class TemplateLoader {
    public Template load(String path) throws IOException {
        try (InputStream input = TemplateLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Template not found: " + path);
            }
            String content = new String(input.readAllBytes(), StandardCharsets.US_ASCII);
            return new Template(content);
        }
    }
}
