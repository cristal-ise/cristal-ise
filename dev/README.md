# dev
Development module to enable basic (crud level) management of Items

### Eclipse launch config for running cristal DSL of dev module

1. Edit Groovy/Compile settings in Eclipse Preferences
   1. Enable Script folder support
   1. Add 'src/main/module/**.groovy'
1. Open src/mian/module/Module.groovy - do not skip this step, it is required for the next step to work
1. Right click 'Run As/Grooy Script' - this run normally fails
1. Edit generated runconfig by replacing content of the 'Arguments/Program Arguments' with these lines:
   ```
   --classpath "${workspace_loc:cristalise-dev}/target/classes;
   ${workspace_loc:cristalise-dev}/src/main/java;
   ${workspace_loc:cristalise-dev}/src/main/resources;
   ${workspace_loc:cristalise-dev}/src/main/groovy" 
   --main groovy.ui.GroovyMain --encoding UTF8 
   "${workspace_loc:cristalise-dev}/src/main/module/Module.groovy"
   ```
1. On the Classpath tab 'Restore Default Entries' - restores maven dependencies
