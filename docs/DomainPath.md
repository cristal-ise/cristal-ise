DomainPath represents the user, a.k.a domain, defined name of Items. 

An Item must have one or more entries in the domain directory to be able to be located and accessed. A single path in this directory is called a Domain Path. Each path contains the full path from the directory root to the Item, and the last component should correspond to the Item's property 'Name'.

There are some structures in the domain tree which hard-coded by the kernel and populated during boot. They are:

| Path | Contents |
|------|----------|
| /desc / | Root folder of Description Items |
| /desc/ActivityDesc | Folder of [[Activity]] and [[CompositeActivity]] description Items |
| /desc/OutcomeDesc | Folder of [[XMLSchema]] Items |
| /desc/Script | Folder of [[Script]] Items |
| /desc/Query | Folder of [[Query]] Items |
| /desc/StateMachine | Folder of [[StateMachine]] Items |
| /desc/modules | Folder of [[Module]] Items, created to represent each registered module in the server |
| /servers | Folder of [[Server Items|ServerItem]], representing kernel Server instances. |