package cache.rag;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST surface for the RAG pipeline.
 * <ul>
 *   <li>POST /v1/rag/ingest - run a raw document through the ingestion pipeline</li>
 *   <li>POST /v1/rag/search - hybrid (dense + BM25) search with metadata pre-filtering</li>
 * </ul>
 * text (http) --> api ("text", (userId, createdAt))
 *
 */
@RestController
@RequestMapping("/v1/rag")
public class RagController {

    private final RagIngestionService ingestionService;
    private final RagSearchService searchService;

    public RagController(RagIngestionService ingestionService, RagSearchService searchService) {
        this.ingestionService = ingestionService;
        this.searchService = searchService;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody IngestRequest request) {
        int chunks = ingestionService.ingest(request.getName(), request.getText(), request.getMetadata());
        return Map.of(
                "status", "ingested",
                "name", request.getName(),
                "chunks", chunks
        );
    }

    @PostMapping("/search")
    public List<RagSearchResult> search(@RequestBody SearchRequest request) {
        return searchService.search(request);
    }
}
