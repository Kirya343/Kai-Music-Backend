package org.kirya343.infrastructure.diagnostics.console;

import org.jline.terminal.Terminal;
import org.jline.utils.WCWidth;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class DiagnosticScreen {

    private static final int MAX_CONTENT_ENTRIES = 1000;
    private static final int MIN_TERMINAL_WIDTH = 10;

    private final Terminal terminal;
    private final Deque<String> content = new ArrayDeque<>();

    private String input = "";
    private String status = "ONLINE";

    /**
     * Количество визуальных строк от нижней границы содержимого.
     * 0 означает, что отображаются последние строки.
     */
    private int scrollOffset = 0;

    public DiagnosticScreen(Terminal terminal) {
        this.terminal = terminal;
    }

    private int contentLines() {
        return Math.max(1, terminal.getHeight() - 7);
    }

    private int terminalWidth() {
        return Math.max(MIN_TERMINAL_WIDTH, terminal.getWidth());
    }

    /**
     * Прокручивает содержимое вверх.
     *
     * @param amount количество визуальных строк
     */
    public synchronized void scrollUp(int amount) {
        if (amount <= 0) {
            return;
        }

        int maxOffset = getMaxScrollOffset();

        scrollOffset = (int) Math.min(
            maxOffset,
            (long) scrollOffset + amount
        );

        render();
    }

    /**
     * Прокручивает содержимое вниз.
     *
     * @param amount количество визуальных строк
     */
    public synchronized void scrollDown(int amount) {
        if (amount <= 0) {
            return;
        }

        scrollOffset = Math.max(0, scrollOffset - amount);

        render();
    }

    /**
     * Возвращает количество строк, доступных для отображения.
     */
    public int getContentLines() {
        return contentLines();
    }

    /**
     * Добавляет новую запись.
     *
     * Хранятся логические записи, а не визуальные строки:
     * перенос выполняется только при рендеринге.
     */
    public synchronized void addLine(String line) {
        content.addLast(line == null ? "null" : line);

        while (content.size() > MAX_CONTENT_ENTRIES) {
            content.removeFirst();
        }

        clampScrollOffset();
        render();
    }

    /**
     * Полностью заменяет отображаемое содержимое.
     */
    public synchronized void setContent(List<String> lines) {
        content.clear();

        int start = Math.max(0, lines.size() - MAX_CONTENT_ENTRIES);

        for (int i = start; i < lines.size(); i++) {
            content.addLast(
                lines.get(i) == null ? "null" : lines.get(i)
            );
        }

        // Новое содержимое показываем с конца.
        scrollOffset = 0;

        render();
    }

    public synchronized void setInput(String input) {
        this.input = input == null ? "" : input;
        render();
    }

    public synchronized void setStatus(String status) {
        this.status = status == null ? "" : status;
        render();
    }

    /**
     * Отрисовывает интерфейс целиком.
     */
    public synchronized void render() {
        int width = terminalWidth();
        int innerWidth = width - 2;
        int contentLines = contentLines();

        List<String> lines = new ArrayList<>();

        // Заголовок.
        String title = " KAI MUSIC DIAGNOSTICS ";
        int titleWidth = displayWidth(title);
        int titlePadding = Math.max(0, innerWidth - titleWidth);

        lines.add(
            "┌"
                + title
                + "─".repeat(titlePadding)
                + "┐"
        );

        // Статус.
        lines.add(
            "│"
                + padRight(
                    " Status: " + sanitize(status),
                    innerWidth
                )
                + "│"
        );

        lines.add("├" + "─".repeat(innerWidth) + "┤");

        // Подготавливаем содержимое с учётом переносов строк
        // и фактической ширины символов.
        List<String> allContent = getWrappedContent(innerWidth - 1);

        // Ограничиваем прокрутку актуальным размером содержимого.
        clampScrollOffset();

        int end = Math.max(0, allContent.size() - scrollOffset);
        int start = Math.max(0, end - contentLines);

        // Выводим видимую часть содержимого.
        for (int i = start; i < end; i++) {
            lines.add(
                "│"
                    + padRight(
                        " " + allContent.get(i),
                        innerWidth
                    )
                    + "│"
            );
        }

        // Заполняем свободное пространство.
        while (lines.size() < 3 + contentLines) {
            lines.add(
                "│" + " ".repeat(innerWidth) + "│"
            );
        }

        // Разделитель перед командной строкой.
        lines.add("├" + "─".repeat(innerWidth) + "┤");

        // Командная строка.
        lines.add(
            "│"
                + padRight(
                    " > " + sanitize(input),
                    innerWidth
                )
                + "│"
        );

        // Нижняя граница.
        lines.add("└" + "─".repeat(innerWidth) + "┘");

        // Отрисовка в альтернативном экране терминала.
        var writer = terminal.writer();

        writer.print("\033[H");

        for (String line : lines) {
            writer.print("\033[2K");
            writer.print(line);
            writer.print("\r\n");
        }

        writer.print("\033[J");
        writer.flush();
    }

    /**
     * Преобразует логические записи в визуальные строки.
     *
     * Обрабатывает:
     * - \n и \r\n внутри одной записи;
     * - длинные строки;
     * - Unicode-символы разной ширины;
     * - управляющие символы, способные нарушить интерфейс.
     */
    private List<String> getWrappedContent(int width) {
        List<String> result = new ArrayList<>();

        for (String entry : content) {
            String sanitized = sanitize(entry);

            String[] logicalLines = sanitized.split("\\R", -1);

            for (String line : logicalLines) {
                result.addAll(wrapText(line, width));
            }
        }

        return result;
    }

    /**
     * Переносит строку по фактической ширине символов терминала.
     */
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

            if (lineWidth + charWidth > width && !line.isEmpty()) {
                result.add(line.toString());

                line.setLength(0);
                lineWidth = 0;
            }

            // Защита от символа, который шире всей доступной строки.
            if (charWidth > width) {
                result.add(line.toString());
                line.setLength(0);
                lineWidth = 0;

                i += Character.charCount(codePoint);
                continue;
            }

            line.appendCodePoint(codePoint);
            lineWidth += charWidth;

            i += Character.charCount(codePoint);
        }

        if (!line.isEmpty() || result.isEmpty()) {
            result.add(line.toString());
        }

        return result;
    }

    /**
     * Дополняет строку пробелами до указанной визуальной ширины.
     * Если строка слишком длинная, обрезает её.
     */
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

    /**
     * Возвращает визуальную ширину строки.
     */
    private int displayWidth(String value) {
        int width = 0;

        for (int i = 0; i < value.length();) {
            int codePoint = value.codePointAt(i);

            width += Math.max(0, WCWidth.wcwidth(codePoint));

            i += Character.charCount(codePoint);
        }

        return width;
    }

    /**
     * Удаляет управляющие символы, кроме переводов строк.
     * ANSI escape-последовательности удаляются, чтобы логи
     * не могли перемещать курсор или нарушать рамку.
     */
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

    /**
     * Вычисляет максимальную прокрутку по визуальным строкам.
     */
    private int getMaxScrollOffset() {
        int width = terminalWidth() - 2;
        int totalLines = getWrappedContent(width - 1).size();

        return Math.max(0, totalLines - contentLines());
    }

    /**
     * Не допускает выхода scrollOffset за границы содержимого.
     */
    private void clampScrollOffset() {
        scrollOffset = Math.min(
            scrollOffset,
            getMaxScrollOffset()
        );

        scrollOffset = Math.max(0, scrollOffset);
    }
}