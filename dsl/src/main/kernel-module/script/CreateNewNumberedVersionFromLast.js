var viewName = job.getActPropString("ViewName");

if (viewName.equals("")) throw "ViewName not specified. Cannot create new version.";

var lastView = -1;
var existingViews = item.getContents("/ViewPoint/"+viewName);

for (i=0; i<existingViews.length; i++) {
    var thisView = parseInt(existingViews[i]);

    if (thisView != NaN && lastView < thisView) lastView = thisView;
}
lastView++;

try {
    var existingLast = agent.unmarshall(item.queryData("/ViewPoint/"+viewName+"/last"));
}
catch (e) {
    throw "No data found for '/ViewPoint/"+viewName+"/last'. You must submit at least one outcome!";
}

var params = new Array(3);
params[0] = viewName;
params[1] = lastView;
params[2] = existingLast.getEventId();

agent.execute(item, "WriteViewpoint", params);

//this was the old code, based on hardcoded collections
//updateDescColls(viewName, lastView); 

updateDependenciesVersion(lastView);
