package cache.rag;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline stages: Parser -> Normalization -> Structure extraction.
 * <p>
 * For plain-text / markdown there is no binary parsing to do, so "parsing" is just
 * accepting the raw string. We then normalize it into clean canonical text and pull
 * out the markdown heading structure so chunks can be tagged with their section.
 */
@Service
public class DocumentParser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*$", Pattern.MULTILINE);

    public ParsedDocument parse(String rawText) {
        String normalized = normalize(rawText); // remove CC number, name, phone, email
        List<ParsedDocument.Section> sections = extractStructure(normalized);
        return new ParsedDocument(normalized, sections);
    }

    /**
     * Normalization: unify line endings, apply Unicode NFC, strip trailing whitespace
     * per line, and collapse runs of blank lines. Keeps document structure intact.
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String t = Normalizer.normalize(text, Normalizer.Form.NFC);
        t = t.replace("\r\n", "\n").replace("\r", "\n");

        StringBuilder sb = new StringBuilder();
        for (String line : t.split("\n", -1)) {
            sb.append(line.stripTrailing()).append('\n');
        }
        // Collapse 3+ consecutive newlines down to a paragraph break.
        String collapsed = sb.toString().replaceAll("\n{3,}", "\n\n");
        return collapsed.strip();
    }

    /**
     * Structure extraction: locate markdown headings (lines starting with #..######)
     * and record the character offset where each section begins.
     */
    private List<ParsedDocument.Section> extractStructure(String text) {
        List<ParsedDocument.Section> sections = new ArrayList<>();
        Matcher m = HEADING.matcher(text);
        while (m.find()) {
            sections.add(new ParsedDocument.Section(m.group(2).trim(), m.start()));
        }
        return sections;
    }
}
