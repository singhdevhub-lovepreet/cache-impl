package cache.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Output of the parse + normalize + structure-extraction stages.
 * {@code normalizedText} is clean text ready for chunking; {@code sections} are the
 * markdown headings discovered in the document, ordered by their offset in the text.
 */
@Data
@AllArgsConstructor
public class ParsedDocument {
    private String normalizedText;
    private List<Section> sections;

    @Data
    @AllArgsConstructor
    public static class Section {
        /** Heading title (without the leading '#' markers). */
        private String title;
        /** Character offset in {@code normalizedText} where this section starts. */
        private int startOffset;
    }
}
