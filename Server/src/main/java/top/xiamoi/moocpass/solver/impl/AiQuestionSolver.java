package top.xiamoi.moocpass.solver.impl;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import top.xiamoi.moocpass.entity.UserQuestionConfig;
import top.xiamoi.moocpass.solver.QuestionSolver;
import top.xiamoi.moocpass.util.OkHttpUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 大模型解题策略实现 (基于 OkHttp)
 */
@Slf4j
@Component
public class AiQuestionSolver implements QuestionSolver {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = OkHttpUtil.createUnsafeClientBuilder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    @Override
    public String getProviderCode() {
        return "AI";
    }

    @Override
    public String solve(String question, List<String> options, UserQuestionConfig config) {
        if (config == null) {
            throw new RuntimeException("题库配置为空");
        }

        String baseUrl = config.getAiBaseUrl();
        String apiKey = config.getAiKey();
        String model = config.getAiModel();

        if (StringUtils.hasText(config.getConfigJson())) {
            try {
                JsonNode root = objectMapper.readTree(config.getConfigJson());
                JsonNode aiNode = root.path("AI");
                if (!aiNode.isMissingNode()) {
                    if (StringUtils.hasText(aiNode.path("baseUrl").asText())) {
                        baseUrl = aiNode.path("baseUrl").asText();
                    } else if (StringUtils.hasText(aiNode.path("endpoint").asText())) {
                        baseUrl = aiNode.path("endpoint").asText();
                    }
                    if (StringUtils.hasText(aiNode.path("key").asText())) apiKey = aiNode.path("key").asText();
                    if (StringUtils.hasText(aiNode.path("model").asText())) model = aiNode.path("model").asText();
                }
            } catch (Exception ignored) {}
        }

        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            throw new RuntimeException("AI 题库配置未完成，请设置 AI Base URL 和 API Key");
        }

        if (!baseUrl.endsWith("/chat/completions")) {
            if (baseUrl.endsWith("/")) {
                baseUrl += "chat/completions";
            } else {
                baseUrl += "/chat/completions";
            }
        }

        if (!StringUtils.hasText(model)) model = "gpt-3.5-turbo";

        String cleanedQuestion = question != null ? question.replaceAll("_{3,}", "___") : "";
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("问题：").append(cleanedQuestion).append("\n");
        if (options != null && !options.isEmpty()) {
            promptBuilder.append("选项：\n");
            for (String opt : options) {
                promptBuilder.append(opt).append("\n");
            }
        }

        String systemPrompt = "你是一个网课答题助手，负责精准解答网课题目。\n" +
                "【严格回答规范】：\n" +
                "1. 单选题：只输出单个大写选项字母（如 A）。\n" +
                "2. 多选题：只输出大写选项字母连续组合，绝对不要加逗号、空格或标点（如 ABD）。\n" +
                "3. 判断题：只输出【正确】或【错误】（或 true / false）。\n" +
                "4. 填空题：只输出纯净的答案短语。如果有多个空，答案之间仅用中文逗号【，】分隔。绝对不要包含任何序号（如 1.、①）、换行或多余前缀！\n" +
                "5. 主观简答题：直接输出解答正文，严禁使用 markdown 代码块（```）包裹，不要包含任何多余开场白或结语。\n" +
                "6. 全局要求：只直接输出最终答案，严禁输出任何分析、解释说明或多余修饰！";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", model);
        requestBodyMap.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", promptBuilder.toString())
        ));

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String jsonPayload = objectMapper.writeValueAsString(requestBodyMap);
                RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(baseUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonNode root = objectMapper.readTree(response.body().string());
                        return root.path("choices").get(0).path("message").path("content").asText().trim();
                    } else {
                        log.error("AI 接口调用失败(尝试 {}/2)，状态码：{}", attempt, response.code());
                    }
                }
            } catch (Exception e) {
                log.warn("AI 搜题请求异常(尝试 {}/2)：{}", attempt, e.getMessage());
                if (attempt == 2) {
                    log.error("AI 搜题最终异常：", e);
                }
            }
        }
        return null;
    }
}
