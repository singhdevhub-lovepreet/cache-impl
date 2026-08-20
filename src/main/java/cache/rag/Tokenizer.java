package cache.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Simple lexical tokenizer shared by BM25 encoding and keyword enrichment.
 * Lowercases, splits on non-alphanumeric boundaries, drops very short tokens and a
 * small English stopword list.
 */
@Component
public class Tokenizer {

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "if", "then", "of", "to", "in", "on",
            "for", "is", "are", "was", "were", "be", "been", "being", "at", "by", "with",
            "as", "it", "its", "this", "that", "these", "those", "i", "you", "he", "she",
            "we", "they", "do", "does", "did", "can", "will", "would", "should", "my",
            "your", "our", "from", "how", "what", "when", "where", "why", "which"
    );

    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.length() >= 2 && !STOPWORDS.contains(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    /**
     * Stable, positive index for a term. Both ingestion and query use this so the same
     * term always maps to the same sparse dimension.
     */
    public int termIndex(String term) {
        return term.hashCode() & 0x7fffffff;
    }
}
