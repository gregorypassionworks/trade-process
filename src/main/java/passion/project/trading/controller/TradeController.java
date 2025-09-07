package passion.project.trading.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import passion.project.trading.model.TradeRequest;
import passion.project.trading.model.ProcessResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/trades")
public class TradeController {

    @PostMapping("/process")
    public Flux<ProcessResult> processTrades(@RequestBody Flux<TradeRequest> trades) {
        System.out.println("Received trades");
        return trades.flatMap(this::processTrade, 20);
    }

    private Mono<ProcessResult> processTrade(TradeRequest trade) {
        return Mono.delay(Duration.ofSeconds(1))
                .map(delay -> new ProcessResult(
                        trade.getId(),
                        trade.getSymbol(),
                        trade.getQuantity() * trade.getPrice(),
                        "PROCESSED"
                ));
    }
}
