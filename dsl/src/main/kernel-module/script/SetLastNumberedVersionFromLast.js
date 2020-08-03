var viewName = job.getActPropString("ViewName");

if (viewName == null || viewName.equals("")) throw "ViewName not specified.";

var lastView = -1;
var existingViews = item.getContents("/ViewPoint/"+viewName);

for (i=0; i<existingViews.length; i++) {
    var thisView = parseInt(existingViews[i]);
    if (thisView != NaN && lastView < thisView) lastView = thisView;
}

if(lastView == -1) throw "No numbered version was found. Execute CreateNewNumberedVersionFromLast first";

try {
    var existingLast = agent.unmarshall(item.queryData("/ViewPoint/"+viewName+"/last"));
}
catch (e) {
    throw "No last view found.";
}

var params = new Array(3);
params[0] = viewName;
params[1] = lastView;
params[2] = existingLast.getEventId();
agent.execute(item, "WriteViewpoint", params);

updateDescColls(viewName, lastView);
