package cache.rag;

import cache.faq.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full ingestion pipeline:
 * <pre>
 *   Raw Document -> Parser -> Normalization -> Structure extraction
 *                -> Chunking -> Metadata enrichment -> Embedding -> Indexing
 * </pre>
 * Dense embeddings reuse the existing OpenAI {@link EmbeddingService}; sparse BM25 vectors
 * come from {@link Bm25SparseEncoder}.
 */
@Service
public class RagIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionService.class);

    private final DocumentParser parser;
    private final ChunkingService chunkingService;
    private final MetadataEnrichmentService enrichmentService;
    private final EmbeddingService embeddingService;
    private final Bm25SparseEncoder sparseEncoder;
    private final RagQdrantService qdrantService;

    public RagIngestionService(DocumentParser parser,
                               ChunkingService chunkingService,
                               MetadataEnrichmentService enrichmentService,
                               EmbeddingService embeddingService,
                               Bm25SparseEncoder sparseEncoder,
                               RagQdrantService qdrantService) {
        this.parser = parser;
        this.chunkingService = chunkingService;
        this.enrichmentService = enrichmentService;
        this.embeddingService = embeddingService;
        this.sparseEncoder = sparseEncoder;
        this.qdrantService = qdrantService;
    }

    public int ingest(String name, String rawText, Map<String, Object> userMetadata) {
        // 1. Parser + Normalization + Structure extraction
        ParsedDocument parsed = parser.parse(rawText);

        // 2. Chunking (fixed-size, section-aware)
        List<DocumentChunk> chunks = chunkingService.chunk(parsed);
        if (chunks.isEmpty()) {
            log.warn("Document '{}' produced no chunks; nothing to ingest", name);
            return 0;
        }

        // 3. Metadata enrichment
        enrichmentService.enrich(chunks, name, userMetadata);

        // 4. Embedding (dense in one batch call + sparse BM25 per chunk)
        List<String> texts = chunks.stream().map(DocumentChunk::getText).toList();
        List<List<Double>> denseVectors = embeddingService.embedBatch(texts);

        List<SparseVector> sparseVectors = new ArrayList<>(chunks.size());
        for (String text : texts) {
            sparseVectors.add(sparseEncoder.encodeDocument(text));
        }

        // 5. Indexing (collection dimension follows the embedding model)
        qdrantService.createCollectionIfNotExists(denseVectors.get(0).size());
        qdrantService.upsertChunks(chunks, denseVectors, sparseVectors);

        log.info("Ingested document '{}' as {} chunks", name, chunks.size());
        return chunks.size();
    }
}
