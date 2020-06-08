An ItemPath represents an Item as represented in the directory as a system key that points to a object on the server. It can be used to obtain an ItemProxy to interact with that Item through the [ProxyManager](../ProxyManager).

Internally, ItemPaths are handled and generated as Class 4 UUIDs and over CORBA as a SystemKey structure of two 64-bit integers.