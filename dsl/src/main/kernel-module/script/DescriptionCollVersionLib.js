function createDescCollVersion( name, version ) {
    var params = new Array(2);
    params[0] = name;
    params[1] = version;
    agent.execute(item, "CreateNewCollectionVersion", params);
}

//Reads all Dependencies of this Description Item and updates their version
function updateDependenciesVersion( lastView ) {
    var allCollections = item.getContents("/Collection");

    for (i = 0; i < allCollections.length; i++) {
        //TODO: check if the collection is a Dependency
        createDescCollVersion(allCollections[i], lastView)
    }
}

//DEPRECATED
function updateDescColls( viewName, lastView ) {
    if (viewName.equals("ElementaryActivityDef")) {
        createDescCollVersion("Schema", lastView);
        createDescCollVersion("Script", lastView);
        createDescCollVersion("StateMachine", lastView);
    }
    else if (viewName.equals("CompositeActivityDef")) {
        createDescCollVersion("Activity", lastView);
    }
    else if (viewName.equals("Script")) {
        createDescCollVersion("Include", lastView);
    }
}
