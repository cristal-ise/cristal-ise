The [Properties](../Property) of an Item are defined in its [ItemDescription](../ItemDescription) in a PropertyDescription outcome. Each Property description defines the following:

 * The name of the Property
 * Its default value on creation, if any
 * Whether the property is read-only after Item initialization, i.e. whether the WriteProperty [predefined step](../PredefinedStep) may write to it.
 * Whether the property is a class identifier of the Item type. Items with these properties set to their default values are considered to be of the type defined by this PropertyDescription.
 * Whether the property is transitive or not. Class identifiers are transitive by default. A transitive property is instantiated when a [CollectionDescription](../CollectionDescription) is instantiated.

If the property 'Name' is not defined by the Property description, then it is automatically included as a writable property that is not a class identifier and not transitive.

### PropertyDesription Instantiation

PropertyDescriptions are instantiated in 2 cases:

1. When an Item is instantiated by its description Item (e.g. factory Item), it calls the CreateItemFromDescription or CreateAgentFromDecription [PredefinedStep](../PredefinedStep). The description Item reads the specified version of its own PropertyDescription outcome and copies the PropertyDescriptions into the properties of the newly created Item.

2. Transitive and Class identifier properties play and important role instantiating [CollectionDescription](../CollectionDescription)