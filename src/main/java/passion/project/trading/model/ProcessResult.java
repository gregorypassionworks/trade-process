package passion.project.trading.model;

public record ProcessResult(
        String id,
        String symbol,
        double notional,
        String status
) {}
