# Implementation XMLDB module of CRISTAL-iSE kernel using eXistDB

![CRISTAL-iSE_XMLDBDocuments.puml](http://uml.mvnsearch.org/gist/bc7e06fbc53b80cdd848d2a0fd3c3088)

The diagram explains the XML document structure implemented in eXistDB.
  * It is the implementation of the [cristal-ise/kernel/wiki/ClusterStorage](https://github.com/cristal-ise/kernel/wiki/ClusterStorage)
  * Each class represents an xmldb:document, the only excpetion that Item is stored as an xmldb:container. 
  * The name of the class shows how the name of the document can be constructed
  * Public attributes list the data available in different CRISTAL-iSE object (the list is not complete) 

