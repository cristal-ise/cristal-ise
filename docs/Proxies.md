## The Proxy API

Proxies are client-side representation of Items and Agent. They manage caching, subscriptions, essential client-side functionality and are intended to be the core of the CRISTAL Client API. Activity Scripts have their relevant Agent and Item proxies already set in their environment when they run, so along with the Job they are the primary entry point of the API. Agent proxies run those scripts as they execute Jobs. As the API has developed, convenience methods have been added to the proxy objects that do not directly reflect the functionality of the Agent and Item objects, but provide useful shortcuts for things Agents and Items need to do.

Proxies provide:
* Data browsing and retrieval. Caching of objects that automatically expires when changes are made on the server.
* Querying of Item lifecycles for Jobs on demand.
* Execution API for Jobs, validating outcomes and executing activity scripts.
* Convenience methods for locating other Items and Castor XML marshalling and unmarshalling of events.

### Caching

Caching is totally transparent for the user. Cache consistency is maintained by the 'Proxy Subscription' described bellow, and can be disabled setting `Storage.disableCache=true` in the [[configuration properties|CRISTALProperties]]

## Proxy Subscription

Proxies also provide Publish/subscribe functionality to automatically load and push data and subsequent changes, focused on a specific subtree of an Item or Agent. Automatic Agents such as usercode processes subscribe to their own Joblist clusters, so automatically receive new Jobs that are assigned to them. Interested classes must implement the EntityProxyObserver interface, and subscribe to the proxy with a MemberSubscription object detailing the subscription. Then they will receive new objects through a callback mechanism.

### Preload

MemberSubscriptions are configured with a preload flag which indicates whether the proxy should first push all existing objects of interest to the EntityProxyObserver before waiting to push updated objects. This preload occurs in a separate thread to that which created the subscription.

e.g. to load all properties and subscribe to all Property changes in an Item

```java
// resolve the path
DomainPath path= new DomainPath("/desc/dev/ScriptFactory");
// get the item proxy
ItemProxy proxy = (ItemProxy)Gateway.getProxyManager().getProxy(path);
proxy.subscribe(new MemberSubscription<Property>(this, ClusterStorage.PROPERTY, true));

..

public void add(Property prop) {
	//process new or updated property
}

public void remove(String id) {
	//process removed property
}
```