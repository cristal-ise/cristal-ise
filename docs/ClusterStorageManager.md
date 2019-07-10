The Cluster Storage Manager handles CRISTAL object persistency. Class names implementing the [ClusterStorage](../ClusterStorage) interface are supplied via system properties, and are instantiated at startup. All objects are loaded through this, even on CRISTAL clients: CORBA calls to load data from remote server Items are wrapped up behind storage manager calls, so that remote database reading storages may be used instead if available

## TransactionManager

In a live CRISTAL process, the ClusterStorageManager is wrapped up in the TransactionManager, which enables writes to be collected in isolation and either rolled back or written atomically to the databases. It uses locker objects to identify each transaction, which may also be passed to the read methods to use a view on the database as if the commit had already taken place.

The TransactionManager and the ClusterStorageManager are singletons within a CRISTAL process, and are accessible as static members of the Gateway class.