#Module XML Format

The module specification file is an XML document that defines the module contents to the kernel. It should be located in the META-INF subdirectory of the module jar, as 'module.xml' in a 'cristal' subdirectory. If you use the Maven archetype, then it will be found in the root of the main resources directory, but copied to the correct place in the jar during build.

Modules specify the following:
 * Module metadata, including version and dependencies
 * Extra CRISTAL properties, to attach included functionality to plugin points
 * CRISTAL description resources
   * Composite Activity definitions
   * Elementary Activity definitions
   * Schemas
   * Scripts
   * State Machines
   * Custom resources defined as [ResourceImportHandler](../ResourceImportHandler) implementations in other modules
 * Full Item 
 * Roles and Agents

##Annotated Module XML

`<CristalModule ns="demo" name="DemoModule">`
> Parent Module XML element, defining the name of the module for reference, and the namespace (ns), which names the sub-contexts in the domain tree in which this module's resources will be stored.

### Info section
The info tag contains module metadata

    <Info>
        <Description>CRISTAL module</Description>

> A human-readable description of the module

        <Version>3.0</Version>
> The current version of the module. In the Maven archetype, this is copied from the POM.

        <Dependency>CristaliseDev</Dependency>
> Defines any modules that this module requires. If the named module is not present, then the server will refuse to start. Module loading is ordered to make sure that a Module's dependencies are loaded before it.

    </Info>
> End of the module metadata

### Kernel configuration
    <ResourceURL>uk/ac/uwe/cristaldev/resources/</ResourceURL>
> Each module bundles a set of text resources containing XML-serialized descriptions, any outcomes to be imported, images to use in the Swing UI and even custom Castor mapfiles if required. These are normally all stored in a unique package in the classpath, but could come from an external server. The Maven archetype derives this setting from the 'Package' parameter, and its initial POM will copy the contents of the src/main/resources directory here (except for the module.xml itself, which goes in META-INF/cristal)

    <Config name="ResourceImportHandler.DEMO" target="server">org.cristalise.demo.DemoImportHandler</Config>
> Any CRISTAL properties to configure this module, or the kernel's use of it. These values are the first loaded, so any properties defined in config files or on the command-line (i.e. passed directly to Gateway.init()) will override them. The target attribute is optional, and declares whether the property should be included in only server or client processes, and omitted from the other.
This particular example defines a [ResourceImportHandler](../ResourceImportHandler), which can then be used to import custom resources in the <Imports> section. The portion of the property name after the dot, 'DEMO' defines the type attribute values that will use this handler.

    <Script target="server|client|both" lang="javascript" event="startup|initialized|shutdown">doStuff()</Script>
> Script logic can be provided in the module to run at various points of the server lifecycle to initialize module components. The lang attribute specifies the scripting engine to use. The target attribute states whether the script should run on server or client processes, or both. Currently supported execution points are:
 * *startup* - In a server, these scripts run just after each module registration during bootstrap. All of this modules resources have finished importing, but there may be other modules remaining. In client processes, they run at the end of Gateway.connect(user, pass, resource).
 * *initialized* - All modules have finished importing/verifying. This is the last thing done by the bootstrap process before it stops. Only occurs on server processes.
 * *shutdown* - These scripts run at the beginning of Gateway.close().

## Imports
All Items to be imported as part of this module are defined here.

    <Imports>
        <Resource name="ParentFactoryWf" version="0" type="CA" id="b9415b57-3a4a-4b31-825a-d307d1280ac0">boot/OD/NewDevObject.xsd</Resource>
> Defines a description object to be imported. This is simpler than using an Item tag, as it will pass the name, id, version and path to the defined handler, which will construct the complete Item, deriving all additional Item data. The id attribute allows the module to specify the UUID of the Item. It is optional, and if absent then a new UUID will be generated, but it is recommended to specify it as future synchronization mechanisms may use it. If no specific handler has been defined for a given type, it is passed to the default handler included in the kernel, which processes the following type attribute values to produce definition Items:
 * CA - Composite Activity Definition
 * EA - Elementary Activity Definition
 * OD - Outcome Description (Schema)
 * SC - Script
 * SM - State Machine
