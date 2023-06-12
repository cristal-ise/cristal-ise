/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dev.scaffold

import static org.cristalise.dev.dsl.DevXMLUtility.recordToXML
import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.*
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION
import static org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription.FACTORY_GENERATED_NAME

import org.atteo.evo.inflector.English
import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.dev.utils.CrudFactoryHelper
import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.csv.TabularGroovyParserBuilder
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.persistency.outcomebuilder.Field
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.StandardClient
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.cli.picocli.CliBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Utility class to be used in different scenarios:
 *<pre>
 * - General purpose tool to create new Item which are CRUD compatible
 * - Import items from excel
 * - Creating Items to test their workflow
 *</pre>
 * {@link org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode}
 */
@CompileStatic
@Slf4j
class CRUDItemCreator extends StandardClient {

    /**
     * Specify behavior when calling the create methods
     * 
     * {@link #ERASE}
     * {@link #UPDATE}
     * {@link #SKIP}
     * {@link #ERROR}
     */
    public enum UpdateMode {
        /** Erase existing item */
        ERASE,
        /** Update existing item */
        UPDATE,
        /** Do not update existing item */
        SKIP,
        /** Throw error for existing item */
        ERROR
    }

    String moduleNs
    UpdateMode updateMode

    public CRUDItemCreator(String ns, UpdateMode mode) {
        moduleNs = ns
        updateMode = mode
    }

    /**
     * Use this constructor when agent cannot be initialised using the inherited login() method.
     * @param a the authenticated agent to be used to create the agents
     */
    public CRUDItemCreator(String ns, UpdateMode mode, AgentProxy a) {
        this(ns, mode)
        agent = a
    }

    /**
     *
     * @param factory
     * @return
     */
    protected static Schema getUpdateSchema(ItemProxy factory) {
        String schemaName = null
        Integer schemaVersion = null

        if (factory.checkCollection(SCHEMA_INITIALISE)) {
            def initSchemaCollection = factory.getCollection(SCHEMA_INITIALISE)
            DependencyMember member = (DependencyMember) initSchemaCollection.getMembers().list[0]

            schemaName = member.getChildUUID()
            def initSchemaVersion = member.getProperties().getBuiltInProperty(VERSION)

            if (initSchemaVersion instanceof String) schemaVersion = Integer.parseInt(initSchemaVersion)
            else                                     schemaVersion = (Integer)initSchemaVersion
        }
        else {
            def nameAndVersion = factory.getProperty('UpdateSchema').split(':')
            schemaName = nameAndVersion[0]
            schemaVersion = Integer.parseInt(nameAndVersion[1])
        }

        return LocalObjectLoader.getSchema(schemaName, schemaVersion)
    }

    /**
     *
     * @param factory
     * @param itemRoot
     * @param itemName
     * @return
     */
    protected ItemProxy instantiateItem(ItemProxy factory, String itemRoot, String subFolder, String itemName) {
        def createJob  = factory.getJobByTransitionName('InstantiateItem', 'Done', agent)
        assert createJob, "Cannot get Job for Activity 'InstantiateItem' of Factory '$factory'"

        createJob.outcome.setField('Name', itemName)
        if (subFolder) createJob.outcome.setField('SubFolder', subFolder)

        def result = agent.execute(createJob)

        //Name could be the generated by the Factory
        if (itemName == FACTORY_GENERATED_NAME) {
            def o = new Outcome(result)
            itemName = o.getField('Name')
        }

        log.info('instantiateItem() - created:{}', getFullPath(itemRoot, subFolder, itemName))

        return agent.getItem(getFullPath(itemRoot, subFolder, itemName))
    }

    /**
     * Uses the given schema to search the record for fields containing Item names and converts them to UUID string.
     * 
     * @param record the list of fields to be processed
     * @param schema containing the meta data to find fields and the referenced Item types
     */
    private void convertItemNamesToUuid(Map record, Schema schema) {
        def builder = new OutcomeBuilder(schema)

        record.each { fieldName, fieldValue ->
            def field = (Field)builder.findChildStructure((String)fieldName)
            StringBuffer newValue = new StringBuffer()

            String referencedItemType = field?.getAppInfoNodeElementValue('reference', 'itemType')

            if (referencedItemType) {
                def typeFolder = English.plural(referencedItemType)
                Boolean isMultiple = field.getAppInfoNodeElementValue('dynamicForms', 'multiple') as Boolean

                if (isMultiple) {
                    newValue.append('[')
                    fieldValue.toString().split(',').each { value ->
                        if (newValue.toString() != '[') newValue.append(',')
                        def referencedItem = agent.getItem("$moduleNs/${typeFolder}/$value")
                        newValue.append(referencedItem.getUuid())
                    }
                    newValue.append(']')
                }
                else {
                    def referencedItem = agent.getItem("$moduleNs/${typeFolder}/$fieldValue")
                    newValue.append(referencedItem.getUuid())
                }

                log.debug('convertItemNamesToUuid() - field:{} replacing value {} with {}', fieldName, fieldValue, newValue)
                record[fieldName] = newValue.toString()
            }
        }
    }

