package cache.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * A single hybrid-search hit returned to the caller.
 * {@code score} is the RRF-fused rank score produced by Qdrant's Query API.
 */
@Data
@AllArgsConstructor
public class RagSearchResult {
    private String documentName;
    private int chunkIndex;
    private String section;
    private String text;
    private double score;
    private Map<String, Object> metadata;
}
