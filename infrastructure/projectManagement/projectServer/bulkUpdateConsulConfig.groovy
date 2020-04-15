#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import net.sf.json.JSONArray
import net.sf.json.JSONSerializer

def consul = new Consul()

pipeline {

	parameters {
		string(defaultValue: "", description: '', name: 'serverRole')
		string(defaultValue: "", description: '', name: 'projectKey')
		string(defaultValue: "", description: 'Необязательный. Системный параметр, содержит код заявки в SD', name: 'issueKey')
	}

	agent {
		label "master_node"
	}

	options {
		timeout(time: 60000, unit: 'SECONDS')
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}

	stages {
		stage('Перед запуском сборки') {
			steps {
				timestamps {
					script {
						projectHelpers.beforeStartJob()
					}
				}
			}
		}
		stage("Подготовка параметров") {
			steps {
				timestamps {
					script {
						def searchMeta = []
						if(serverRole != null && serverRole != ""){
							searchMeta.add("serverRole:${serverRole}")
						}
						if(projectKey != null && projectKey != ""){
							searchMeta.add("projectKey:${projectKey}")
						}
						echo searchMeta.join(",")
						def consulRes = consul.getNodes(searchMeta.join(",")).content
						echo consulRes
						JSONArray j = (JSONArray)JSONSerializer.toJSON(consulRes)
						def jobtasks = [:]
						for(int i = 0; i < j.size(); i++){
							echo j[i].getString("Node")
							def server = j[i].getString("Node")
							def pKey = j[i].getJSONObject("Meta").getString("projectKey")
							def role = j[i].getJSONObject("Meta").getString("serverRole")
							jobtasks["bulkUpdateConsulConfig ${server}"] = updateConfig(server, pKey, role)
						}
						parallel jobtasks
					}
				}
			}
		}
	}
	post {
		always {
			script {
				projectHelpers.beforeEndJob()
			}
		}
	}
}

def updateConfig(serverName, projectKey, serverRole) {
	return {
		node ("${serverName}service") {
			stage("Обновление конфига на ${serverName}") {
				timestamps {
					checkout scm
					Consul consul = new Consul()
					Utils utils = new Utils()
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