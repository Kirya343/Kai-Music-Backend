package org.kirya343.infrastructure.diagnostics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DiagnosticTextBuilder{

    private static final String INDENT = "    ";

    private String title = "";
    
    private List<DiagnosticTextBuilder> sections = new ArrayList<>();

    private List<String> contentRows = new ArrayList<>();

    public String getContent() {
        return getContent(0);
    }

    private String getContent(int depth) {
        String indentation = INDENT.repeat(depth);

        return String.join("\n",
            renderTitle(indentation),
            renderContent(depth),
            "",
            renderSections(depth)
        );
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
            .collect(Collectors.joining("\n"));
    }
}
