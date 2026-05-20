package com.example.cab302project;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small helper for requesting crime summaries from a local Ollama model.
 */
public class OllamaService {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "llama3.2";

    private final HttpClient httpClient;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Sends a prompt to Ollama and returns the generated response text.
     *
     * @param prompt the complete prompt to send to the local model
     * @return the model response text
     * @throws IOException if Ollama is not running or the response cannot be read
     * @throws InterruptedException if the request is interrupted
     */
    public String generateSummary(String prompt) throws IOException, InterruptedException {
        // Ollama expects a small JSON body with the model, prompt, and stream=false for one response.
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", DEFAULT_MODEL);
        requestJson.put("prompt", prompt);
        requestJson.put("stream", false);

        // The request stays local to Ollama; no external AI API or API key is required.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        // This method is called from a background thread so the blocking HTTP call does not freeze JavaFX.
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama returned status " + response.statusCode());
        }

        // With stream=false, Ollama returns the generated text in the response JSON field.
        JSONObject responseJson = new JSONObject(response.body());
        return responseJson.optString("response", "").trim();
    }
}