    /**
     *
     * @param newItem
     * @param itemRoot
     * @param itemName
     * @param updateSchema
     * @param record
     */
    protected void updateItem(ItemProxy newItem, Schema updateSchema, Map record) {
        def itemUpdateJob = newItem.getJobByName('Update', agent)
        assert itemUpdateJob, "Cannot get Job for Activity 'Update' of Item '$newItem'"

        Outcome outcome = null

        if (record.containsKey('outcome') && (record.outcome instanceof Outcome)) {
            outcome = (Outcome)record.outcome
        }
        else {
            convertItemNamesToUuid(record, updateSchema)
            outcome = new Outcome(DevXMLUtility.recordToXML(updateSchema.getName(), record), updateSchema)
        }

        itemUpdateJob.setOutcome(outcome)
        agent.execute(itemUpdateJob)

        log.info('updateItem() - updated:{}', newItem)

        //Checks viewpoint of Update outcome
        newItem.getViewpoint(updateSchema.getName(), 'last')
    }

    /**
     * 
     * @param record
     * @param factoryPath
     * @param eraseOrWhat
     * @return
     */
    public ItemProxy createItemWithConstructor(Map record, String factoryPath) {
        ItemProxy factory = agent.getItem(factoryPath)
        String itemRoot = CrudFactoryHelper.getDomainRoot(factory)
        String subFolder = record.SubFolder // can be null
        String itemName = record.Name ?: FACTORY_GENERATED_NAME
        Schema updateSchema = getUpdateSchema(factory)

        ItemProxy item = null

        def dp = new DomainPath(getFullPath(itemRoot, subFolder, itemName))

        if (dp.exists()) {
            item = agent.getItem(dp)

            if (itemName && updateMode == ERASE) {
                agent.execute(item, Erase.class)
                item = null
            }
            else if (itemName && updateMode == ERROR) {
                throw new ObjectAlreadyExistsException(dp.toString())
            }
        }

        if (!item) {
            def createJob  = factory.getJobByTransitionName('InstantiateItem', 'Done', agent)
            assert createJob, "Cannot get Job for Activity 'InstantiateItem' of Factory '$factory'"

            convertItemNamesToUuid(record, updateSchema)

            def outcome = createJob.getOutcome()
            //Name could be the generated by the Factory
            outcome.setField('Name', itemName)
            if (subFolder) createJob.outcome.setField('SubFolder', subFolder)

            outcome.appendXmlFragment(
                "/CrudFactory_NewInstanceDetails/SchemaInitialise", 
                recordToXML(updateSchema.name, record)
            )

            def result = agent.execute(createJob)

            //Name could be the generated by the Factory
            if (itemName == FACTORY_GENERATED_NAME) {
                def o = new Outcome(result)
                itemName = o.getField('Name')
            }
            record.Name = itemName

            item = agent.getItem(getFullPath(itemRoot, subFolder, itemName))
        }
        else {
            if (updateMode != SKIP) updateItem(item, updateSchema, record)
        }

        //Checks viewpoint of Update outcome
        item.getViewpoint(updateSchema.getName(), 'last')

        return item
    }

    /**
     * 
     * @param record
     * @param factoryPath
     * @return
     */
    public ItemProxy createItemWithUpdate(Map record, String factoryPath) {
        String itemName = record.Name ?: FACTORY_GENERATED_NAME
        String subFolder = record.SubFolder // can be null

        ItemProxy item = createItem(itemName, subFolder, factoryPath)

        //Name could be the generated by the Factory
        record.Name = item.name

        ItemProxy factory = agent.getItem(factoryPath)
        Schema updateSchema = getUpdateSchema(factory)

        // Make sure that the item is updated for the first time
        boolean forceUpate = ! item.checkViewpoint(updateSchema.name, 'last')

        if (updateMode != SKIP || forceUpate) updateItem(item, updateSchema, record)

        return item
    }

