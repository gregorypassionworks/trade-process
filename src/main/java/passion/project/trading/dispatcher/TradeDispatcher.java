package passion.project.trading.dispatcher;

import passion.project.trading.model.TradeRequest;
import passion.project.trading.model.ProcessResult;
import reactor.core.publisher.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TradeDispatcher {

    private final int maxConcurrency;

    // Tracks how many trades are actively processing
    private final AtomicInteger activeCount = new AtomicInteger(0);

    // Per-ID queues
    private final Map<String, Deque<TradeRequest>> queues = new ConcurrentHashMap<>();

    // Currently active IDs
    private final Set<String> activeIds = ConcurrentHashMap.newKeySet();

    // Flag set when inbound completes
    private final AtomicBoolean inboundCompleted = new AtomicBoolean(false);

    public TradeDispatcher(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Feeds in trades from the inbound Flux, returns a Flux of ProcessResult that
     * completes when inbound completes and all queued trades finish.
     */
    public Flux<ProcessResult> dispatch(Flux<TradeRequest> inbound) {
        return Flux.create(sink -> {
            // 1) Subscribe inbound here (only once), feeding our queues
            inbound.subscribe(
                    trade -> {
                        enqueue(trade);
                        tryDispatch(sink);
                    },
                    sink::error,
                    () -> {
                        inboundCompleted.set(true);
                        // final dispatch to clear out remaining
                        tryDispatch(sink);
                    }
            );

            // 2) When downstream cancels, we should stop
            sink.onCancel(() -> System.out.println("Downstream cancelled"));
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private void enqueue(TradeRequest trade) {
        queues.computeIfAbsent(trade.getId(), id -> new ArrayDeque<>())
                .offer(trade);
    }

    /**
     * Attempt to dispatch as many trades as allowed by maxConcurrency,
     * picking the next available ID that isn’t already active.
     */
    private void tryDispatch(FluxSink<ProcessResult> sink) {
        synchronized (this) {
            // If we're at cap, no new dispatch
            if (activeCount.get() >= maxConcurrency) {
                completeIfDone(sink);
                return;
            }

            for (var entry : queues.entrySet()) {
                String id = entry.getKey();
                Deque<TradeRequest> queue = entry.getValue();

                // if nothing queued, remove the empty queue
                if (queue.isEmpty()) {
                    queues.remove(id);
                    continue;
                }

                // skip IDs already in flight
                if (activeIds.contains(id)) {
                    continue;
                }

                // we have a ready trade for this ID
                TradeRequest next = queue.pollFirst();
                if (queue.isEmpty()) {
                    queues.remove(id);
                }

                activeIds.add(id);
                activeCount.incrementAndGet();

                // process it
                processTrade(next)
                        .doFinally(sig -> {
                            activeIds.remove(id);
                            activeCount.decrementAndGet();
                            tryDispatch(sink); // trigger another round
                        })
                        .subscribe(
                                sink::next,
                                sink::error
                        );

                // enforce global cap
                if (activeCount.get() >= maxConcurrency) {
                    break;
                }
            }

            completeIfDone(sink);
        }
    }

    /** When inbound is done and no more active or queued trades, complete the sink */
    private void completeIfDone(FluxSink<ProcessResult> sink) {
        if (inboundCompleted.get() && queues.isEmpty() && activeCount.get() == 0) {
            sink.complete();
        }
    }

    /** Simulate a 1-second processing delay and produce a ProcessResult */
    private Mono<ProcessResult> processTrade(TradeRequest t) {
        System.out.println("START [" + t.getId() + "] at " + LocalTime.now() + " | Active: " + activeCount.get());
        return Mono.delay(Duration.ofSeconds(1))
                .map(__ -> {
                    ProcessResult r = new ProcessResult(
                            t.getId(),
                            t.getSymbol(),
                            t.getQuantity() * t.getPrice(),
                            "PROCESSED"
                    );
                    System.out.println("END   [" + t.getId() + "] at " + LocalTime.now() + " | Active: " + (activeCount.get()-1));
                    return r;
                });
    }
}


