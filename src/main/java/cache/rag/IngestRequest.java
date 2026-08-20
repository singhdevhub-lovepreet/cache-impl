package cache.rag;

import lombok.Data;

import java.util.Map;

/**
 * Raw document submitted to the ingestion pipeline.
 * <p>
 * {@code name} is a human-readable document name (used both as metadata and as a
 * retrieval filter). {@code text} is the raw plain-text / markdown content.
 * {@code metadata} is an optional bag of user-supplied tags copied onto every chunk.
 */
@Data
public class IngestRequest {
    private String name;
    private String text;
    private Map<String, Object> metadata;
}
