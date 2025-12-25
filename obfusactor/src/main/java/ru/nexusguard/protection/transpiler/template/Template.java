package ru.nexusguard.protection.transpiler.template;

import java.util.Map;

public final class Template {
    private final String raw;

    public Template(String raw) {
        this.raw = raw;
    }

    public String render(Map<String, String> values) {
        String result = raw;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
