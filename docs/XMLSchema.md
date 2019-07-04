CRISTAL uses a subset of W3C standard XML Schema as its Data Definition Language. Every Outcome must be valid according to a named schema version, stored in an Outcome description Item. 

There are two important constraints that CRISTAL imposed on XML Schema:

1. Defining namespaces is not supported for domain schemas.
1. Including other schemas is not supported

These restrictions are a consequence of storing and versioning the schemas inside Items.

