A Dependency is a [Collection](../Collection) which may references zero-to-many Items (optionally of a specific type). They are the most basic collection type, used for simple referencing Items.

## Dependency used by the kernel

The predefined step CreateItemFromDescription and CreateAgentFromDescription expects a dependency collection called 'workflow' to exist within its Item, containing a single member that points to the Composite activity definition Item that defines its instance workflow. A collection member property named 'Version' indicated the definition version to use.

## Dependency unique member check

By default Dependency will check whether the Item was already added or not. This check can be disabled for Members which are added using properties. Use the following Cristal property to configure the server (default value is true):

`Dependency.checkMemberUniqueness=false`

When members are assigned with properties, it make sense to allow to add the same Item many times, because those properties define the data enabling the distinction among them. Future releases shall implement this feature by configuring [CollectionDescription](../CollectionDescription) and propagate this information during instantiation.

## Instantiation of Dependency between Descriptions

The general rule-of-thumb that a Dependency is instantiated as set of properties. There are 2 different kind of properties in Kernel. ItemProperties and VertexProperties, and depending on the purpose of the Dependency, it is converted to either of these properties. 

If the instantiated Dependency is one of the built-in one (i.e. Activity, Schema, Script, StateMachine), the kernel has code to convert them. For domain specific Dependency the kernel will use the ScriptName and ScriptVersion built-in properties if available in the CollectionMember (similar to RoutingScript of Split), and executes the Script which depending on the context shall produce a list of ItemPropeties or VetexPropeties. There are 2 points when the Kernel instantiates a Dependency during the CreateItemFromDescription:

1. The instantiated Description Item has a Dependency to another Item - converted to **[ItemProperties](../ItemProperties)**. This means that BuiltInCollections (i.e. Activity, Schema, Script, StateMachine) are converted to the BuiltInItemProperties. Domain specific Dependencies can provide a Script as explained earlier.

1. The instantiated WorkflowDesc Item has Dependencies to Script/Schema/SM/Activity/Prefill - converted to **[VertexProperties](../VertexProperties)**. When Workflow Description is instantiated the CompositeActDescs are recursively traversed to instantiate all of their Dependencies (Activities, Schemas, Scripts, StateMachines) by converting them to BuiltInVertexProperties. Domain specific Dependencies can provide a Script as explained earlier.
