# CRISTAL-iSE Kernel in-memory lookup implementation

In-memory Implementation of lookup interfaces of cristal-ise kernel. Its primary purpose it to enable *functional testing*

## These methods search are unimplemented, they always return an empty result

```java
public Iterator<Path> search(Path start, Property... props)
public Iterator<Path> searchAliases(ItemPath itemPath)
public Iterator<Path> search(Path start, PropertyDescriptionList props)
```