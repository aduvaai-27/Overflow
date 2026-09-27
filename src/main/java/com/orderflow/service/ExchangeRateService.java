package com.orderflow.service;

import javafx.concurrent.Task;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches the live USD -> BDT exchange rate from a public REST API
 * (https://open.er-api.com) and parses the JSON response.
 *
 * This isn't a standalone demo: the Dashboard uses it to show Total Revenue
 * and Capital as an approximate USD figure as well as Tk, which is genuinely
 * useful context for a business that may deal with foreign suppliers or
 * wants an internationally comparable snapshot of its finances. The call
 * runs on a background thread (javafx.concurrent.Task) so the UI never
 * freezes while waiting on the network, and the Dashboard degrades quietly
 * (it just keeps showing Tk only) if the API can't be reached.
 */
public class ExchangeRateService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";

    /** A background Task that resolves to how many BDT one USD currently buys. */
    public Task<Double> fetchUsdToBdtRateTask() {
        return new Task<>() {
            @Override
            protected Double call() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Exchange rate API returned HTTP " + response.statusCode());
                }

                JSONObject json = new JSONObject(response.body());
                JSONObject rates = json.getJSONObject("rates");
                return rates.getDouble("BDT");
            }
        };
    }
}
