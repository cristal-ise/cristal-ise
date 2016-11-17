# Description-Driven Trigger module

This module is based on [Quartz Scheduler](http://www.quartz-scheduler.org/). The implementation use StdSchedulerFactory to create very basic jobs using SimpleTrigger. Jobs are not persisted by Quartz Scheduler, which means each time the process starts the QuartzJobs are recreated form the persistent Joblist of CRISTAL-iSE Agent. Check the definition of triggerAgent in module.xml.

##Description of Trigger StateMachine

This diagram shows the CRISTAL-iSE Activity StateMachine defined in this module. Check this wiki for further details on StateMachines: [cristal-ise/kernel/wiki/State-Machine](https://github.com/cristal-ise/kernel/wiki/State-Machine). This definition is provided so the the module can be used without further changes, but it is possible to configure it to use a different one.

![CRISTAL-iSETriggerSM.puml](http://uml.mvnsearch.org/gist/f5a862d0bb01c192a6c34f0259f3b469)

##Trigger process configuration

| Name |  Default Value | Description |
|------|----------------|-------------|
| Trigger.Enabled        | true                | switch on/off the trigger |
| Trigger.StateMachineNS | trigger             | the namespace of the module (ns attribute) containing the definition of StateMachine |
| Trigger.StateMachine   | boot/SM/Trigger.xml | the bootstrap file containing the definition of StateMachine |
| Trigger.Transitions    | Warning,Timeout     | the list of Transition names used bz the Trigger |

##Actitivty properties used by the Trigger
Trigger uses the name of the Transition with On, Duration and Unit suffixes to find the data in Activity properties to create the Quartz Job.

| Name | Default Value | Description |
|------|---------------|-------------|
| ${TransitionName}On       | true | enable/disable Triggering of this Transiation for the given Activity |
| ${TransitionName}Duration | n/a  | Time when the Quartz Job is triggered |
| ${TransitionName}Unit     | n/a  | Values of org.quartz.DateBuilder.IntervalUnit enum |
