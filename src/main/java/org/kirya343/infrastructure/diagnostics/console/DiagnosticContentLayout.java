package org.kirya343.infrastructure.diagnostics.console;

import java.util.ArrayList;
import java.util.List;

import org.jline.utils.WCWidth;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticContentLayout {

    public List<String> wrap(List<String> content, int width) {
        List<String> result = new ArrayList<>();

        for (String entry : content) {
            String sanitized = sanitize(
                entry == null ? "null" : entry
            );

            String[] logicalLines = sanitized.split("\\R", -1);

            for (String line : logicalLines) {
                result.addAll(wrapText(line, width));
            }
        }

        return result;
    }

    private List<String> wrapText(String text, int width) {
        List<String> result = new ArrayList<>();

        if (width <= 0) {
            result.add(text);
            return result;
        }

        if (text.isEmpty()) {
            result.add("");
            return result;
        }

        StringBuilder line = new StringBuilder();
        int lineWidth = 0;

        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            int charWidth = Math.max(0, WCWidth.wcwidth(codePoint));

            if (charWidth > width) {
                if (line.length() > 0) {
                    result.add(line.toString());
                    line.setLength(0);
                    lineWidth = 0;
                }

                i += Character.charCount(codePoint);
                continue;
            }

            if (lineWidth + charWidth > width && line.length() > 0) {
                result.add(line.toString());

                line.setLength(0);
                lineWidth = 0;
            }

            line.appendCodePoint(codePoint);
            lineWidth += charWidth;

            i += Character.charCount(codePoint);
        }

        if (line.length() > 0 || result.isEmpty()) {
            result.add(line.toString());
        }

        return result;
    }

    private String sanitize(String value) {
        String withoutAnsi = value.replaceAll(
            "\u001B\\[[0-?]*[ -/]*[@-~]",
            ""
        );

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < withoutAnsi.length();) {
            int codePoint = withoutAnsi.codePointAt(i);

            if (codePoint == '\n' || codePoint == '\r') {
                result.appendCodePoint(codePoint);
            } else if (codePoint == '\t') {
                result.append("    ");
            } else if (!Character.isISOControl(codePoint)) {
                result.appendCodePoint(codePoint);
            }

            i += Character.charCount(codePoint);
        }

        return result.toString();
    }
}