>
>The paths used to store these new Items are `<type root>/system/<namespace>'. Custom handlers may be defined to replace these. In the case of the default resource handler, the resource path given in the element text refers to a path within the module resources, but custom handlers may use this value however needed.

### Items

        <Item name="ParentFactory" initialPath="/desc/dev" workflow="ParentFactoryWf" id="57d87f6d-a83e-42b9-a7c2-800778d5e4d5">
> Specifies the name, Domain context, UUID and lifecycle name for the new Item. The UUID is optional, and will be generated if absent. A new instance of the named workflow will be created from description for this Item whenever the server boots.

            <Property name="Type">Factory</Property>
> Gives properties for this Item. The 'Name' property is automatically overlaid onto this set before instantiation. These will be overwritten on each server boot.

            <Outcome viewname="last" schema="PropertyDescription" version="0">boot/property/DescProp.xml</Outcome>
> Imports an Outcome into the new Item, generating a 'Bootstrap' event and a Viewpoint with the given name. The 'schema' and 'version' attributes refer to the Schema of the Outcome, and the text gives the path in the module resources in which the Outcome is stored.
> In this example, a PropertyDescription outcome is loaded, because it is an Item description. This outcome defines the Property set of its instantiated Items.

#### Collection definition

            <Dependency name="workflow" version="0" isDescription="false" itemDescriptionPath="/desc/dev/CompositeActivityFactory" itemDescriptionVersion="0">
> This is the definition of a [Dependency](../Dependency) [Collection](../Collection) named 'workflow' with a snapshot version '0'. It will be able to contain instances of the Item located at /dev/CompositeActivityFactory (which is the CristaliseDev workflow factory), complying with version 0 of its property description. This means it will reference CompositeActivityDef items. 

                <DependencyMember itemPath="b9415b57-3a4a-4b31-825a-d307d1280ac0">
> The Dependency will be initialized with one member, referenced by UUID here. It may instead referencea DomainPath.

                        <MemberProperties>
                            <KeyValuePair Key="Version" String="0"/>
                        </MemberProperties>
> Properties of the member slot may be defined. In this case a 'Version' property is supplied, which specifies the workflow version that will be used in instances of this factory.

                </DependencyMember>
                <CollectionProperties/>
> Collection properties may also be defined in the same format. Both property blocks may be omitted if not required.
            </Dependency>

            <Aggregation name="layout" version="0" isDescription="true">
> This defines an [Aggregation](../Aggregation) [CollectionDescription](../CollectionDescription) called 'layout' with snapshot version '0'. Note that Aggregations do not permit collection-level typing.

                <AggregationMember slotNo="0" itemPath="/desc/ItemDesc/ChildItemDesc">
> A member referencing an Item description that defines children that will fit in this slot. ItemDescriptionPath and Version may be defined here if required, as in Dependency collections but at the member level


                    <Geometry x="500" y="500" w="50" h="50/>
> 2-dimensional layout information giving the location and size of this member.

                    <MemberProperties/>
> Any member properties for this member

                </AggregationMember>
            </Aggregation>
        </Item>

###Roles

        <Role jobList="false">User</Role>
        <Role jobList="true">User/SubUser</Role>

Defines Agent roles. The path defines subroles, for which the parent roles must be already defined. The 'jobList' attribute defines whether this role is 'active' i.e. Activities push Jobs to the Joblists of Agents holding this role.

###Agents

Agent definition in modules does not yet support their new subclassing as Items. This format will be expanded to include the Item elements at a later date.

        <Agent name="user1" password="hunter2">
> Defines the agent with their default password.

            <Role>User</Role>
            <Role>Admin</Role>
> Each role the agent holds

        </Agent>
    </Imports>
</CristalModule>
