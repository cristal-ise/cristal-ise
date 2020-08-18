/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
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
String itemPath       = "/integTest/XPathOutcomeInitTest_Second-2017-05-19_12-22-43_004"
String activityName   = "AssignNewVersionFromLast"
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
//Logger.msg("Result:" + item.requestAction(job))

Gateway.close()