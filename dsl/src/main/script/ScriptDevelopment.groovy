import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway;;
import org.cristalise.kernel.utils.Logger

/**
 * This Groovy script is proveded to help CRISTAL-iSE Script development. It simulates the Script 
 * execution environment, actually the behaviour AgentProxy.execute(job) method call. 
 * 
 * In order to use this script you need to launch CRISTAL-iSE server before.
 */

//Configuration setup
String config   = 'src/integration-test/bin/client.conf'
String connect  = 'src/integration-test/bin/integTest.clc'
int logLevel    = 5

String user = "dev"
String pwd  = "test"

//Change this parameters to setup the actual Script execution environment.
//These 3 parameters will retrieve the Job used during AgentProxy.execute(job)
String itemPath       = "/integTest/PatientFactory-2016-11-21_12-24-24_051"
String activityName   = "CreateNewInstance"
String transitionName = "Done"

Gateway.init(AbstractMain.readC2KArgs( ['-logLevel', "$logLevel", '-config', config, '-connect', connect] as String[] ))

//These are the default binding variables created by the Script class
AgentProxy agent = Gateway.connect(user, pwd)
ItemProxy  item  = agent.getItem(itemPath)
Job        job   = item.getJobByTransitionName(activityName, transitionName, agent)

assert job

//========= Actual Script code shall be bellow this line ==================================

//Query has to be executed by the script, this will call it if defined
if(job.hasQuery()) job.setOutcome(item.executeQuery(job.getQuery()));


//========= Actual Script code shall be above this line ===================================

//This line  will execute the actual Activity simulating the AgentProxy.execute(job) behaviour
Logger.msg("Result:" + item.requestAction(job))

Gateway.close()