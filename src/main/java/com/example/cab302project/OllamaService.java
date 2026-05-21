package com.example.cab302project;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.models.chat.OllamaChatResult;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Small helper for requesting crime summaries from a local Ollama model.
 * Ollama4j wraps the local HTTP API so controllers do not need to build JSON requests directly.
 */
public class OllamaService {
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.2";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private final Ollama ollama;
    private final String model;

    public OllamaService() {
        this(resolveConfigValue("OLLAMA_BASE_URL", DEFAULT_BASE_URL),
                resolveConfigValue("OLLAMA_MODEL", DEFAULT_MODEL));
    }

    public OllamaService(String baseUrl, String model) {
        this.ollama = new Ollama(baseUrl);
        this.ollama.setRequestTimeoutSeconds(REQUEST_TIMEOUT_SECONDS);
        this.model = model;
    }

    /**
     * Sends a prompt to Ollama and returns the generated response text.
     *
     * @param prompt the complete prompt to send to the local model
     * @return the model response text
     * @throws IOException if Ollama is not running, the model is missing, or the response is invalid
     */
    public String generateSummary(String prompt) throws IOException {
        try {
            // The chat request maps to Ollama's local /api/chat endpoint with stream=false by default.
            OllamaChatRequest request = OllamaChatRequest.builder()
                    .withModel(model)
                    .withMessage(OllamaChatMessageRole.USER, prompt)
                    .build();

            // This method is called from a background JavaFX task, so the local model call cannot freeze the UI.
            OllamaChatResult result = ollama.chat(request, null);
            String response = extractResponseText(result);

            if (response.isBlank()) {
                throw new IOException("Ollama returned an empty response.");
            }

            return response;
        } catch (OllamaException exception) {
            throw toFriendlyIOException(exception);
        }
    }

    /**
     * Allows OLLAMA_BASE_URL and OLLAMA_MODEL to be supplied as environment variables or JVM properties.
     */
    private static String resolveConfigValue(String key, String defaultValue) {
        String systemPropertyValue = System.getProperty(key);
        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return systemPropertyValue.trim();
        }

        String environmentValue = System.getenv(key);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }

        return defaultValue;
    }

    /**
     * Ollama4j returns a chat response model; this keeps response parsing in one reusable place.
     */
    private String extractResponseText(OllamaChatResult result) throws IOException {
        if (result == null || result.getResponseModel() == null) {
            throw new IOException("Ollama returned an unexpected response format.");
        }

        OllamaChatResponseModel responseModel = result.getResponseModel();
        if (responseModel.getError() != null && !responseModel.getError().isBlank()) {
            throw new IOException("Ollama error: " + responseModel.getError());
        }

        OllamaChatMessage message = responseModel.getMessage();
        if (message == null || message.getResponse() == null) {
            throw new IOException("Ollama response did not include assistant text.");
        }

        return message.getResponse().trim();
    }

    /**
     * Converts common local Ollama failures into clearer messages for logging and controller fallbacks.
     */
    private IOException toFriendlyIOException(OllamaException exception) {
        Throwable cause = exception.getCause();
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        String lowerMessage = message.toLowerCase();

        if (cause instanceof ConnectException || lowerMessage.contains("connection refused")) {
            return new IOException("Ollama is not running at the configured local URL.", exception);
        }

        if (cause instanceof SocketTimeoutException || lowerMessage.contains("timed out") || lowerMessage.contains("timeout")) {
            return new IOException("Ollama request timed out.", exception);
        }

        if (lowerMessage.contains("model") && (lowerMessage.contains("not found") || lowerMessage.contains("pull"))) {
            return new IOException("Ollama model '" + model + "' is not available. Run: ollama pull " + model, exception);
        }

        return new IOException("Ollama summary request failed: " + message, exception);
    }
}
