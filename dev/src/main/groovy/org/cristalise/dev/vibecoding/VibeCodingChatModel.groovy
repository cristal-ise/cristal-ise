/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dev.vibecoding

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.listener.ChatModelListener
import dev.langchain4j.model.chat.listener.ChatModelResponseContext
import dev.langchain4j.model.mistralai.MistralAiChatModel
import dev.langchain4j.model.mistralai.MistralAiChatModel.MistralAiChatModelBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.cristalise.kernel.process.Gateway

import static org.cristalise.kernel.persistency.outcomebuilder.utils.OutcomeUtils.getBigDecimalOrNull
import static org.cristalise.kernel.persistency.outcomebuilder.utils.OutcomeUtils.getBigIntegerOrNull

@CompileStatic
@Slf4j
class VibeCodingChatModel implements ChatModelListener {

    private static volatile VibeCodingChatModel instance

    private static final String LLM_CONNECTIONS_PATH = '/vibecoding/LlmConnections/'
    private static final String VIEWPOINT_SCHEMA = 'LlmConnection'
    private static final String MISTRAL_AI_API_KEY_ENV_VAR = 'MISTRAL_AI_API_KEY'
    private static final String DEFAULT_MODEL_FAMILY = 'Mistral'
    private static final String DEFAULT_MODEL_NAME = 'mistral-small-latest'

    private final String llmFamily
    private final ChatModel model

    @Override
    void onResponse(ChatModelResponseContext responseContext) {
        super.onResponse(responseContext)
        log.info("onResponse() - metadata:{}", responseContext.chatResponse().metadata())
    }

    static VibeCodingChatModel getInstance() {
        return getInstance(DEFAULT_MODEL_FAMILY)
    }

    static synchronized VibeCodingChatModel getInstance(String chatFamily) {
        if (instance == null) {
            instance = new VibeCodingChatModel(chatFamily)
        } else if (chatFamily != instance.llmFamily) {
            instance = new VibeCodingChatModel(chatFamily)
        }
        return instance
    }

    protected VibeCodingChatModel(String chatFamily) {
        llmFamily = chatFamily

        def llmConnection = Gateway.getProxy(LLM_CONNECTIONS_PATH + llmFamily)
        def llmConfig = llmConnection.getOutcome(VIEWPOINT_SCHEMA)

        String apiKey = llmConfig.getField('ApiKey') ?: System.getenv(MISTRAL_AI_API_KEY_ENV_VAR)
        String modelName = llmConfig.getField('ModelName') ?: DEFAULT_MODEL_NAME
        Double temperature = getBigDecimalOrNull(llmConfig, 'Temperature')?.toDouble()
        Integer maxTokens = getBigIntegerOrNull(llmConfig, 'MaxTokens')?.toInteger()

        MistralAiChatModelBuilder builder = MistralAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName != null ? modelName : DEFAULT_MODEL_NAME)
                .listeners(List.<ChatModelListener>of(this))

        if (temperature != null) builder.temperature(temperature)
        if (maxTokens != null) builder.maxTokens(maxTokens)

        this.model = builder.build()
    }

    /**
     * Constructor for testing.
     */
    protected VibeCodingChatModel(ChatModel model) {
        this.llmFamily = 'UNKNOWN'
        this.model = model
    }

    /**
     * Sends a message to the AI model and returns the response.
     *
     * @param message the message to send
     * @return the model's response
     */
    String chat(String message) {
        log.debug("chat() - message:{}", message)
        return model.chat(message)
    }
}
