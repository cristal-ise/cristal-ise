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

import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.ERASE
import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.SKIP
import static org.cristalise.dev.scaffold.CRUDItemCreator.UpdateMode.UPDATE
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION

import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.csv.TabularGroovyParserBuilder
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.persistency.outcomebuilder.Field
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.StandardClient
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.cli.commons.CliBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class CRUDItemCreator extends StandardClient {

    public enum UpdateMode {ERASE, UPDATE, SKIP}

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

        if (factory.checkContent(ClusterType.COLLECTION, SCHEMA_INITIALISE.name)) {
            def initSchemaCollection = factory.getCollection(SCHEMA_INITIALISE)
            DependencyMember member = initSchemaCollection.getMembers().list[0]

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
    protected ItemProxy createItemAndCheck(ItemProxy factory, String itemRoot, String itemName) {
        def createJob  = factory.getJobByTransitionName('InstantiateItem', 'Done', agent)
        assert createJob, "Cannot get Job for Activity 'InstantiateItem' of Factory '$factory.path'"

        def outcome = createJob.getOutcome()

        //Name could be the generated by the Factory
        if (itemName) outcome.setField('Name', itemName)

        def result = agent.execute(createJob)

        //Name could be the generated by the Factory
        if (!itemName) {
            def o = new Outcome(result)
            itemName = o.getField('Name')
        }

        log.info('createItemAndCheck() - created:{}/{}', itemRoot, itemName)

        return agent.getItem("$itemRoot/$itemName")
    }

    /**
     * 
     * @param record
     * @param updateSchema
     * @param itemRoot
     */
    private void convertItemNamesToUuid(Map record, Schema updateSchema, String itemRoot) {
        def builder = new OutcomeBuilder(updateSchema)

        record.each { fieldName, fieldValue ->
            def field = (Field)builder.findChildStructure((String)fieldName)
            StringBuffer newValue = new StringBuffer()

            String referencedItemType = field.getAppInfoNodeElementValue('reference', 'itemType')

            if (referencedItemType) {
                Boolean isMultiple = field.getAppInfoNodeElementValue('dynamicForms', 'multiple') as Boolean

                if (isMultiple) {
                    newValue.append('[')
                    fieldValue.toString().split(',').each { value ->
                        if (newValue.toString() != '[') newValue.append(',')
                        newValue.append(agent.getItem("$moduleNs/${referencedItemType}s/$value").getPath().getUUID().toString())
                    }
                    newValue.append(']')
                }
                else {
                    newValue.append(agent.getItem("$moduleNs/${referencedItemType}s/$fieldValue").getPath().getUUID().toString())
                }

                log.info('convertItemNamesToUuid() - field:{} replacing value {} with {}', fieldName, fieldValue, newValue)
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
    protected void updateItemAndCheck(ItemProxy newItem, String itemRoot, String itemName, Schema updateSchema, Map record) {
        def itemUpdateJob = newItem.getJobByName('Update', agent)
        assert itemUpdateJob, "Cannot get Job for Activity 'Update' of Item '$itemRoot/$itemName'"

        def updateOutcome = itemUpdateJob.getOutcome()
        convertItemNamesToUuid(record, updateSchema, itemRoot)

        itemUpdateJob.setOutcome(new Outcome(DevXMLUtility.recordToXML(updateSchema.getName(), record), updateSchema))

        agent.execute(itemUpdateJob)

        log.info('updateItemAndCheck() - updated:{}/{}', itemRoot, itemName)

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
    public ItemProxy createItemWithConstructorAndCheck(Map record, String factoryPath) {
        ItemProxy factory = agent.getItem(factoryPath)
        String itemRoot = factory.getProperty('Root')
        String itemName = record.Name ?: ''
        Schema updateSchema = getUpdateSchema(factory)
        ItemProxy item = null

        def dp = new DomainPath(itemRoot+'/'+itemName)

        if (dp.exists()) {
            item = agent.getItem(dp)

            if (itemName && updateMode == ERASE) {
                agent.execute(item, Erase.class)
                item = null
            }
        }

        if (!item) {
            def createJob  = factory.getJobByTransitionName('InstantiateItem', 'Done', agent)
            assert createJob, "Cannot get Job for Activity 'InstantiateItem' of Factory '$factory.path'"

            convertItemNamesToUuid(record, updateSchema, itemRoot)

            def outcome = createJob.getOutcome()
            //Name could be the generated by the Factory
            if (itemName) outcome.setField('Name', itemName)
            outcome.appendXmlFragment "/CrudFactory_NewInstanceDetails/SchemaInitialise", DevXMLUtility.recordToXML(updateSchema.getName(), record)

            def result = agent.execute(createJob)
            //Name could be the generated by the Factory
            if (!itemName) {
                def o = new Outcome(result)
                itemName = o.getField('Name')
            }
            item = agent.getItem(dp)
        }
        else {
            if (updateMode != SKIP) updateItemAndCheck(item, itemRoot, itemName, updateSchema, record)
        }

        //Checks viewpoint of Update outcome
        item.getViewpoint(updateSchema.getName(), 'last')

        return item
    }

    /**
     * 
     * @param record
     * @param factoryPath
     * @param eraseOrWhat
     * @return
     */
    public ItemProxy createItemWithUpdateAndCheck(Map record, String factoryPath) {
        ItemProxy factory = agent.getItem(factoryPath)
        String itemRoot = factory.getProperty('Root')
        String itemName = record.Name ?: ''
        ItemProxy item = null

        def dp = new DomainPath(itemRoot+'/'+itemName)

        if (dp.exists()) {
            item = agent.getItem(dp)

            if (itemName && updateMode == ERASE) {
                agent.execute(item, Erase.class);
                item = null
            }
        }

        if (!item) {
            item = createItemAndCheck(factory, itemRoot, itemName)
            updateMode = UPDATE //this makes sure that the item is updated for the first time
        }

        itemName = item.getName()

        //Name could be generated by the Factory
        record.Name = itemName

        if (updateMode != SKIP) updateItemAndCheck(item, itemRoot, itemName, getUpdateSchema(factory), record)

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
            createItemWithUpdateAndCheck(record, "/$moduleNs/${itemType}Factory")
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
