package cache.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline stage: Chunking (fixed-size, character-based, with overlap).
 * <p>
 * Slides a window of {@code rag.chunk.size} characters across the normalized text,
 * stepping forward by {@code size - overlap} so adjacent chunks share {@code overlap}
 * characters of context. Each chunk is tagged with the nearest preceding markdown
 * heading from the extracted structure.
 */
@Service
public class ChunkingService {

    private final int chunkSize;
    private final int overlap;

    public ChunkingService(@Value("${rag.chunk.size}") int chunkSize,
                           @Value("${rag.chunk.overlap}") int overlap) {
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("rag.chunk.overlap must be smaller than rag.chunk.size");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<DocumentChunk> chunk(ParsedDocument doc) {
        String text = doc.getNormalizedText();
        List<DocumentChunk> chunks = new ArrayList<>();
        if (text.isEmpty()) {
            return chunks;
        }

        int step = chunkSize - overlap;
        int index = 0;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + chunkSize, text.length());
            String slice = text.substring(start, end).strip();
            if (!slice.isEmpty()) {
                chunks.add(new DocumentChunk(slice, index++, sectionFor(doc, start)));
            }
            if (end == text.length()) {
                break;
            }
        }
        return chunks;
    }

    /** Nearest heading whose offset is at or before this chunk's start offset. */
    private String sectionFor(ParsedDocument doc, int startOffset) {
        String section = null;
        for (ParsedDocument.Section s : doc.getSections()) {
            if (s.getStartOffset() <= startOffset) {
                section = s.getTitle();
            } else {
                break;
            }
        }
        return section;
    }
}
