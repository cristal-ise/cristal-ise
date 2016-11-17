# Description-Driven Trigger module

This module is based on [Quartz Scheduler](http://www.quartz-scheduler.org/). The implementation use StdSchedulerFactory to create very basic jobs using SimpleTrigger. Jobs are not persisted by Quartz Scheduler, which means each time the process starts the QuartzJobs are recreated form the persistent Joblist of CRISTAL-iSE Agent (e.g. triggerAgent configured in module.xml).

##Configuring the StateMachine

This diagram shows the CRISTAL-iSE Activity StateMachine defined in this module. Check this wiki for further details on StateMachines: [cristal-ise/kernel/wiki/State-Machine](https://github.com/cristal-ise/kernel/wiki/State-Machine). This definition is provided so the the module can be used without further changes, but it is possible to configure it to use a different one.

![CRISTAL-iSETriggerSM.puml](http://uml.mvnsearch.org/gist/f5a862d0bb01c192a6c34f0259f3b469)

##Configuring the StateMachine
| Name |  Default Value | Description |
|------|----------------|-------------|
| Trigger.StateMachineNS | trigger | |
| Trigger.StateMachine   | boot/SM/Trigger.xml | |
| Trigger.Transitions    | Warning,Timeout | |
