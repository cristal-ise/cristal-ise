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
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterType;
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

    public D loadObjectFromBootstrap(String name) throws InvalidDataException, ObjectNotFoundException {
        try {
            log.trace("loadObjectFromBootstrap() - name:" + name + " Loading it from kernel items");

            String bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));
            StringTokenizer str = new StringTokenizer(bootItems, "\n\r");
            while (str.hasMoreTokens()) {
                String resLine = str.nextToken();
                String[] resElem = resLine.split(",");
                if (resElem[0].equals(name) || isBootResource(resElem[1], name)) {
                    log.trace("loadObjectFromBootstrap() - Shimming " + getTypeCode() + " " + name + " from bootstrap");
                    String resData = Gateway.getResource().getTextResource(null, "boot/" + resElem[1] + (resElem[1].startsWith("OD") ? ".xsd" : ".xml"));
                    return buildObject(name, 0, new ItemPath(resElem[0]), resData);
                }
            }

            for (Module module: Gateway.getModuleManager().getModules()) {
                log.trace("loadObjectFromBootstrap() - name:" + name + " Lodaing it from module:"+module.getName());

                ModuleResource res = (ModuleResource) module.getImports().findImport(name);

                if (res != null) {
                    res.setNs(module.getNs());
                    String resData = Gateway.getResource().getTextResource(module.getNs(), res.getResourceLocation());
                    // At this point the resource loaded from classpath, which means it has no UUID so a random UUID is assigned 
                    String uuid = res.getID() == null ? UUID.randomUUID().toString() : res.getID();
                    return buildObject(name, 0, new ItemPath(uuid), resData);
                }
            }
        }
        catch (Exception e) {
            log.error("Error finding bootstrap resources", e);
            throw new InvalidDataException("Error finding bootstrap resources");
        }
        throw new ObjectNotFoundException("Resource " + getSchemaName() + " " + name + " not found in bootstrap resources");
    }

    protected boolean isBootResource(String filename, String resName) {
        return filename.equals(getTypeCode() + "/" + resName);
    }

    public ItemPath findItem(String name) throws ObjectNotFoundException, InvalidDataException {
        if (Gateway.getLookup() == null) throw new ObjectNotFoundException("Cannot find Items without a Lookup");

        // first check for a UUID name
        // exception handling is slow, the if() avoids to use exception to check valid UUID
        if (ItemPath.isUUID(name)) {
            try {
                ItemPath resItem = new ItemPath(name);
                if (resItem.exists()) return resItem;
            }
            catch (InvalidItemPathException ex) {}
        }

        // then check for a direct path
        DomainPath directPath = new DomainPath(name);
        if (directPath.exists() && directPath.getItemPath() != null) { 
            return directPath.getItemPath();
        }

        Iterator<Path> searchResult = null;

        // else ...
        if (Gateway.getProperties().getBoolean("LocalObjectLoader.lookupUseProperties", false)) {
            // search for it in the whole tree using properties
            Property[] searchProps = new Property[classIdProps.length + 1];
            searchProps[0] = new Property(NAME, name);
            System.arraycopy(classIdProps, 0, searchProps, 1, classIdProps.length);

            searchResult = Gateway.getLookup().search(new DomainPath(getTypeRoot()), searchProps);
        }
        else {
            if (StringUtils.isBlank(getTypeRoot())) {
                throw new InvalidDataException("TypeRoot is empty");
            }
            // or search for it in the subtree using name
            searchResult = Gateway.getLookup().search(new DomainPath(getTypeRoot()), name);
        }

        if (searchResult.hasNext()) {
            Path defPath = searchResult.next();
            if (searchResult.hasNext()) throw new ObjectNotFoundException("Too many matches for " + getTypeCode() + " " + name);

            if (defPath.getItemPath() == null)
                throw new InvalidDataException(getTypeCode() + " " + name + " was found, but was not an Item");

            return defPath.getItemPath();
        }
        else {
            throw new ObjectNotFoundException("No match for " + getTypeCode() + " " + name);
        }
    }

    /**
     * 
     * @param name the Name or the UUID of the resource Item
     * @param version the Version of the resource Item
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public D get(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        try {
            CacheEntry<D> thisDefEntry = null;
            synchronized (cache) {
                thisDefEntry = cache.get(name + "_" + version);
            }

            if (thisDefEntry != null) {
                log.trace("get() - " + name + " v" + version + " found in cache.");
                return thisDefEntry.def;
            }

            ItemPath defItemPath = findItem(name);
            String defUuid = defItemPath.getUUID().toString();

            synchronized (cache) {
                log.trace("get() - " + name + " v" + version + " not found in cache. Checking uuid.");
                thisDefEntry = cache.get(defUuid + "_" + version);

                if (thisDefEntry != null) {
                    log.trace("get() - " + name + " v" + version + " found in cache.");
                    return thisDefEntry.def;
                }
            }

            log.trace("get() - " + name + " v" + version + " not found in cache. Loading from database.");

            ItemProxy defItemProxy = Gateway.getProxyManager().getProxy(defItemPath);
            if (name.equals(defUuid)) {
                String itemName = defItemProxy.getName();
                if (itemName != null) name = itemName;
            }

            D thisDef = loadObject(name, version, defItemProxy);

            CacheEntry<D> entry = new CacheEntry<>(thisDef, defItemProxy, this);
            synchronized (cache) {
                cache.put(defUuid + "_" + version, entry);
                cache.put(name + "_" + version, entry);
            }

            return thisDef;
        }
        catch (ObjectNotFoundException ex) {
            // for bootstrap and testing, try to load built-in kernel objects from resources
            if (version == 0) {
                try {
                    return loadObjectFromBootstrap(name);
                }
                catch (ObjectNotFoundException ex2) {}
            }
            throw ex;
        }
    }

    public abstract String getTypeCode();

    public abstract String getSchemaName();

    public abstract String getTypeRoot();

    public abstract D buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException;

    public D loadObject(String name, int version, ItemProxy proxy) throws ObjectNotFoundException, InvalidDataException {
        Viewpoint smView = (Viewpoint) proxy.getObject(ClusterType.VIEWPOINT + "/" + getSchemaName() + "/" + version);
        String rawRes;
        try {
            rawRes = smView.getOutcome().getData();
        }
        catch (PersistencyException ex) {
            log.error("Problem loading " + getSchemaName() + " " + name + " v" + version, ex);
            throw new ObjectNotFoundException("Problem loading " + getSchemaName() + " " + name + " v" + version + ": " + ex.getMessage());
        }
        return buildObject(name, version, proxy.getPath(), rawRes);
    }

    public void removeObject(String id) {
        synchronized (cache) {
            if (cache.keySet().contains(id)) {
                log.debug("remove() - activityDef:" + id);
                cache.remove(id);
            }
        }
    }

    public class CacheEntry<E extends DescriptionObject> implements ProxyObserver<Viewpoint> {
        public String                    id;
        public ItemProxy                 proxy;
        public E                         def;
        public DescriptionObjectCache<E> parent;

        public CacheEntry(E def, ItemProxy proxy, DescriptionObjectCache<E> parent) {
            this.id = def.getItemID() + "_" + def.getVersion();
            this.def = def;
            this.parent = parent;
            this.proxy = proxy;
            proxy.subscribe(new MemberSubscription<Viewpoint>(this, ClusterType.VIEWPOINT.getName(), false));
        }

        @Override
        public void finalize() {
            parent.removeObject(id);
            proxy.unsubscribe(this);
        }

        @Override
        public void add(Viewpoint contents) {
            parent.removeObject(id);
        }

        @Override
        public void remove(String oldId) {
            parent.removeObject(id);
        }

        @Override
        public String toString() {
            return "Cache entry: " + id;
        }

        @Override
        public void control(String control, String msg) {
        }
    }
}
