var name = job.getOutcome().getField("Name");
var type = job.getOutcome().getField("Type");

var found = false;

try { // check if already exists
	item.getCollection(name);
	found = true;
} catch (e) { }
if (found) throw "Collection "+name+" already exists in this Item Description";

var params = new Array(2);
params[0] = name;
params[1] = type;
agent.execute(item, "AddNewCollectionDescription", params);
