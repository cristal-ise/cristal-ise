Persistency in CRISTAL is implemented using ClusterStorage objects, which are organized using the [ClusterStorageManager](../ClusterStorageManager). Almost every piece of data associated with a CRISTAL Entity (Item or Agent) are stored this way, except for its [Lookup](../Lookup) data ([ItemPath](../ItemPath)s and [DomainPath](../DomainPath)s).

ClusterStorages declare their support for reading and/or writing one or more types of [C2KLocalObject](../C2KLocalObject). Each type is stored under a particular path pattern. The:

| C2KLocalObject type | Storage Path |
|---------------------|--------------|
| [Property](../Property) | /Property/_Name_ |
| [Collection](../Collection) | /Collection/_Collection version_/_Name_ |
| [Workflow](../Workflow) | /LifeCycle/Workflow |
| [Outcome](../Outcome) | /Outcome/_Schema Name_/_Schema Version_/_Event ID_ |
| [OutcomeAttachment](../OutcomeAttachment) | /Attachment/_Schema Name_/_Schema Version_/_Event ID_ |
| [Event](../Event) | /AuditTrail/_Event ID_ |
| [Viewpoint](../Viewpoint) | /ViewPoint/_Schema Name_/_View Name_ |
| [Job](../Job) (_Agents only_) | /Job/_Job ID_ |

ClusterStorage implementations must be able to return an identical object to that passed to its `put()` method when its path is passed to its `get()` method. Fields that should be persisted to achieve this are listed in the classes mapfile, except for Outcomes, for which the storage of the Outcome XML fragment is sufficient.

ClusterStorage is an abstract class in the org.cristalise.kernel.persistency package which must be extended by all ClusterStorages. It defines constants used in ClusterStorage management, String constants for the first path component of each Cluster, and a minimal set of methods to implement:

* `getName()`, `getID()` - return the description and short name of the storage. These values will identify the storage in logs.
* `open()` - initialize the storage at startup
* `close()` - close the storage for shutdown
* `short queryClusterSupport(String clusterType)` - for each C2KLocalObject type supported, the object should return the constants READ, WRITE or READWRITE, depending on what it can do with those objects. For all other strings it should return NONE. The ClusterStorageManager will only call get() on that storage for types reported as READ or READWRITE and put/delete() for those reported as WRITE or READWRITE.
* `C2KLocalObject get(Integer sysKey, String path)` - bring the object stored in the Entity with the given system key, under the given path out of persistency and return it.
* `put(Integer sysKey, C2KLocalObject obj)` - store the given object of the Entity with the given system key under the relevant path.
* `delete(Integer sysKey, Path obj)` - delete the object stored in the Entity with the given system key, under the given path.

There are two methods to support execution of [Query](../Query):

* `boolean checkQuerySupport(String language)` - Checks is the Query is supported by the ClusterStorage implementation.
* `String executeQuery(Query query)` - Perform a query and return the result as an XML. AgenProxy.execute(job) validates the result agains the Schema associated with the Job.
