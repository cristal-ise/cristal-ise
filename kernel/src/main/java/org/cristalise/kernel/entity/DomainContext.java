package org.cristalise.kernel.entity;

import static org.apache.commons.lang3.StringUtils.capitalize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.DescriptionObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Name is computed from the DomainPath string: fist letters capitalised, slashes removed and 'Context' postfix added, 
 * e.g. /desc/Script/kernel => DescScriptKernelContext.
 */
@Getter @Setter @Slf4j
public class DomainContext implements DescriptionObject {
    private String   namespace;
    private String   name;
    private Integer  version;

    private ItemPath itemPath;

    @Setter(AccessLevel.NONE)
    private DomainPath domainPath;

    /**
     * List of SubContexts (can be empty)
     */
    private List<DomainContext> subCcontexts = new ArrayList<DomainContext>();

    public DomainContext() {}

    public DomainContext(String path) throws InvalidDataException {
        this(new DomainPath(path));
    }

    public DomainContext(String path, Integer v) throws InvalidDataException {
        this(new DomainPath(path), null, v);
    }

    public DomainContext(DomainPath path) throws InvalidDataException {
        this(path, null, null);
    }

    public DomainContext(String path, String ns, Integer v) throws InvalidDataException {
        this(new DomainPath(path), ns, v);
    }

    public DomainContext(DomainPath path, String ns, Integer v) throws InvalidDataException {
        if (path.getTarget() != null) throw new InvalidDataException("DomainContext '" + path + "' has target");

        namespace = ns;
        version = v;
        domainPath = path;

        String[] origPath = domainPath.getPath();
        String[] capitalized = Arrays.stream(origPath).map(s -> capitalize(s)).collect(Collectors.toList()).toArray(new String[origPath.length]);
        name = String.join("", capitalized) + "Context";

        log.debug("ctor() - name:{}", name);
    }

    @Override
    public String getItemID() {
        return (itemPath != null) ? itemPath.getUUID().toString() : null;
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        CollectionArrayList retArr = new CollectionArrayList();

//        retArr.put(makeDescCollection(BuiltInCollections.PARENT_CONTEXT, transactionKey, getParentContext()));
//        retArr.put(makeDescCollection(BuiltInCollections.SUB_CONTEXTS, transactionKey, getSubContexts(transactionKey).toArray(new DomainContext[subCcontexts.size()])));

        return retArr;
    }

    public DomainContext getParentContext() throws InvalidDataException {
        DomainPath parentPath = domainPath.getParent();

        if (parentPath != null) return new DomainContext(parentPath.getStringPath());
        else                    return null;
    }

    public List<DomainContext> getSubContexts(TransactionKey transactionKey) {
        List<Path> children = Gateway.getLookup().getChildren(domainPath, 0, 0, true, transactionKey).rows;
        List<DomainContext> result = new ArrayList<>();

        for (Path child : children) {
            DomainPath childDomainPath = (DomainPath)child;

            try {
                if (childDomainPath.getTarget() == null) result.add(new DomainContext(childDomainPath));
            }
            catch (InvalidDataException e) {
                //this should never happen because of the if statement above
            }
        }

        return result;
    }

    /**
     * DO NOT use this method, it is only needed for castor xml marshalling
     * 
     * @param path the domain path in string format
     */
    public void setDomainPath(String path) {
        this.domainPath = new DomainPath(path);
    }

    /**
     * Gets the domain path represented bz the the DomainContext item. String return type is required 
     * for castor xml marshalling
     * 
     * @return string format of the domain path
     */
    public String getDomainPath() {
        return domainPath.getStringPath();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public BuiltInResources getResourceType() {
        return BuiltInResources.DOMAIN_CONTEXT_RESOURCE;
    }
}
