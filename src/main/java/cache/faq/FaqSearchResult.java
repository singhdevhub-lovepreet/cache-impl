package cache.faq;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FaqSearchResult {
    private String question;
    private String answer;
    private double score;
}
