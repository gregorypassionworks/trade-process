package passion.project.trading.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import passion.project.trading.app.TradeProcessApplication;
import passion.project.trading.model.TradeRequest;
import passion.project.trading.model.ProcessResult;
import java.util.List;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TradeProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class TradeControllerIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    private List<TradeRequest> trades;

    @BeforeEach
    void setUp() {
        trades = List.of(
                tr("T-0001","EUR/USD",100000,1.1010),
                tr("T-0001","GBP/USD",150000,1.2750),
                tr("T-0002","USD/JPY",200000,145.25),
                tr("T-0004","AUD/USD",120000,0.6650),
                tr("T-0005","USD/CAD",180000,1.3450),
                tr("T-0003","GBP/USD",160000,1.2760),
                tr("T-0006","NZD/USD",110000,0.5950),
                tr("T-0002","USD/JPY",210000,145.30),
                tr("T-0008","USD/CHF",130000,0.8900),
                tr("T-0008","EUR/JPY",140000,159.80)
        );
    }

    private TradeRequest tr(String id, String sym, double qty, double price) {
        TradeRequest t = new TradeRequest();
        t.setId(id);
        t.setSymbol(sym);
        t.setQuantity(qty);
        t.setPrice(price);
        return t;
    }

    private void verifyOrder(List<ProcessResult> results, String id) {
        // 1) Extract the symbols for this ID in the order they were sent
        List<String> expected = trades.stream()
                .filter(t -> t.getId().equals(id))
                .map(TradeRequest::getSymbol)
                .collect(Collectors.toList());

        // 2) Extract the symbols for this ID in the order they were received
        List<String> actual = results.stream()
                .filter(r -> r.id().equals(id))
                .map(ProcessResult::symbol)
                .collect(Collectors.toList());

        // 3) Assert they match exactly
        assertThat(actual)
                .as("Order preserved for ID " + id)
                .isEqualTo(expected);
    }
    @Test
    void testProcessTrades() {
        webClient.post().uri("/trades/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trades)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProcessResult.class)
                .hasSize(trades.size())  // ← this line drives the subscription for ALL 10 items
                .consumeWith(b -> {
                    List<ProcessResult> results = b.getResponseBody();
                    Assertions.assertNotNull(results);
                    verifyOrder(results, "T-0001");
                    verifyOrder(results, "T-0002");
                    verifyOrder(results, "T-0008");
                });
    }
}
