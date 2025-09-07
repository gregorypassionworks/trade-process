package passion.project.trading.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import passion.project.trading.dispatcher.TradeDispatcher;
import passion.project.trading.model.TradeRequest;
import passion.project.trading.model.ProcessResult;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/trades")
public class TradeController {

    private final TradeDispatcher dispatcher = new TradeDispatcher(5);

    @PostMapping("/process")
    public Flux<ProcessResult> processTrades(@RequestBody Flux<TradeRequest> trades) {
        return dispatcher.dispatch(trades);
    }
}