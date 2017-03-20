# Description-Driven Trigger module [![Build Status](https://travis-ci.org/cristal-ise/trigger.svg?branch=master)]

This module provides Description-Driven Trigger funcionalities. It is based on [Quartz Scheduler](http://www.quartz-scheduler.org/) and it uses StdSchedulerFactory to create very basic jobs using SimpleTrigger. Jobs are NOT persisted by Quartz Scheduler becuase Jobs are persysted in the JobList of the Agent. Each time the process starts the QuartzJobs are recreated form the persistent Joblist of CRISTAL-iSE Agent. Check the definition of triggerAgent in module.xml.

##Description of Trigger StateMachine

This diagram shows the CRISTAL-iSE Activity StateMachine defined in this module. Check this wiki for further details on StateMachines: [cristal-ise/kernel/wiki/State-Machine](https://github.com/cristal-ise/kernel/wiki/State-Machine). This definition is provided so the the module can be used without further changes, but it is possible to configure it to use a different one.

![CRISTAL-iSETriggerSM.puml](http://uml.mvnsearch.org/gist/f5a862d0bb01c192a6c34f0259f3b469)

##TriggerProcess configuration

| Name                             |  Default Value      | Description |
|----------------------------------|---------------------|-------------|
| Trigger.Enabled                  | true                | switch on/off the entire trigger |
| Trigger.agent                    | triggerAgent        | the name of the agent |
| Trigger.password                 | n/a                 | the password of the agent |
| Trigger.StateMachine.name        | n/a                 | the name of the required StateMachine to retrieve from the backend |
| Trigger.StateMachine.version     | n/a                 | the version of the required StateMachine to retrieve from the backend |
| Trigger.StateMachine.transitions | Warning,Timeout     | the list of Transition names to be used by the TriggerProcess |
| Trigger.StateMachine.namespace   | trigger             | the namespace of the module, i.e. ns attribute in the module.xml, containing the definition of StateMachine. Use _Trigger.StateMachine.name and version_ if possbile |
| Trigger.StateMachine.bootfile    | boot/SM/Trigger.xml | the bootstrap file containing the definition of StateMachine. Use _Trigger.StateMachine.name and version_ if possbile |

##Activity properties used by the TriggerProcess
Trigger uses the naming convention, i.e. the name of the Transition with On/Duration/Unit suffixes, to find the data in Activity properties to create the Quartz Job.

| Name                      | Default Value | Description |
|---------------------------|---------------|-------------|
| ${TransitionName}On       | true          | enable/disable Triggering of this Transiation for the given Activity |
| ${TransitionName}Duration | n/a           | time when the Quartz Job is triggered |
| ${TransitionName}Unit     | n/a           | values of org.quartz.DateBuilder.IntervalUnit enumeration |
