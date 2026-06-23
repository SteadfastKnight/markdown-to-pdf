package com.steadfastknight.md2pdf;

import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.List;

/**
 * Markdown -> HTML, matching the Python `markdown` call with
 * extensions=["extra", "sane_lists", "nl2br"]:
 *   - tables / footnotes / definition lists  ~= "extra"
 *   - SOFT_BREAK = "<br/>"                    == "nl2br" (single newline -> br)
 *   - flexmark's default list handling        ~= "sane_lists"
 */
public final class MarkdownRenderer {

    private static final MutableDataSet OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, List.of(
                    TablesExtension.create(),
                    FootnoteExtension.create(),
                    DefinitionExtension.create()))
            // nl2br: render a soft line break as <br>.
            .set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    private MarkdownRenderer() {}

    /** Renders Markdown to an HTML body fragment (no document wrapper). */
    public static String toBody(String mdText) {
        return RENDERER.render(PARSER.parse(mdText));
    }
}
