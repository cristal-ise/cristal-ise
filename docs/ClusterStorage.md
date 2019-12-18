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


# The sequence of get() implementation
For example, if a `DomainHandler` executes a script (for example aggregate script) which reads a viewpoint (details schema):
[`Script.evaluate()`](https://github.com/cristal-ise/kernel/blob/56e221a176dd9c9330bb286b41aba10494353662/src/main/java/org/cristalise/kernel/scripting/Script.java#L510)

   * get the ItemProxy
   * set the **`locker`** as a **`transactionKey`** on the `ItemProxy`
   * `execute()`
   * then in the script, for example: <br/>
[`ItemProxy.getViewpoint()`](https://github.com/cristal-ise/kernel/blob/7845981def21deef7a2a0a0180d13b7bbffb91fe/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L572)
      * `locker == null ?` **`transactionKey`** `: locker`
      * [`ItemProxy.getObject()`](https://github.com/cristal-ise/kernel/blob/7845981def21deef7a2a0a0180d13b7bbffb91fe/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L1062)
         * `Gateway.getStorage().get()` =
[`TransactionManager.get()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L144)
            * HISTORY and JOB `ClusterType`s are handled in a special way
            * _if_ this **`locker`** has been modifying this `itemPath`, **read the object from the cache**
            * _else_ read the object from the `ClusterStorage` using [`ClusterStorageManager.get()`](https://github.com/cristal-ise/kernel/blob/c49dd8aa8b7b278798a1f7e80c580f6b739ed7f8/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#L258)