package com.orderflow.service;

import javafx.concurrent.Task;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ExchangeRateService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";

    public Task<Double> fetchUsdToBdtRateTask() {
        return new Task<>() {
            @Override
            protected Double call() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("API returned status code " + response.statusCode());
                }

                JSONObject json = new JSONObject(response.body());
                JSONObject rates = json.getJSONObject("rates");
                return rates.getDouble("BDT");
            }
        };
    }
}
