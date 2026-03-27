package com.company.finance.agent;

import kd.ai.nova.chat.ModelOptions;
import kd.ai.nova.core.model.chat.ChatModel;
import kd.ai.nova.core.model.provider.openai.OpenAiChatModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI-Nova ChatModel 和 ModelOptions 配置
 */
@Configuration
public class ChatModelConfig {

    @Value("${ai.model.base-url:https://ai-nova.company.com/api}")
    private String baseUrl;

    @Value("${ai.model.api-key:your-api-key}")
    private String apiKey;

    @Value("${ai.model.name:deepseek-chat}")
    private String modelName;

    @Value("${ai.model.temperature:0.7}")
    private Double temperature;

    @Value("${ai.model.max-tokens:2048}")
    private Integer maxTokens;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    @Bean
    public ModelOptions modelOptions() {
        return ModelOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
}
