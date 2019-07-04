Collections are Item objects that declare relationships between Items. Each collection has a name, and holds properties. Each Item is referenced by a **Slot** object, which holds additional properties about the link. An Item assigned to a Slot is referred to as a **Member** of the collection.

There are two types of collection in CRISTAL-iSE kernel: [[Dependency]] and [[Aggregation]]. Each may also be a [[CollectionDescription]], which are used in [[Item descriptions|ItemDescription]] to define instance collections.

### Slots and Typing
Collection membership may be restricted to certain types of Item, using the class identifying Properties defined in an Item description's [[PropertyDescription]] outcome. On instantiation, the collection (or member slot) stores a list of the names of Item properties that are class identifiers, and copies the required property names and values into its own properties. Assignment to that collection will require that the class identifying properties of the requested child Item must match the values stored. This type information is stored without any dependency on the Item description it was derived from, and functions purely by comparing its stored data to the requested Item's properties. Thus this information may be manipulated in domain applications, and an Item will 'match' any type it has the correct properties for. This makes CRISTAL-iSE typing a 'duck typing' mechanism, and makes it easy to implement such mechanisms as sub-typing (using more than one class identifier, adding one for each subtype) and abstract types (Item descriptions with a [[PropertyDescription]] by no workflow or Instantiate activity defined)

### Manipulation

Collections may be created and managed through [[PredefinedStep]]s, called by Activity Scripts or directly through the Java Client API. Collection descriptions are created in Item descriptions, and both collections and their descriptions may have members and/or slots added. It is not possible to create a Collection instance this way: all collections should be derived from a description. Collections of any type may be imported in [[Modules]]

#### Creation

 * CreateNewCollectionDescription

#### Dependency

##### Assignment:
 * AddMemberToCollection
 * RemoveSlotFromCollection

#### Aggregation

##### Slots:

 * AddNewSlot
 * RemoveSlotFromCollection

##### Assignment:

 * AssignItemToSlot
 * ClearSlot

 
