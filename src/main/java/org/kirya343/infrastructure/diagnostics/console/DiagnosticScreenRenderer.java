package org.kirya343.infrastructure.diagnostics.console;

import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.WCWidth;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DiagnosticScreenRenderer {

    private static final int MIN_TERMINAL_WIDTH = 10;

    private final Terminal terminal;

    public synchronized void render(DiagnosticScreenFrame frame) {
        int width = terminalWidth();
        int innerWidth = width - 2;

        List<String> lines = new ArrayList<>();

        renderHeader(lines, frame.status(), innerWidth);
        renderContent(
            lines,
            frame.content(),
            frame.contentHeight(),
            innerWidth
        );
        renderFooter(lines, frame.input(), innerWidth);

        var writer = terminal.writer();

        writer.print("\033[H");

        for (String line : lines) {
            writer.print("\033[2K");
            writer.print(line);
            writer.print("\r\n");
        }

        writer.print("\033[J");
        writer.flush();

        System.out.printf(
            "Terminal type: %s, width: %d, height: %d%n",
            terminal.getType(),
            terminal.getWidth(),
            terminal.getHeight()
        );
    }

    private void renderHeader(
        List<String> lines,
        String status,
        int innerWidth
    ) {
        String title = " KAI MUSIC DIAGNOSTICS ";
        int titleWidth = displayWidth(title);

        int remainingWidth = Math.max(0, innerWidth - titleWidth);

        lines.add(
            "┌"
                + title
                + "─".repeat(remainingWidth)
                + "┐"
        );

        lines.add(
            "│"
                + padRight(" Status: " + status, innerWidth)
                + "│"
        );

        lines.add("├" + "─".repeat(innerWidth) + "┤");
    }

    private void renderContent(
        List<String> lines,
        List<String> content,
        int contentHeight,
        int innerWidth
    ) {
        for (int i = 0; i < contentHeight; i++) {
            String contentLine = i < content.size()
                ? " " + content.get(i)
                : "";

            lines.add(
                "│"
                    + padRight(contentLine, innerWidth)
                    + "│"
            );
        }
    }

    private void renderFooter(
        List<String> lines,
        String input,
        int innerWidth
    ) {
        lines.add("├" + "─".repeat(innerWidth) + "┤");

        lines.add(
            "│"
                + padRight(" > " + input, innerWidth)
                + "│"
        );

        lines.add("└" + "─".repeat(innerWidth) + "┘");
    }

    private int terminalWidth() {
        return Math.max(MIN_TERMINAL_WIDTH, terminal.getWidth());
    }

    private String padRight(String value, int width) {
        StringBuilder result = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < value.length();) {
            int codePoint = value.codePointAt(i);
            int charWidth = Math.max(0, WCWidth.wcwidth(codePoint));

            if (currentWidth + charWidth > width) {
                break;
            }

            result.appendCodePoint(codePoint);
            currentWidth += charWidth;

            i += Character.charCount(codePoint);
        }

        if (currentWidth < width) {
            result.append(" ".repeat(width - currentWidth));
        }

        return result.toString();
    }

    private int displayWidth(String value) {
        return value.codePoints()
            .map(codePoint -> Math.max(0, WCWidth.wcwidth(codePoint)))
            .sum();
    }
}