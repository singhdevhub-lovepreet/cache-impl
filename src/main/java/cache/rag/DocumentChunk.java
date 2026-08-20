package cache.rag;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * A single fixed-size slice of a parsed document as it flows through the pipeline.
 * <p>
 * {@code section} is the nearest markdown heading above the chunk (structure extraction).
 * {@code metadata} is progressively filled by the enrichment stage and becomes the
 * Qdrant point payload.
 */
@Data
public class DocumentChunk {
    private String text;
    private int chunkIndex;
    private String section;
    private Map<String, Object> metadata = new HashMap<>();

    public DocumentChunk(String text, int chunkIndex, String section) {
        this.text = text;
        this.chunkIndex = chunkIndex;
        this.section = section;
    }
}
