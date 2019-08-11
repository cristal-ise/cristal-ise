CRISTAL entities are CORBA objects that could be located in different server processes. They are all registered in a directory which allows client processes to find a particular Item or Agent and communicate with it. The directory consists of two trees:

 * Entity - this is the index of all [Item](../Item)s and [Agent](../Agent)s, indexed by their [SystemKey](../SystemKey) (an integer ID). [Properties](../Properties) are also stored here for faster browsing and enhanced searching. Each Item and Agent appears only once in this tree.
 * Domain - this is the user-visible index of all Items and Agents. They are indexed by the Property 'Name' into a tree. There is a 'desc' sub-tree containing all descriptions, and an 'agents' tree containing the users by role, but domain Item instances should be collected in their own subtrees. Branches of this tree are called 'CRISTAL contexts' and may be nested as deeply as the directory server allows. Items are aliased to this tree - they must already exist in the Entity tree. Items may appear in the Domain tree more than once.

Client processes use the directory by using Path objects, once the [Gateway](../Gateway) has been initialized and an Agent has been authenticated. A [DomainPath](../DomainPath) may be created with a forward-slashed path as a string parameter, and an [ItemPath](../ItemPath) is created with the system key. DomainPaths can either be contexts or aliases for Items. In the latter case the DomainPath can look up and return the associated ItemPath of its Item. Context DomainPaths can search for named Items in their subtree.

## Obtaining a Path object

At their simplest, when the full path of an Item in the directory is know, Paths may be instantiated directly:

```java
DomainPath myWorkflowPath = new DomainPath("/desc/ActivityDesc/MyWorkflowDef");
```

or by system key:


```java
ItemPath myItemPath = new ItemPath(myUUID);
```

If the path or system key of the Item is not known, then Paths can be found using the various search() methods of the [Lookup](../Lookup)

If a DomainPath is an alias for an Item, then its ItemPath can be resolved:

```java
DomainPath myWorkflowPath = new DomainPath("/desc/ActivityDesc/MyWorkflowDef");
if (myWorkflowPath.getType() == Path.ENTITY) {
	EntityPath wfItemPath = myWorkflowPath.getEntity();
}
```

## Getting Proxies from Paths

An [ItemProxy](../ItemProxy) or [AgentProxy](../AgentProxy) can be obtained from the Gateway [ProxyManager](../ProxyManager) singleton from either an ItemPath or a DomainPath that is an alias.

```java
ItemProxy myProxy = (ItemProxy)Gateway.getProxyManager().getProxy(new DomainPath("/desc/ActivityDesc/MyWorkflowDef"));
```

AgentProxy contains utility methods for quickly resolving Items, to make scripting easier:

```java	
// by Path
agent.getItem("/desc/ActivityDesc/MyWorkflowDef");

// by system key
agent.getItem(myUUID);

// by unique name in the whole directory tree
agent.searchItem("MyWorkflowDef");
```