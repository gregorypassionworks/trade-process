package passion.project.trading.model;

import lombok.Data;

@Data
public class TradeRequest {
    private String id;
    private String symbol;
    private double quantity;
    private double price;
}
