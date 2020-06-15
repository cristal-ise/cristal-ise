var name   = job.getOutcome().getField("Name");
var folder = job.getOutcome().getField("SubFolder");
var roles  = job.getOutcome().getField("InitialRoles");
var pwd    = job.getOutcome().getField("Password");

var root = job.getActPropString("Root");

var domPath = (root != null ? root : "") + "/" + (folder != null ? folder : "");

// Create new Item
var params = new Array(4);
params[0] = name;
params[1] = domPath;
params[2] = roles;
params[3] = pwd;

try {
    agent.execute(item, "CreateAgentFromDescription", params);
} catch (e) {
    throw "Could not create "+name+": "+e.message;
}
