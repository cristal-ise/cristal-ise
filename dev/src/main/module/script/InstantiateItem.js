// Get parameters from outcome
var name = job.getOutcome().getField("ObjectName");
var folder = job.getOutcome().getField("SubFolder");
var root = job.getActPropString("Root");
var domPath = (root != null?root:"") + "/" + (folder != null?folder:"");

// Create new Item
var params = new Array(2);
params[0] = name;
params[1] = domPath;

try {
    agent.execute(item, "CreateItemFromDescription", params);
} catch (e) {
    throw "Could not create "+name+": "+e.message;
}
