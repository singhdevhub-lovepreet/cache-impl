package cache.rag;

import lombok.Data;

/**
 * Hybrid search request.
 * <p>
 * {@code query} is the natural-language question. The remaining fields are optional
 * metadata filters applied <em>before</em> the vector/BM25 search so we only search a
 * smaller slice of the collection instead of the whole vector space:
 * <ul>
 *   <li>{@code name} - restrict to a single source document</li>
 *   <li>{@code keyword} - require the chunk's enriched keyword list to contain this term</li>
 *   <li>{@code createdAfter} - epoch millis; keep only chunks ingested at/after this time</li>
 * </ul>
 */
@Data
public class SearchRequest {
    private String query;
    private String name;
    private String keyword;
    private Long createdAfter;
    private Integer topK;
}