    /**
     * 
     * @param itemRoot the root directory for the newly created items
     * @param subFolder add this folder to itemRoot. Can be null
     * @param itemName the Name of the Item to be created
     * @return
     */
    private String getFullPath( String itemRoot, String subFolder, String itemName) {
        return itemRoot + (subFolder ? "/$subFolder/$itemName" : "/$itemName")
    }

    /**
     * 
     * @param itemName the name of the Item to be created
     * @param factoryPath string path of the factory Item
     * @return the ItemProxy of the item with the given Name and root
     */
    public ItemProxy createItem(String itemName, String factoryPath) {
        return createItem(itemName, null, factoryPath)
    }

    /**
     * 
     * @param itemName the name of the Item to be created
     * @param subFolder add this folder to itemRoot. Can be null
     * @param factoryPath string path of the factory Item
     * @return the ItemProxy of the item with the given Name and root
     */
    public ItemProxy createItem(String itemName, String subFolder, String factoryPath) {
        assert itemName, "name must be provided. Use $FACTORY_GENERATED_NAME if the name is generated by the factory"

        ItemProxy factory = agent.getItem(factoryPath)
        String itemRoot = CrudFactoryHelper.getDomainRoot(factory)
        ItemProxy item = null

        def dp = new DomainPath(getFullPath(itemRoot, subFolder, itemName))

        if (dp.exists()) {
            item = agent.getItem(dp)

            if (updateMode == ERASE) {
                agent.execute(item, Erase.class);
                item = null
            }
            else if (updateMode == ERROR) {
                throw new ObjectAlreadyExistsException(dp.toString())
            }
        }

        if (!item) item = instantiateItem(factory, itemRoot, subFolder, itemName)

        return item
    }

    /**
     * 
     * @param xlsx
     * @param itemType
     */
    public void createItems(File xlsx, String itemType) {
        TabularGroovyParser parser = TabularGroovyParserBuilder.build(xlsx, itemType, 1)

        parser.eachRow() { Map<String, Object> record, int i ->
            createItemWithUpdate(record, "/$moduleNs/${itemType}Factory")
        }
    }

    /**
     * 
     * @param args
     */
    @CompileDynamic
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AbstractMain.shutdown(0);
            }
        });

        def cli = new CliBuilder(usage: 'CRUDItemCreator -[hclstnm] [excelCsvfile]')
        cli.width = 100

        cli.with {
            h(longOpt: 'help', 'Show usage information')
            c(longOpt: 'config',     args: 1, argName: 'config.conf', 'Cristal-ise config file',            required: true)
            l(longOpt: 'connect',    args: 1, argName: 'local.clc',   'Cristal-ise clc file',               required: true)
            s(longOpt: 'shiro',      args: 1, argName: 'shiro.ini',   'Shiro auth config file',             required: true)
            t(longOpt: 'itemTypes',  args: 1, argName: 'types',       'Comma separated list of Item types', required: true)
            n(longOpt: 'moduleNs',   args: 1, argName: 'ns',          'Module namespace',                   required: true)
            m(longOpt: 'updateMode', args: 1, argName: 'mode',        'if the Item exists ERASE|UPDATE|SKIP default:UPDATE')
        }

        def options = cli.parse(args)
        
        //if there was an error 'usage' was already printed
        if (!options) return

        // Show usage text when -h or --help option is used.
        if (options.h) {
            cli.usage();
            return
        }

        if (!options.arguments()) {
            println "Please provide input csv/excel file"
            cli.usage()
            return
        }

        File xlsx       = new File(options.arguments()[0])
        String types    = (String)options.t
        String moduleNs = (String)options.n
        UpdateMode mode = UPDATE;

        if (options.m) mode = options.m as UpdateMode

        Properties properties = readPropertyFiles(options.c, options.l, null)
        properties.setProperty('Shiro.iniFile', options.s)
        Gateway.init(properties)

        def creator = new CRUDItemCreator(moduleNs, mode)
        creator.login('admin', 'test', null)

        types.split(',').each { type ->
            log.info('main() - creating items of type:{}', type)
            creator.createItems(xlsx, type)
        }

        Gateway.close()
    }
}
