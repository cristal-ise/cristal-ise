import org.cristalise.kernel.collection.Collection

import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*

Module(name: 'CristaliseDev', namespace: 'dev', rootPackage: 'org.cristalise.dev.vibecoding', generateProperty: true, generateModule: false, webuiConfigs: false) {

    Item(name: 'LlmConnection') {
        field(name: 'Description')
        field(name: 'ModelFamily', values: ['Gemini', 'GPT', 'Llama', 'Mistral', 'Claude'])
        field(name: 'ModelName')
        field(name: 'ApiKey')
        field(name: 'Temperature', type: 'decimal', documentation: 'Low value for strict, predictable data parsing')
        field(name: 'MaxOutputTokens', type: 'integer', documentation: 'Budget limit cap')
        field(name: 'Timeout', type: 'integer', documentation: 'Prevents thread blocking')
        field(name: 'SystemInstruction')
        field(name: 'AllowCodeExecution', type: 'boolean', documentation: 'Let it compute mathematical formulas natively')
        field(name: 'LogRequests', type: 'boolean', documentation: 'Useful for local debug logs')
        field(name: 'LogResponses', type: 'boolean')
    }

    Item(name: 'VibeConversation') {
        field(name: 'Description')

        dependency(to: 'LlmConnection', type: Bidirectional, cardinality: ManyToOne)
    }

    Agent(name: 'VibeCoder') {
        field(name: 'FistName')
        field(name: 'LastName')
        field(name: 'Email')

        dependency(to: 'VibeConversation', type: Bidirectional, cardinality: ManyToMany)
    }
}
