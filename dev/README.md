# dev
Development module to enable basic (crud level) management of Items

### Eclipse launch config for running cristal DSL of dev module

1. Edit Groovy/Compile settings in Eclipse Preferences
   1. Enable Script folder support
   1. Add 'src/main/module/org/cristalise/dev/**.groovy'
1. Open src/main/module/org/cristalise/dev/Module.groovy - do not skip this step, it is required for the next step to work
1. Right click 'Run As/Grooy Script' - this run normally fails
1. Edit generated runconfig by replacing content of the 'Arguments/Program Arguments' with these lines:
   `--main groovy.ui.GroovyMain --encoding UTF8 "${workspace_loc:cristalise-dev}/src/main/module/org/cristalise/dev/Module.groovy"`
1. On the Classpath tab 'Restore Default Entries' - restores maven dependencies

## Maven commands to scaffold Items and to DSL generation
- `mvn exec:java@scaffold -Dlogback.configurationFile=bin/logback-simple.xml`
- `mvn exec:java@module -Dlogback.configurationFile=bin/logback-simple.xml`

### Docker commands to setup development
- `docker pull postgres:16`
- `docker run -p 5432:5432 --name psql16 -e POSTGRES_PASSWORD=cristal -d postgres:16`
- `docker start psql16`
- `docker stop psql16`
