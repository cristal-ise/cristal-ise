var name    = job.getOutcome().getField("ObjectName");
var folder  = job.getOutcome().getField("SubFolder");
var handler = new org.cristalise.kernel.process.resource.DefaultResourceImportHandler(job.getActPropString("NewType"));

// Find the root of that object type
var domPath = handler.getTypeRoot();
if (folder != null) domPath = domPath + "/" + folder;

var params = new Array(2);
params[0] = name;
params[1] = domPath;

// Create the new item
try {
    agent.execute(item, "CreateItemFromDescription", params);
}
catch (e) {
    throw "Could not create "+name+": "+e.message;
}
