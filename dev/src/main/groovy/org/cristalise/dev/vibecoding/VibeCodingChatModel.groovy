package org.cristalise.dev.vibecoding

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.mistralai.MistralAiChatModel
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import lombok.Builder
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.process.Gateway

@CompileStatic
@Slf4j
class VibeCodingChatModel {

    private final ChatModel model

    @Builder
    VibeCodingChatModel(String apiKey, String modelName, Double temperature, Integer maxTokens) {
        def builder = MistralAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName != null ? modelName : "mistral-small-latest")
 
        if (temperature != null) {
            builder.temperature(temperature)
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens)
        }

        this.model = builder.build()
    }

    /**
     * Constructor for testing.
     */
    VibeCodingChatModel(ChatModel model) {
        this.model = model
    }

    /**
     * Sends a message to the Mistral AI model and returns the response.
     *
     * @param message the message to send
     * @return the model's response
     */
    String chat(String message) {
        log.debug("Sending message to Mistral AI: {}", message)
        return model.chat(message)
    }
}
