## Server Side Scripting

- This documentation will guide the developers to understand the flow when a script has been executed server side.  

** Class Call **:
	1. [`AgentProxy.execute(job)`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L176)
		1. call executeServerSideScripting(job) [`line 182`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L182)
			
	1. [`AgentProxy.executeServerSideScripting(job)`]	(https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L247)	
		1. (ItemProxy) item.requestActionWithScript(job) [`line 263`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L263) 
	
	1. [`ItemProxy.requestActionWithScript()`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L408)
	
    1. 	[`ItemImplementation.requestActionWithScript()`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L364)
	    1. callScript(item, agent, job) [`line 415`](https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L415)
			1. [`ItemImplementation.callScript()`] [`line 518`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L518)
				1. execute() [`line 526`](https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L526)
				1. ** If the script will call another job, ProcessType will be then set to Server  **
					1. [`AgentProxy.execute(job)`]  (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L176)
						
						1.  callScript(item, job) [`line 194`]  (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L194)
							1. (Script) script.evaluate() [`line 305`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L305)
						1. (ItemProxy) item.requestAction(job) [`line 236`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java#L236)
					
					1. [`Script.evaluate()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/scripting/Script.java#L660)
						1. execute() [`line 647`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/scripting/Script.java#L674)
					
					1. [`ItemProxy.requestAction()`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L303)
						1. item.delegatedAction() [`line 348`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L348)
					
					1. 	[`ItemImplementation.delagatedAction()`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L250)
						1. (Workflow) lifeCycle.requestAction() [`line 278`](https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L278)
	    
		1. (Workflow) lifeCycle.requestAction() [`line 453`](https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L453)
		1. (Transaction Manager) mStorage.commit(lifeCycle) [`line 466`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L466)	
	
	1. [`Workflow.requestAction()`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/lifecycle/instance/Workflow.java#L138)
		1. (Activity) vert).request() [`line 146`] (https://github.com/cristal-ise/cristal-ise/blob/Issue344_Server_side_scripting/kernel/src/main/java/org/cristalise/kernel/lifecycle/instance/Workflow.java#L146)

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
	
    Only relevant item
	1. [`ProxyManager.processMessage()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyManager.java#L125)
		1.  gets relevant ItemProxy from proxyPool [`line 138`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyManager.java#L138)
		1. (ItemProxy) relevant.notify(message) [`line 144`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ProxyManager.java#L144)

	1. [`ItemProxy.notify()`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L1393)
		1. updates subscriptions [`line 1407`] (https://github.com/cristal-ise/cristal-ise/blob/develop/kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L1407)
