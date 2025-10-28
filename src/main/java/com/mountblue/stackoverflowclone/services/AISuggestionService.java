package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class AISuggestionService {
    private final QuestionRepository questionRepository;
    private final String geminiApiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    // v1beta + model placeholder
    private static final String GEMINI_GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${gemini.model-id:gemini-2.5-flash-lite}")
    private String modelId;

    public AISuggestionService(QuestionRepository questionRepository,
                               @Value("${gemini.api-key}") String geminiApiKey) {
        this.questionRepository = questionRepository;
        this.geminiApiKey = geminiApiKey;
    }

    public String getResponse(Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(parser.parse(q.getBody() == null ? "" : q.getBody()));
        String plainText = html.replaceAll("<img[^>]*>", "[image]")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .trim();

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return "AI suggestions are unavailable (missing API key).";
        }

        try {
            String prompt = buildPrompt(q.getTitle(), plainText);
            String url = String.format(GEMINI_GENERATE_URL, modelId, geminiApiKey);

            Map<String, Object> generationConfig = Map.of(
                    "temperature", 0.4,
                    "maxOutputTokens", 512
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", generationConfig
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return "No response from AI model.";

            Object candidatesObj = body.get("candidates");
            if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty())
                return "AI did not return any suggestions.";

            Map<?,?> firstCandidate = (Map<?,?>) candidates.get(0);
            Map<?,?> content = (Map<?,?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");

            StringBuilder sb = new StringBuilder();
            if (parts != null) {
                for (Object p : parts) {
                    if (p instanceof Map<?,?> pm && pm.get("text") != null) {
                        sb.append(pm.get("text"));
                    }
                }
            }
            String out = sb.toString().trim();
            if (out.isEmpty()) return "AI returned an empty response.";
            // Convert AI Markdown to HTML so UI can render like question content
            String aiHtml = renderer.render(parser.parse(out));
            return aiHtml;
        } catch (HttpClientErrorException.TooManyRequests ex) {
            String retryAfter = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null;
            if (retryAfter != null && retryAfter.isBlank()) retryAfter = null;
            return "AI rate limit exceeded. Please try again" + (retryAfter != null ? " after " + retryAfter + " seconds." : ".");
        } catch (HttpClientErrorException ex) {
            return "AI request failed: " + ex.getStatusCode().value() + " " + ex.getStatusText();
        } catch (Exception ex) {
            return "Failed to get AI suggestion: " + ex.getMessage();
        }
    }

    private String buildPrompt(String title, String bodyPlainText) {
        return """
            You are a helpful StackOverflow assistant.
            Respond ONLY in GitHub-Flavored Markdown (no HTML tags).
            Requirements:
            - A brief summary of the issue.
            - Likely cause(s).
            - Concrete, step-by-step fixes.
            - Use fenced code blocks with an explicit language (e.g., ```java, ```js) for any code.
            - Keep it concise and actionable.

            Question Title: %s

            Question Details:
            %s
            """.formatted(title == null ? "" : title.trim(),
                bodyPlainText == null ? "" : bodyPlainText.trim());
    }
}
