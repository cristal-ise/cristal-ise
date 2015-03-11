# restapi
REST API for CRISTAL

This is the start of a JAX-RS wrapper for the CRISTAL Client API. So far there's no authentication, and only GET is supported for Item UUIDs to retrieve Item objects.

The included Main may be invoked as a CRISTAL Client with -conf and -connect parameters, and will launch a Jersey http server on port 8081.
