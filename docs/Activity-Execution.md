# Activity Execution
- For each item an `ItemImplementation` instance is created in the memory.
- When a CORBA call is received, Cristal finds the `ItemImplementation` object for the item and calls `delegatedAction` on it

***
[`ItemImplementation.delegatedAction()`](https://github.com/cristal-ise/kernel/blob/196706c46e7b76c1cb24d4f87b523760c071c042/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L226) is invoked for all actions and predefined steps <br/>
The original idea behind `delegated` was that if an authorized agent goes on vacation, he delegates his duties to someone else.

   1. get the Workflow of the item
   1. [`Workflow.requestAction()`](https://github.com/cristal-ise/kernel/blob/618fe12abc80e1db5482843d78c590966608665c/src/main/java/org/cristalise/kernel/lifecycle/instance/Workflow.java#L128)
      1. find the Activity (vertex)
      1. call `Activity.request()` using itself as a **`locker`** object
      1. [`Activity.request()`](https://github.com/cristal-ise/kernel/blob/d3cc8fd92607e097dd934e4ed9b674cc36cecdd2/src/main/java/org/cristalise/kernel/lifecycle/instance/Activity.java#L172)
         1. find requested Transition
         1. check authorization
         1. check that outcome was given if needed
         1. get new state
         1. [`Activity.runActivityLogic`](https://github.com/cristal-ise/kernel/blob/d3cc8fd92607e097dd934e4ed9b674cc36cecdd2/src/main/java/org/cristalise/kernel/lifecycle/instance/Activity.java#L334)
            * run extra logic in predefined steps (overridden method in predefined steps)
         1. set new state and reservation
         1. unmarshal Outcome
         1. `History.addEvent()` -> [`RemoteMap.put()`](https://github.com/cristal-ise/kernel/blob/develop/src/main/java/org/cristalise/kernel/persistency/RemoteMap.java#L285) called for the `event`
            * [`TransactionManager.put()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L175)
               * caches the `put()` call (as a "pending transaction"), but only in an internal cache, does not invoke the `ClusterStorage`
               * calls are grouped by the **`locker`** object (basically used as a transaction ID)
         1. `Gateway.getStorage().put()` called for the `outcome`
            * [`TransactionManager.put()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L175)
         1. `Gateway.getStorage().put()` called for the `attachment`
         1. `Gateway.getStorage().put()` called for the `viewpoint`
         1. `Gateway.getStorage().put()` called for the `last` `viewpoint`
         1. `Activity.updateItemProperties()`
         1. `runNext`
         1. `pushJobsToAgents`
   1. store the new workflow if state changed
   1. handle `Erase`
   1. [`TransactionManager.commit()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L282) - called for the Workflow **`locker`** object: commits all the previous `put()` changes
      1. for each "pending transaction"
      1. [`ClusterStorageManager.put()`](https://github.com/cristal-ise/kernel/blob/c49dd8aa8b7b278798a1f7e80c580f6b739ed7f8/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#L328)
         1. call `put()` on all ClusterStorages
         1. [`JooqClusterStorage.put()`](https://github.com/cristal-ise/jooqdb/blob/1cb92d738c6b711250302ea8ecab9e38e6d2f14c/src/main/java/org/cristalise/storage/jooqdb/JooqClusterStorage.java#L268)
            * calls `JooqHandler.put()` on the `JooqHandler` corresponding to the `ClusterType`
            * calls `DomainHandler.put()` on all registered domain handlers to update domain specific tables - this sees ALL the changes done with the same **`locker`** object (i.e. in the same transaction) because of [the get() implementation](#the-get-implementation)
