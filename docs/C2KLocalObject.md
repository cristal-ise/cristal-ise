Every persistent object that can be stored in the context of an item implements the interface C2KLocalObject (historically representing 'CRISTAL 2 Kernel Local Object'). They must be serializable to XML using CASTOR for storage and transmission. They are identified by a Name, and declare which [ClusterStorage](../ClusterStorage) type they implement. Not all C2KLocalObjects are stored explicitly in Item Clusters, but are instead only store in Outcomes. These objects return null as their cluster type.

Features:

 * Marshalling as XML and unmarshalling from XML back to the original object using a [CastorXMLUtility](../CastorXMLUtility). Mapfiles can be supplied for use in modules in object storage in [Outcome](../Outcome)s.
 * Persistency. All objects handled by the [ClusterStorageManager](../ClusterStorageManager). 
 * Data transmission. Returned by:
   * queryData method of [Item](../Item)
   * getObject method of [ItemProxy](../ItemProxy)
   * [EntityProxyObserver](../EntityProxyObserver) subscriptions.

## Kernel C2KLocalObjects

Aside from its [SystemKey](../SystemKey) and [DomainPath](../DomainPath)s, all Item and Agent structure and function are held in C2KLocalObjects. This is a key feature of their described nature. 

[Item](../Item)

 * [Property](../Property)
 * [Collection](../Collection)
 * [Workflow](../Workflow)
 * [Event](../Event)
 * [Outcome](../Outcome)
 * [Viewpoint](../Viewpoint)

[Agent](../Agent)

 * [Job](../Job)

Description objects using C2KLocalObject
 
 * [ElementaryActivityDef](../ElementaryActivityDef)
 * [CompositeActivityDef](../CompositeActivityDef)
 * [CollectionDescription](../CollectionDescription)

C2KLocalObject is also implemented by [RemoteMap](../RemoteMap), so that it can be returned by the ClusterStorageManager to provide History and JobList objects.
