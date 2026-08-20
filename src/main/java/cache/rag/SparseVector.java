package cache.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * A sparse vector in Qdrant's wire format: parallel lists of non-zero dimension
 * {@code indices} and their {@code values}. Serializes directly to
 * {@code {"indices": [...], "values": [...]}}.
 */
@Data
@AllArgsConstructor
public class SparseVector {
    private List<Integer> indices;
    private List<Double> values;
}


// dense vector (chunk Texts) --> openai small 3
// sparse vector --> bm25

// retrieval --> dense vector Top 5 vectors
// sparse bm25 --> Top 5 vectors

// 10 vectors --> Fuse (RRF) --> 0.1v1 + 0.5v1'