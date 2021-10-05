/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
/**
 *
 */
package org.cristalise.kernel.utils;

import static org.cristalise.kernel.lookup.Lookup.SearchConstraints.EXACT_NAME_MATCH;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.Module;
import org.cristalise.kernel.process.module.ModuleResource;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DescriptionObjectCache<D extends DescriptionObject> {

    SoftCache<String, CacheEntry<D>> cache = new SoftCache<String, CacheEntry<D>>();
    Property[]                       classIdProps;

    public DescriptionObjectCache() {
        try {
            String propDescXML = Gateway.getResource().findTextResource("boot/property/" + getTypeCode() + "Prop.xml");
            PropertyDescriptionList propDescs = (PropertyDescriptionList) Gateway.getMarshaller().unmarshall(propDescXML);
            ArrayList<Property> classIdPropList = new ArrayList<Property>();
            for (PropertyDescription propDesc : propDescs.list) {
                if (propDesc.getIsClassIdentifier()) classIdPropList.add(propDesc.getProperty());
            }
            classIdProps = classIdPropList.toArray(new Property[classIdPropList.size()]);
        }
        catch (Exception ex) {
            log.error("Could not load property description for " + getTypeCode() + ". Cannot filter.", ex);
            classIdProps = new Property[0];
        }
    }

    /**
     * 
     * @param name UUID or Name of the resource Item
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private D loadObjectFromBootstrap(String name) throws InvalidDataException, ObjectNotFoundException {
        try {
            log.debug("loadObjectFromBootstrap() - name:{} typeCode:{}", name, getTypeCode());

            String bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));
            StringTokenizer str = new StringTokenizer(bootItems, "\n\r");

            while (str.hasMoreTokens()) {
                String resLine = str.nextToken();

                String[] resElem = resLine.split(",");
                String uuid = resElem[0];
                String path = resElem[1];

                if (uuid.equals(name) || isBootResource(path, name)) {
                    String textResourcePath = "boot/" + path + (path.startsWith("OD") ? ".xsd" : ".xml");
                    log.trace("loadObjectFromBootstrap() - FOUND in KERNEL uuid:{} textResourcePath:{}", uuid, textResourcePath);

                    String resData = Gateway.getResource().getTextResource(null, textResourcePath);
                    String realName = path.split("/")[1]; //name input could contain UUID (see if() statement)
                    return buildObject(realName, 0, new ItemPath(uuid), resData);
                }
            }

            //this search only works if name does not contain the UUID (see note bellow)
            for (Module module: Gateway.getModuleManager().getModules()) {
                ModuleResource res = (ModuleResource) module.getImports().findImport(name, getTypeCode());

                if (res != null) {
                    res.setNs(module.getNs());
                    log.trace("loadObjectFromBootstrap() - FOUND in module:{} textResourcePath:{}", module.getName(), res.getResourceFileName());

                    String resData = Gateway.getResource().getTextResource(module.getNs(), res.getResourceFileName());
                    // if it has no UUID a random UUID is assigned 
                    String uuid = res.getID() == null ? UUID.randomUUID().toString() : res.getID();
                    return buildObject(name, 0, new ItemPath(uuid), resData);
                }
            }
        }
        catch (Exception e) {
            String msg = "name:"+name+" typeCode:"+getTypeCode();

            log.error("loadObjectFromBootstrap() cannot find resource {}", msg, e);
            throw new InvalidDataException("Cannot find resource "+ msg);
        }
        throw new ObjectNotFoundException("Resource " + getSchemaName() + " " + name + " not found in bootstrap resources");
    }

    /**
     * 
     * @param filename
     * @param resName
     * @return
     */
    protected boolean isBootResource(String filename, String resName) {
        return filename.equals(getTypeCode() + "/" + resName);
    }

    /**
     * Finds the resource Item in the database and returns the ItemPath based on the UUID/Name/DomainPath
     * 
     * @param id UUID or Item Name or DomainPath
     * @param transactionKey if transaction is involved
     * @return the ItemPath
     * @throws ObjectNotFoundException if object was not found
     * @throws InvalidDataException Data was inconsistent 
     */
    private ItemPath findItem(String id, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        if (Gateway.getLookup() == null) throw new ObjectNotFoundException("Cannot find Items without a Lookup");

        // first check if name is a UUID or not
        if (ItemPath.isUUID(id)) {
            try {
                // exception handling is slow, the if() avoids to use exception to check valid UUID
                ItemPath resItem = new ItemPath(id);
                if (resItem.exists(transactionKey)) return resItem;
            }
            catch (InvalidItemPathException ex) {/*should never happen*/}
        }
        else if (id.contains("/")) {
            // then check for a DomainPath
            DomainPath directPath = new DomainPath(id);
            if (directPath.exists(transactionKey) && directPath.getItemPath(transactionKey) != null) { 
                return directPath.getItemPath(transactionKey);
            }
        }
        else {
            // finally search item tree
            Iterator<Path> searchResult = null;

            if (Gateway.getProperties().getBoolean("LocalObjectLoader.lookupUseProperties", false) || StringUtils.isBlank(getTypeRoot())) {
                // search for it in the whole tree using properties
                Property[] searchProps = new Property[classIdProps.length + 1];
                searchProps[0] = new Property(NAME, id);
                System.arraycopy(classIdProps, 0, searchProps, 1, classIdProps.length);

                searchResult = Gateway.getLookup().search(new DomainPath(getTypeRoot()), transactionKey, searchProps);
            }
            else {
                // or search for it in the subtree using name
                searchResult = Gateway.getLookup().search(new DomainPath(getTypeRoot()), id, EXACT_NAME_MATCH, transactionKey);
            }

            if (searchResult.hasNext()) {
                Path defPath = searchResult.next();
                if (searchResult.hasNext()) throw new InvalidDataException("Too many matches for id:" + id + " typeCode:" + getTypeCode());

                if (defPath.getItemPath(transactionKey) == null) {
                    throw new InvalidDataException("id:" + id + " typeCode:" + getTypeCode() + " was found, but was not an Item");
                }

                return defPath.getItemPath(transactionKey);
            }
        }
        throw new ObjectNotFoundException("No match for id:" + id + " typeCode:" + getTypeCode());
    }

    /**
     * 
     * @param id Name or UUID or DomainPath of the resource Item
     * @param version of the resource Item
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public D get(String id, int version) throws ObjectNotFoundException, InvalidDataException {
        return get(id, version, null);
    }

    /**
     * 
     * @param uuid
     * @param version
     * @return
     */
    private D findInCache(String uuid, int version) {
        String key = uuid + "_" + version;

        CacheEntry<D> cacheEntry = cache.get(key);

        if (cacheEntry != null) {
            log.trace("findInCache() - key:{} found in cache.", key);
            return cacheEntry.descObject;
        }

        return null;
    }

    /**
     * 
     * @param id Name or UUID or DomainPath of the resource Item
     * @param version the Version of the resource Item
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public synchronized D get(String id, int version, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        String key = id + "_" + version;

        // If id is a uuid check if cache contains the key
        if (ItemPath.isUUID(id)) {
            D obj = findInCache(id, version);
            if (obj != null) return obj;
        }

        try {
            log.trace("get() - key:{} not found in cache. Loading from database.", key);

            ItemPath resourcePath = findItem(id, transactionKey);
            String realUuid = resourcePath.getName();

            // if id is NOT a uuid check again if cache contains the key already using the real uuid
            if ( ! ItemPath.isUUID(id)) {
                D obj = findInCache(realUuid, version);
                if (obj != null) return obj;
            }

            D thisDef = loadObject(realUuid, version, Gateway.getProxy(resourcePath), transactionKey);

            CacheEntry<D> newEntry = new CacheEntry<>(thisDef, this);
            cache.put(newEntry.key, newEntry);

            return thisDef;
        }
        catch (ObjectNotFoundException ex) {
            log.trace("get() - failed to load resource key:{}_{} from database, loading from classpath.", id, version);
            // for bootstrap and testing, try to load built-in kernel objects from resources
            if (version == 0) {
                try {
                    return loadObjectFromBootstrap(id);
                }
                catch (ObjectNotFoundException ex2) {
                    log.error("get() - ", ex2);
                }
            }
            else {
                log.error("get() - only resources with version zero can be loaded from classpath - name:{} version:{}", id, version);
            }
            throw ex;
        }
    }

    protected abstract String getTypeCode();

    protected abstract String getSchemaName();

    protected abstract String getTypeRoot();

    /**
     * 
     * @param name
     * @param version
     * @param path
     * @param data
     * @return
     * @throws InvalidDataException
     */
    protected abstract D buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException;

    /**
     * 
     * @param name
     * @param version
     * @param proxy
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected D loadObject(String name, int version, ItemProxy proxy, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        try {
            Viewpoint view = proxy.getViewpoint(getSchemaName(), String.valueOf(version), transactionKey);
            String rawRes = view.getOutcome(transactionKey).getData();
            return buildObject(proxy.getName(), version, proxy.getPath(), rawRes);
        }
        catch (PersistencyException ex) {
            log.error("loadObject() - Problem loading " + getSchemaName() + " " + name + " v" + version, ex);
            throw new ObjectNotFoundException("Problem loading " + getSchemaName() + " " + name + " v" + version + ": " + ex.getMessage());
        }
    }

    public synchronized void invalidate(String id, int version) {
        cache.remove(id+"_"+version);
    }

    public synchronized void invalidate() {
        cache.clear();
    }

    protected class CacheEntry<E extends DescriptionObject> {
        public String key;
        public E      descObject;

        public CacheEntry(E def, DescriptionObjectCache<E> parent) {
            this.key = def.getItemID() + "_" + def.getVersion();
            this.descObject = def;
        }

        @Override
        public String toString() {
            return "Cache entry: " + key;
        }
    }
}
