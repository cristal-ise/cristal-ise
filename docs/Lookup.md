Items and Agents in CRISTAL are referenced by Path objects that are stored in a network accessible directory. There are two fundamental types of Path:

* [[ItemPath]] - This is the abstract representation of the entity in the directory. It contains, and is identified by, a UUID or 'System Key', but can also include data to help with browsing and searching without accessing the other storages. The default LDAP kernel backend stores Properties so that they can be loaded quickly for UIs to put trees and other lists of Items together quickly.
   * [[AgentPath]]s are specialized ItemPaths which represent CRISTAL agent, and contain an encoded password so users can authenticate against the LDAP server.

* [[DomainPath]] - Completely separate to the entity tree is the domain tree, which is a domain specific organization of the Items and Agents in a tree that is supposed to be meaningful to the users. Items present in this tree as Domain Paths are aliases into the entity tree, so the same Item entry may appear in more than one place. Domain paths do not need to reference Items or Agents - in which case they are contexts (folders) to contain other Domain Paths. The last part of a DomainPath should correspond to the 'Name' property of the Item it refers to.
   * A [[RolePath]] is a specialized DomainPath that references AgentPaths, for future LDAP authorization support.

The LDAP Lookup object is initialized with the [[Gateway]] and is available as a static singleton within it. It manages the CRISTAL process' LDAP connection - which in the case of a server will be connected as the root user, but in a client process will authenticate against a CRISTAL Agent.

## Lookup Path Class Diagram

[[/images/paths.png|Lookup Path Class Diagram]]

[Edit with PlantText](https://www.planttext.com/?text=VLAnJiCm4Dtz5IUpWLpHjIfH4LqwWK2T48CRcyGeYIFx0aAb_yx5IUmKYKxHtRrtxzsBDnwbmwwk6Df2OwaT71ayIInX_IC4FDjAzzMyjqDT_-rjRIrD4xedDW66qNsX6moJD12lfL5ADs-LVaUH9Hac3nwTp14UAkdzgB2UkbdMsmFnWiuq1RI37algm5lfI0-M74YRvwMf55ppcK5O0UTiKloN4041VYP1Kp4QvxUB1J7ZOIkWF2b85g-60I_29zqP6NmPfw7dRAiT6kqP6oA8oFxo8_qlDFa9RiO8dBK8Ty19pU0t6GMuskztM-Wwe_hYyV2XAKvZNP_KR6KK2elFIUFoHjPwLdxAgjDfoooIYzPKmF54d5Auo7842fmyS5AUymSm6aplmVilwnJHDhfHzAn_0G00)