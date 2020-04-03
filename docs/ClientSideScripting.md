## Client Side Scripting

- This documentation will guide the developers to understand the flow when a script has been executed on client side.  

** Class Call **:

	1. [`AgentProxy.execute(job)`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L162)
		1. callScript(item, job) [`line 175`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L175)
			1. (Script) script.evaluate() [`line 253`](https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L253)
		1. (ItemProxy) item.requestAction(job) [`line 211`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L211)
		
	1. [`Script.evaluate()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/scripting/Script.java#L629)
		1. execute() [`line 647`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/scripting/Script.java#L674)
	
    1. [`ItemProxy.requestAction()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L274)
		1. item.delegatedAction() [`line 313`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L313)
	
    1. 	[`ItemImplementation.delagatedAction()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L231)
	    1. (Workflow) lifeCycle.requestAction() [`line 256`](https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L256)
		1. (Transaction Manager) mStorage.commit(lifeCycle) [`line 267`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L267)

	1. [`Workflow.requestAction()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/lifecycle/instance/Workflow.java#L126)
		1. (Activity) vert).request() [`line 134`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/lifecycle/instance/Workflow.java#L134)

    1. [`TransactionManager.commit()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L299)
		1. storage.begin(locker) [`line 307`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L307)
		1. checks each lockerTransactions [`line 309`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L309)
		1. (ClusterStorage) storage.commit(locker) [`line 316`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L316)
		
	1. [`ClusterStorageManager.commit()`]  (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#L497)
		1. (JooqClusterStorage) each clusterStorage calls thisStore.commit(locker) [`line 500`](https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#500)
		1. send proxyEvent for that cluster, calls Gateway.getProxyServer().sendProxyEvent() [`line 506`] https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#506)

** Note **: If the script will execute another job, this will create another CORBA call and will call [`AgentProxy.execute(job)`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L162)

	1. ProxyMessageListener
	1. ProxyServer
	1. [`ProxyServerConnection.run()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyServerConnection.java#L64)
		1. (ProxyManager) manager.processMessage() [`line 78`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyServerConnection.java#L78)
	
    For each item:	
	1. [`ProxyManager.processMessage()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyManager.java#L125)
		1.  gets relevant ItemProxy from proxyPool [`line 138`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyManager.java#L138)
		1. (ItemProxy) relevant.notify(message) [`line 144`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyManager.java#L144)

	1. [`ItemProxy.notify()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L1393)
		1. updates subscriptions [`line 1407`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L1407)
