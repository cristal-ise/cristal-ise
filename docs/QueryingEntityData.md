## The Item object model

Data is organized in Items in a tree-like structure under classifying contexts (known as Clusters). Each leaf is an instance of the interface [C2KLocalObject](../C2KLocalObject). The first level organizes the objects by their type (Property, Workflow, Event etc), and below this some types have a further substructure.

See [ClusterStorage](../ClusterStorage)

Item and Agent data can be accessed through client proxy objects or directly from ClusterStorage through the TransactionManager. Proxies can be created from a [Path](../Path) object, using the [EntityProxyManager](../EntityProxyManager) singleton in the [Gateway](../Gateway).

To explore this data, the EntityProxy.getContents method may be used, or the TransactionManager.getClusterContents may be queried directly. Objects may be retrieved from the proxy using the EntityProxy.getObject method, from storage with TransactionManager.get, or using the underlying direct queryData method of the Item which returns on the XML marshalled forms of the objects from the server.

Client processes should be and usually are restricted to read-only access to ClusterStorage providers, so may not write directly. All writing is done through Activity execution. Normal domain activity execution will result in a new Event and a change of state in the Item lifecycle. When required, Outcomes and Viewpoints will be written too. All other manipulations of Item data is achieved through the [PredefinedStep](../PredefinedStep)s - special Activities that run extra logic in the server when they are executed. No other direct manipulation is possible, so all client actions are traced.

```java
e.g. Getting every property of an Item:

// resolve the path
DomainPath path= new DomainPath("/desc/dev/ScriptFactory");

// get the item proxy
ItemProxy proxy = (ItemProxy)Gateway.getProxyManager().getProxy(path);

// get the contents of the 'Property' cluster
String[] propNames = proxy.getContents(ClusterStorage.PROPERTY);
for (String propName: propNames) {
	String propVal = proxy.getProperty(propName);
	System.out.println("Property "+propName+" is "+propVal);
}
```