package cache.expense;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    private String expense;
    private String currency;
    private double money;
    private String merchant;
}
