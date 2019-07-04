NOTE: This page describes Item properties, which are distinct from [[ActivityProperties]] and [[CollectionProperties]]

A Property is a name/value string pair associated with a CRISTAL Item or Agent. They are used for identification and typing. Each Item is instantiated with a set of Properties created from the [[PropertyDescription]] outcome of its [[Description|ItemDescription]]. Default values can be specified which give their initial values.

Properties are important for typed collections. If the PropertyDescription outcome tags a particular Property as being a type identifier, then Items must have that Property set to its declared default value to be considered an Item of that type. The PropertyDescription also declares whether a Property can have its value changed once it has been instantiated, although this is only enforced in the 3.0 version of the kernel. Multiple Properties can be type identifiers, which allows for subtyping: each subtype adds another property to the type identifiers.

The Property 'Name' is special. It is the 'domain key' of the Item, and the last part of any [[DomainPath]]s the Item has.

On Activity execution, a list of Properties could be updates. An Activity may specify a list of 'ItemProperty.${name}' properties to update the Property specified by the `${name}` string and its value can be one of the following cases:

* value to be updated
* xpath expression to select a value from the actual Outcome
    * example: xpath:/TestOutcome/StrinValue