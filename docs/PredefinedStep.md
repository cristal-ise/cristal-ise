Predefined steps are special subclasses of [[Activity]] that are available in every [[Workflow]] object. The [[CompositeActivity]] that you see in the user interface and which represents the lifecycle of the [[Item]] is not the root of the Workflow: it is a child of it called 'domain'. Each Workflow also contains a second CompositeActivity called 'predefined'. This is a subclass of CompositeActivity, PredefinedStepContainer, which contains these special activities. PredefinedStepContainer is not persistent, so the steps available in a workflow changes with the kernel. That said, the library of predefined steps changes very rarely, and any chance should be considered a new major version of the CRISTAL API.

Each is always active so available for execution, is invoked by its class name, and takes an outcome of the type 'PredefinedStepOutcome', which is a flat list of CDATA elements named 'param'. The number and content of the expected params varies between each one.

The Server predefined step container was introduced in version 2.2, to hold global functions in the Server item that operates on the server in ways not specific to any particular Item, such as description-less Item and Agent instantiation. These have been expanded in version 3.0 to include directory management, including Roles. Also since version 3.0, Agents are also Items with workflows, so they have their own predefined step container too dealing with password and role management.

## Standard Predefined Steps 

| Name | Category | Purpose | Parameters | Since |
|------|----------|---------|------------|-------|
| CreateItemFromDescription | [[Instantiation]] | Attempts to instantiate a new Item using this one as its description. The current Item must contain a PropertyDescription outcome, and a Dependency collection named 'workflow' that contains a Composite Activity Description Item | *1:* New Item name <br> *2:* The parent domain path to create it in<br>*3:*(optional) Description version to use<br>*4:*(optional) Marshalled PropertyArrayList of initial property values | version:2.0 <br>[3.0](/cristal-ise/kernel/releases/tag/3.0) - Param 3 (Issue #212), Param 4 (Issue #215) |
| CreateAgentFromDescription | [[Instantiation]] | Attempts to instantiate a new Agent using this Item as its description. Requires a PropertyDescription Outcome and 'workflow' Dependency collection as for Item creation. | *1:* New Agent name <br>*2:* The parent domain path to create it in<br>*3:*(optional) Description version to use<br>*4:*(optional) Marshalled PropertyArrayList of initial property values | [3.0](/cristal-ise/kernel/releases/tag/3.0) [3.1](/cristal-ise/kernel/releases/tag/3.1) - Change of params to harmonize with CreateItemFromDescription |
| AssignItemToSlot | [[Collection]] | Assigns the referenced entity to a pre-existing slot in a collection of this one | *1:* Collection name <br> *2:* Slot number <br>*3:* Target entity key | version:2.0 |
| AddDomainPath | [[Lookup]] | Adds a new path to this entity in the Lookup domain tree | *1:* The new path | version:2.0 |
| RemoveDomainPath | Lookup | Removes an existing path to this Entity from the LDAP domain tree | *1:* The path to remove | version:2.1 |
| WriteProperty | [[Property]] | Writes an Item property value. The property must already exist and be writable. | *1:* Property name <br>*2:* Property value | version:2.1 <br> [3.0](/cristal-ise/kernel/releases/tag/3.0) - verifies existence and mutability |
| WriteViewpoint | [[Viewpoint]] | Points a Viewpoint to a different event. | *1:* Schema name <br>*2:* View name<br>*3:* new Event id | [3.0](/cristal-ise/kernel/releases/tag/3.0) |
| Import | Storage | Inserts an arbitrary outcome into this Item | *1:* "schema_version:viewpoint" <br>*2:* XML outcome (CDATA)| version:2.1 |
| ClearSlot | Collection | De-assigns any item from the given slot of the named collection. Aggregation collections only. | *1:* Collection name <br>*2:* Slot number | version:2.3.2 (Issue #19) |
| AddMemberToCollection | Dependency | Adds the given item to the named collection in a new slot. Can execute Script defined in the `MemberAddScript` property | *1:* Collection name <br>*2:* UUID or DomainPath of target Item <br>*3:* New slot properties. | version:2.3.2 (Issue #19)<br>[3.0](/cristal-ise/kernel/releases/tag/3.0) - Param 3 (Issue #164) |
| RemoveSlotFromCollection | Collection | Removes a given slot from the collection.  | *1:* Collection name <br>*2:* Slot number (can be empty or _-1_ if *3* is supplied) <br>*3:* (optional) Target entity key  | version:2.3.2 (Issue #19) |
| AddNewSlot | Aggregation | Creates a new slot in the given aggregation, that holds instances of the given item description  | *1:* Collection name <br>*2:* Item Description key (optional) <br>*3:* (optional) Item Description version | version:2.3.2 (Issue #19) <br>[3.0](/cristal-ise/kernel/releases/tag/3.0) - Param 3 (Issue #212)|
| AddNewCollectionDescription | Collection | Creates a new collection description in the current item  | *1:* Collection name <br>*2:* Collection type ('Aggregation' or 'Dependency') | [3.0](/cristal-ise/kernel/releases/tag/3.0) (Issue #209) |
| CreateNewCollectionVersion | Collection | Creates a new collection snapshot version in the current item, with a generated integer ID  | *1:* Collection name | [3.0](/cristal-ise/kernel/releases/tag/3.0) (Issue #212) |

**Collection note:** Dependencies should be managed with AddMemberToCollection & RemoveSlotFromCollection. Aggregation slots assignments are managed with AddItemToSlot and ClearSlot, and Aggregation slots are managed with AddNewSlot & RemoveSlotFromCollection.

## Agent Predefined Steps

These steps are specialized Agent steps, that are only available in the Agent predefined step container

| Name | Category | Purpose | Parameters | Since |
|------|----------|---------|------------|-------|
| SetAgentPassword | [[Agent]] | Replaces this Agent's login password in the lookup | *1:* New password | [3.0](/cristal-ise/kernel/releases/tag/3.0) |
| SetAgentRoles | [[Agent]] | Replaces this Agent's role membership | Each role as a separate parameter | [3.0](/cristal-ise/kernel/releases/tag/3.0) |


## Administrative Predefined Steps

The following operations are available in all Items, but their use is restricted to Agents who hold the 'Admin' role because they can bypass restrictions imposed by other steps, such as the mutable check in 'WriteProperty'. They are intended for special administrative interventions that should still be recorded.


| Name | Category | Purpose | Parameters | Since |
|------|----------|---------|------------|-------|
| ReplaceDomainWorkflow | Workflow | Overwrite the domain workflow with the supplied new version | *1:* The XML marshalled domain CompositeActivity | version:2.0 |
| AddC2KObject | [[Storage]] | Adds or overwrites a C2Kernel object for this Item | *1:* The XML marshalled C2KObject | version:2.0 (Admin only since [3.0](/cristal-ise/kernel/releases/tag/3.0)) |
| RemoveC2KObject | Storage | Removes the named C2Kernel object from this Item | *1:* The local path of the object to remove | version:2.0 (Admin only since [3.0](/cristal-ise/kernel/releases/tag/3.0)) |
| Erase (Item only) | Storage | Deletes all objects and domain paths for this item. | None | version:2.1 |
| RemoveAgent (Agent only) | Storage | Removes this Agent from all Roles, deletes all its objects and removes it from the directory | None | [3.0](/cristal-ise/kernel/releases/tag/3.0) |

## Additional Server Predefined Steps

All Items share identical predefined steps but one: the server Item. This Item is the abstract representation of the CRISTAL server instance, and in addition to the standard set contains two special predefined steps that allow creation of Items and Agents without description so that the server may bootstrap its initial Items. These steps use their own parameter schemas. This is defined in a subclass of PredefinedStepContainer, ServerPredefinedStepContainer.

| Name | Category | Purpose | Schema | Since |
|------|----------|---------|--------|-------|
| CreateNewItem | Instantiation | Creates a complete Item | [Item.xsd](/cristal-ise/kernel/tree/master/src/main/resources/boot/OD/Item.xsd) | version:2.2 |
| CreateNewAgent | Instantiation |Creates a complete Agent | [Agent.xsd](/cristal-ise/kernel/tree/master/src/main/resources/boot/OD/Agent.xsd) | version:2.2 |
| CreateNewRole | Instantiation |Creates a new Role | [Role.xsd](/cristal-ise/kernel/tree/master/src/main/resources/boot/OD/Role.xsd) | [3.0](/cristal-ise/kernel/releases/tag/3.0) |
| AddDomainContext | Lookup |Adds a new context to the domain tree of the lookup | PredefinedStepOutcome: *1:* The new path | [3.0](/cristal-ise/kernel/releases/tag/3.0) |
| RemoveDomainContext | Lookup |Removes a context from the lookup if empty | PredefinedStepOutcome: *1:* The path to remove | [3.0](/cristal-ise/kernel/releases/tag/3.0) |
| RemoveRole | Agent |Removes a Role, if it has no Agents | PredefinedStepOutcome: *1:* The role name | [3.0](/cristal-ise/kernel/releases/tag/3.0) |

## Obsolete Predefined Steps.

The following predefined steps were found in earlier versions of the kernel but have now been removed.

| Name | Category | Purpose | Parameters | Introduced in | Removed since |
|------|----------|---------|------------|---------------|---------------|
| AddStepsFromDescription | [[Workflow]] | Creates the domain workflow from a description | *1:* The name of the CompositeActivityDef to instantiate | version:2.0 | [3.0](/cristal-ise/kernel/releases/tag/3.0) |

## Calling Predefined Steps

These activities cannot be scheduled in the domain workflow of an Item, they must be called via a direct request typicallz from a Script. Two additional `execute` methods exist in [[AgentProxy]] to make calling them easier:

* `execute(ItemProxy item, String predefStep, String... param)` - Creates the PredefinedStepOutcome using `PredefinedStep.bundleData()`, which wraps the params in CDATA tags, then calls the named step.
* `execute(ItemProxy item, String predefStep, C2KLocalObject obj)` - Marshalls the given object, and passes it as a single parameter to the named step.

These can be called from any Cristal client process that has an AgentProxy with which to execute, but they are more often called in [[Script|scripts]] as part of the logic of the execution of a domain activity.