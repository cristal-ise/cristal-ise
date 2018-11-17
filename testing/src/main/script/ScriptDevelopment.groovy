@BaseScript(org.cristalise.dsl.scripting.ScriptDevelopment)
import groovy.transform.BaseScript

setConfig  'src/main/bin/client.conf'
setConnect 'src/main/bin/integTest.clc'

setUser 'dev'
setPwd  'test'

WriteScriptHere('/desc/ActivityDesc/kernel/ManageSchema', 'EditDefinition') {
    assert agent
    assert item
    assert job
}
