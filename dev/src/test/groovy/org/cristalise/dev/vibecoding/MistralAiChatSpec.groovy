package org.cristalise.dev.vibecoding

import dev.langchain4j.model.chat.ChatModel
import spock.lang.Specification

class MistralAiChatSpec extends Specification {

    def "should send message to chat model"() {
        given:
        ChatModel mockModel = Mock()
        VibeCodingChatModel mistralAiChat = new VibeCodingChatModel(mockModel)

        when:
        def response = mistralAiChat.chat("Hello")

        then:
        1 * mockModel.chat("Hello") >> "Hi there!"
        response == "Hi there!"
    }

    def "should build MistralAiChat with constructor"() {
        when:
        def mistralAiChat = new VibeCodingChatModel("test-key", "mistral-tiny", 0.7, 100)

        then:
        mistralAiChat != null
    }
}
