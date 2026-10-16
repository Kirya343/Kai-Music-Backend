package org.kirya343.infrastructure.diagnostics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NoArgsConstructor;

@NoArgsConstructor 
public final class DiagnosticTextBuilder{

    private static final String INDENT = "    ";

    private String title = "";
    
    private List<DiagnosticTextBuilder> sections = new ArrayList<>();

    private List<String> contentRows = new ArrayList<>();

    public String getContent() {
        return getContent(0);
    }

    /**
     * structure:
     *  title
     *      content,
     *      content
     * 
     *      section title
     *          section content
     * 
     *      section title
     *          section content
     */
    private String getContent(int depth) {
        String indentation = INDENT.repeat(depth);

        String title = renderTitle(indentation);
        String content = renderContent(depth);
        String sectionsContent = renderSections(depth);

        StringBuilder result = new StringBuilder(title);

        if (!content.isEmpty()) {
            result.append("\n").append(content);
        }

        if (!sectionsContent.isEmpty()) {
            result.append("\n\n").append(sectionsContent);
        }

        return result.toString();
    }


    public DiagnosticTextBuilder addRow(String row) {
        this.contentRows.add(row);

        return this;
    }

    public DiagnosticTextBuilder addSection(DiagnosticTextBuilder section) {

        if (section == null) {
            section = new DiagnosticTextBuilder();
        }

        sections.add(section);

        return section;
    }

    public DiagnosticTextBuilder setTitle(String title) {
        this.title = title;

        return this;
    }

    private String renderTitle(String indentation) {
        return indentation + title;
    }

    private String renderContent(int depth) {
        String indentation = INDENT.repeat(depth + 1);

        return contentRows.stream()
            .map(row -> indentation + row)
            .collect(Collectors.joining("\n"));
    }

    private String renderSections(int depth) {
        return sections.stream()
            .map(section -> section.getContent(depth + 1))
            .collect(Collectors.joining("\n\n"));
    }

    public DiagnosticTextBuilder(String title) {
        this.title = title;
    }
}
