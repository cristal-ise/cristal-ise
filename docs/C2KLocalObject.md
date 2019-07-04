Every persistent object that can be stored in the context of an item implements the interface C2KLocalObject (historically representing 'CRISTAL 2 Kernel Local Object'). They must be serializable to XML using CASTOR for storage and transmission. They are identified by a Name, and declare which [[ClusterStorage]] type they implement. Not all C2KLocalObjects are stored explicitly in Item Clusters, but are instead only store in Outcomes. These objects return null as their cluster type.

Features:

 * Marshalling as XML and unmarshalling from XML back to the original object using a [[CastorXMLUtility]]. Mapfiles can be supplied for use in modules in object storage in [[Outcome]]s.
 * Persistency. All objects handled by the [[ClusterStorageManager]]. 
 * Data transmission. Returned by:
   * queryData method of [[Item]]
   * getObject method of [[ItemProxy]]
   * [[EntityProxyObserver]] subscriptions.

## Kernel C2KLocalObjects

Aside from its [[SystemKey]] and [[DomainPath]]s, all Item and Agent structure and function are held in C2KLocalObjects. This is a key feature of their described nature. 

[[Item]]

 * [[Property]]
 * [[Collection]]
 * [[Workflow]]
 * [[Event]]
 * [[Outcome]]
 * [[Viewpoint]]

[[Agent]]

 * [[Job]]

Description objects using C2KLocalObject
 
 * [[ElementaryActivityDef]]
 * [[CompositeActivityDef]]
 * [[CollectionDescription]]

C2KLocalObject is also implemented by [[RemoteMap]], so that it can be returned by the ClusterStorageManager to provide History and JobList objects.
