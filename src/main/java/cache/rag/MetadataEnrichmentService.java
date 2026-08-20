package cache.rag;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pipeline stage: Metadata enrichment.
 * <p>
 * Populates each chunk's payload with retrieval-time metadata: the source document
 * name, chunk index, section, ingestion timestamp (kept as epoch millis so it can be
 * range-filtered, plus an ISO string for readability), a length hint, and a small set
 * of high-frequency keywords used for keyword pre-filtering. Any user-supplied metadata
 * from the ingest request is merged in as well.
 */
@Service
public class MetadataEnrichmentService {

    private static final int MAX_KEYWORDS = 10;

    private final Tokenizer tokenizer;

    public MetadataEnrichmentService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public void enrich(List<DocumentChunk> chunks, String documentName, Map<String, Object> userMetadata) {
        long now = System.currentTimeMillis();
        String nowIso = Instant.ofEpochMilli(now).toString();

        for (DocumentChunk chunk : chunks) {
            Map<String, Object> meta = chunk.getMetadata();
            if (userMetadata != null) {
                meta.putAll(userMetadata);
            }
            meta.put("name", documentName);
            meta.put("chunkIndex", chunk.getChunkIndex());
            if (chunk.getSection() != null) {
                meta.put("section", chunk.getSection());
            }
            meta.put("createdAt", now);
            meta.put("createdAtIso", nowIso);
            meta.put("charCount", chunk.getText().length());
            meta.put("keywords", topKeywords(chunk.getText()));
        }
    }

    /** Most frequent content terms in the chunk, used for cheap keyword filtering. */
    private List<String> topKeywords(String text) {
        Map<String, Long> freq = tokenizer.tokenize(text).stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        return freq.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(MAX_KEYWORDS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
    }
}
