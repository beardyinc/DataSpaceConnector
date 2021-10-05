package org.eclipse.dataspaceconnector.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.List;
import java.util.Queue;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

public class Crawler implements Runnable {

    private final List<CrawlerProtocolAdapter> adapters;
    private final Monitor monitor;
    private final Queue<UpdateResponse> queue;
    private final RetryPolicy<Object> retryPolicy;

    public Crawler(List<CrawlerProtocolAdapter> adapters, Monitor monitor, Queue<UpdateResponse> queue, RetryPolicy<Object> retryPolicy) {
        this.adapters = adapters;
        this.monitor = monitor;
        this.queue = queue;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void run() {
        adapters.forEach(adapter -> {
            adapter.sendRequest(new UpdateRequest())
                    .whenComplete((updateResponse, throwable) -> {
                        if (throwable != null) {
                            handleError(throwable);
                        } else {
                            handleResponse(updateResponse);
                        }
                    });
        });
    }

    private void handleError(Throwable throwable) {
        monitor.severe("An update-request failed", throwable);
    }

    private void handleResponse(UpdateResponse updateResponse) {
        monitor.info(format("update-response received: %s", updateResponse.toString()));
        var offered = with(retryPolicy).get(() -> queue.offer(updateResponse));
        if (!offered) {
            monitor.severe("Inserting update-response into queue failed due to timeout!");
        }
    }
}
