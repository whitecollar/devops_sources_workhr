#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils

def consul = new Consul()
def utils = new Utils()

pipeline {

	parameters {
		string(defaultValue: "", description: '', name: 'serverName')
		string(defaultValue: "", description: '', name: 'serverRole')
		string(defaultValue: "", description: '', name: 'projectKey')
		string(defaultValue: "", description: 'Необязательный. Системный параметр, содержит код заявки в SD', name: 'issueKey')
	}

	agent {
		label "${serverName}service"
	}

	options {
		timeout(time: 60000, unit: 'SECONDS')
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}

	stages {

		stage("Подготовка параметров") {
			steps {
				timestamps {
					script {
						def config = consul.updateConfig(projectKey, serverRole, serverName)
						echo config
						def pathToConfig = "C:\\consul\\etc\\consul.d\\client-config.json"
						def pathToConfigForCheck = "C:\\consul\\etc\\consul.d\\client-config-check.json"
						utils.cmd("mkdir C:\\consul\\consulChecks\\")
						echo utils.cmdOut("COPY ${workspace}\\software-installation\\consulChecks\\* C:\\consul\\consulChecks\\")
						writeFile(file: pathToConfigForCheck, text: config, encoding: "UTF-8")
						def checkConfigResult = utils.cmdOut("C:\\consul\\consul.exe validate ${pathToConfigForCheck}")
						if(checkConfigResult.contains("Configuration is valid")){
							writeFile(file: pathToConfig, text: config, encoding: "UTF-8")
							echo utils.cmdOut("DEL ${pathToConfigForCheck}")
							def metas = [:]
							metas.put("projectKey", projectKey)
							metas.put("serverRole", serverRole)
							def metaParam = ""
							metas.each{k, v -> metaParam += " -node-meta=${k}:${v}"}
							utils.cmd("powershell -file ${env.WORKSPACE}\\servers-maintenance\\consul_reinstall.ps1 \"${metaParam}\"")
						}else{
							utils.raiseError("Неправильно собрана конфигурация, проверьте этот json - ${config}")
						}
					}
				}
			}
		}
	}
}