CRISTAL-iSE framework uses CORBA IDL technology to call distributed objects, Java object on different platforms across the network. CRISTAL-iSE framework uses Common Object Request Brokerage Architecture (CORBA) for client and server communications, a key feature of **CORBA** is **IDL** (Interface Definition Language).

## IDL - Java Language Mapping

In CRISTAL-iSE, we have idl files that can be found in [`cristal-ise/idl/src/main/idl`](https://github.com/cristal-ise/cristal-ise/tree/develop/idl/src/main/idl). When updating a java interface that uses or that is bound with an IDL file, one should update the idl file itself and compile.

**For Example**:

The interface `ItemOperations.java` is linked to an IDL file [`Entity.idl`](https://github.com/cristal-ise/cristal-ise/tree/develop/idl/src/main/idl/Entity.idl) and to modify this interface, we need to change the IDL file and run the `idlj` command for `Entity.idl`. This would then generate the java interface and classes with the updated/added methods.


### Build using maven `idl` profile
Execute maven commands from the project root directory (e.g. `cd ~/workspace/cristal-ise`)

* `mvn generate-sources -P idl -pl idl` - only generate the java classes from idls
* `mvn install -P idl -pl idl` - full build with generation of java classes from idls
* `mvn install -pl idl` - full build (no java generation)

**NOTE:** The `generate-sources` will generate all java classes including the current timestapm in the comment of the file. Unfortunatelly this will actually create a change in all the files, instead of changing only the effected ones.

### Build manually

```bash
idlj -pkgTranslate entity org.cristalise.kernel.entity -pkgTranslate common org.cristalise.kernel.common -fserverTie -fallTie Entity.idl
```

## References - idlj
- [https://docs.oracle.com/javase/7/docs/technotes/tools/share/idlj.html](https://docs.oracle.com/javase/7/docs/technotes/tools/share/idlj.html)
- [https://docs.oracle.com/javase/8/docs/technotes/guides/idl/GShome.html](https://docs.oracle.com/javase/8/docs/technotes/guides/idl/GShome.html)
