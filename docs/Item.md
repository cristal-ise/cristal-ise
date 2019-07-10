Items are the Objects of CRISTAL. They are workflow driven, fully traceable contexts, which can be used to model application data. Their behaviour and data structures are dictated by data, which we call descriptions. That data is stored in other Items. Some description items store Workflow and Activity descriptions that define their lifecycles, Data structure descriptions (using XML Schema), or Activity logic descriptions (Scripts). Others reference those, and additionally define the identifying properties of new Items, and their relationships to other Items though Collections. The latter type, called Item Descriptions, can be used to create new Items. Items are passive entities - they never perform any of their Activities themselves. They require interaction from [Agent](../Agents) to advance their lifecycles.

Items contain:

 * *[Properties](../Property)* - to identify them individually and as a particular type, as well as to hold key indicators.
 * *[Collections](../Collection)* - to link Items to other Items, storing information about each link in CollectionSlots.
 * *[Workflow](../Workflow)* - to define the Item's lifecycle, defining in Activities what Agents should do and what data they need to supply.
 * *[Events](../Event)* - a full record of every interaction of Agents with their Activities.
 * *[Outcomes](../Outcome)* - data supplied by Agents when completing Activities. Pieces of XML defined by [XMLSchema](../XMLSchema).
 * *[Viewpoints](../Viewpoint)* - pointers to current Outcomes, similar to variables.

