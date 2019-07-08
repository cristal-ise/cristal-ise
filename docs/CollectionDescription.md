A collection description is a special type of [[Collection]] that defines collection instances to be created in Item instances. Collection members reference [[ItemDescription|Item descriptions]], which define the types of Item instances which may fit in the instantiated collection. [[Dependency]] descriptions only reference one Item Description, whereas [[Aggregation]] descriptions can contain many members, each defining the type of one slot in the Aggregation instance.

### Collection Description Instantation 

When a collection description is instantiated by the CreateItemFromDescription or CreateAgentFromDecription [[PredefinedStep]], it reads the [[PropertyDescription]] outcome in the referenced Item(s) and copies the transitive [[PropertyDescription]] into the properties of the newly created collection. This is mechanism is required for the implementation of 'typed' collections.