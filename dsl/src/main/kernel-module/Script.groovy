Script('DescriptionCollVersionLib', 0) {
    param('item',  'org.cristalise.kernel.entity.proxy.ItemProxy')
    param('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    param('job',   'org.cristalise.kernel.entity.agent.Job')
    output ('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', 'src/main/kernel-module/script/DescriptionCollVersionLib.js')
}

Script('CreateNewNumberedVersionFromLast', 0) {
    include('DescriptionCollVersionLib', 0)
    param('item',  'org.cristalise.kernel.entity.proxy.ItemProxy')
    param('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    param('job',   'org.cristalise.kernel.entity.agent.Job')
    output ('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', 'src/main/kernel-module/script/CreateNewNumberedVersionFromLast.js')
}

Script('SetLastNumberedVersionFromLast', 0) {
    include('DescriptionCollVersionLib', 0)
    param('item',  'org.cristalise.kernel.entity.proxy.ItemProxy')
    param('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
    param('job',   'org.cristalise.kernel.entity.agent.Job')
    output ('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('javascript', 'src/main/kernel-module/script/SetLastNumberedVersionFromLast.js')
}
