package cache.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces BM25 sparse vectors for documents and queries.
 * <p>
 * We only compute the <em>term-frequency</em> component of BM25 here:
 * <pre>
 *   weight(term) = (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * docLen / avgdl))
 * </pre>
 * The inverse-document-frequency component is applied on the server: the collection's
 * sparse vector is configured with {@code "modifier": "idf"}, so Qdrant multiplies each
 * dimension by the IDF it computes from the whole corpus at query time. That keeps IDF
 * statistics correct as the collection grows without us tracking them client-side.
 * <p>
 * Query vectors use raw term presence (weight 1.0 per query term); ranking contrast then
 * comes entirely from the document TF weights times the server-side IDF.
 */
@Service
public class Bm25SparseEncoder {

    private final Tokenizer tokenizer;
    private final double k1;
    private final double b;
    private final double avgdl;

    public Bm25SparseEncoder(Tokenizer tokenizer,
                             @Value("${rag.bm25.k1}") double k1,
                             @Value("${rag.bm25.b}") double b,
                             @Value("${rag.bm25.avgdl}") double avgdl) {
        this.tokenizer = tokenizer;
        this.k1 = k1;
        this.b = b;
        this.avgdl = avgdl;
    }

    /** Document-side encoding with BM25 term-frequency saturation and length normalization. */
    public SparseVector encodeDocument(String text) {
        List<String> tokens = tokenizer.tokenize(text);
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1, Integer::sum);
        }
        int docLen = tokens.size();
        double norm = k1 * (1 - b + b * (docLen / avgdl));

        Map<Integer, Double> weights = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            double f = e.getValue();
            double weight = (f * (k1 + 1)) / (f + norm);
            // Merge in case two terms hash to the same index (rare).
            weights.merge(tokenizer.termIndex(e.getKey()), weight, Double::sum);
        }
        return toSparseVector(weights);
    }

    /** Query-side encoding: presence weight of 1.0 per unique query term. */
    public SparseVector encodeQuery(String text) {
        Map<Integer, Double> weights = new HashMap<>();
        for (String token : tokenizer.tokenize(text)) {
            weights.put(tokenizer.termIndex(token), 1.0);
        }
        return toSparseVector(weights);
    }

    private SparseVector toSparseVector(Map<Integer, Double> weights) {
        List<Integer> indices = new java.util.ArrayList<>(weights.size());
        List<Double> values = new java.util.ArrayList<>(weights.size());
        for (Map.Entry<Integer, Double> e : weights.entrySet()) {
            indices.add(e.getKey());
            values.add(e.getValue());
        }
        return new SparseVector(indices, values);
    }
}
