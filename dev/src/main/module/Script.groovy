Script('CollDescCreator', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.agent.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/CollDescCreator.js")
}

Script('InstantiateAgent', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.agent.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/InstantiateAgent.js")
}

Script('InstantiateItem', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.agent.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/InstantiateItem.js")
}

Script('LocalObjectDefCreator', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.agent.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/LocalObjectDefCreator.js")
}

Script('New', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.agent.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/New.js")
}

Script('SetWorkflow', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    input('job', 'org.cristalise.kernel.entity.agent.Job')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', "src/main/module/script/SetWorkflow.js")
}
    