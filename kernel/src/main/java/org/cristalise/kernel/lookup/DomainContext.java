package org.cristalise.kernel.lookup;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.utils.DescriptionObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
public class DomainContext implements DescriptionObject {
    String   namespace;
    String   name;
    Integer  version;
    ItemPath itemPath;

    DomainPath domainPath;

    public DomainContext(String path) {
        domainPath = new DomainPath(path);
        String[] capitalized = (String[]) Arrays.stream(domainPath.getPath()).map(s -> StringUtils.capitalize(s)).toArray();
        name = String.join("", capitalized) + "Context";

        log.debug("ctor() - name:{}", name);
    }

    public void setDomainPath(String path) {
        domainPath = new DomainPath(path);
    }

    @Override
    public String getItemID() {
        return (itemPath != null) ? itemPath.getUUID().toString() : null;
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public void export(Writer imports, File dir, boolean shallow)
            throws InvalidDataException, ObjectNotFoundException, IOException {
        // TODO Auto-generated method stub
    }

}
