CRISTAL-iSE framework uses Java IDL technology to bind distributed objects, Java object on different platforms across the network. CRISTAL-iSE framework uses Common Object Request Brokerage Architecture (CORBA) for client and server communications, a key feature of **CORBA** is **IDL** (Interface Definition Language).

# IDL - Java Language Mapping

In Cristal-ise, we have defined idl object that can be found in [`CRISTAL-iSEidl`](https://github.com/cristal-ise/cristal-ise/tree/develop/idl/src/main/idl). When updating a java interface in that uses or that is binded
with an IDL file, one should update the idl object itself and compile. <br/>

In Example:  <br/> <br/>
The interface `ItemOperations.java` is linked to an IDL file `(Entity.idl)` and to modify this interface, we need to change the IDL file and run the following command  <br/>

```
 > idlj Entity.idl
```

This would then generate the java interface and classes with the updated/added method or function. <br/> <br/>

Options can be added as such when compiling idl files. <br/>

```
 > idlj  -pkgTranslate entity org.cristalise.kernel.entity -pkgTranslate common org.cristalise.kernel.common  -fserverTie -fallTie Entity.idl
```

# References
- [https://docs.oracle.com/javase/7/docs/technotes/tools/share/idlj.html](https://docs.oracle.com/javase/7/docs/technotes/tools/share/idlj.html)
- [https://docs.oracle.com/javase/8/docs/technotes/guides/idl/GShome.html](https://docs.oracle.com/javase/8/docs/technotes/guides/idl/GShome.html)
