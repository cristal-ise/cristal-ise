# REST API for CRISTAL [![Build Status](https://travis-ci.org/cristal-ise/restapi.svg?branch=master)](https://travis-ci.org/cristal-ise/restapi)

This is the start of a JAX-RS wrapper for the CRISTAL Client API as specified on the [Google Doc](https://docs.google.com/document/d/1jAJyETl-iFbNXvrWa7FJLGi4mu1vEmAZNGlKlQ1I4X8/edit?usp=sharing)

The included Main may be invoked as a CRISTAL Client with -conf and -connect parameters, and will launch a Jersey http server on the URI specified in the CRISTAL property 'REST.URI', which defaults to http://localhost:8081/.
