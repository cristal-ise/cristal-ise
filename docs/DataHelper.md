Data helpers are a mechanism that allows easy referencing of different types of data within an Item, in order to use that data for process control or generating new data. They are referenced using a URI-like syntax, usually in workflow vertex properties.

In Activities and other script supporting workflow vertices, these URLs are stored at values of activity properties, which are then referenced as input parameters to the associated script. CRISTAL will resolve those values and insert the results, using the activity property names as variable names, into the script execution environment. 

Supported data helpers as of CRISTAL kernel 3.2:

* [Viewpoint](../Viewpoint): viewpoint//schema/viewName:XPath
* [Property](../Property): property//propertyName
* [Activity](../Activity): activity//pathToActivity:XPath - activity path may be relative.

As many state machine parameters may be delegated to activity properties, data helpers may also be used in those cases.