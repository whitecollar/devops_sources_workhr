package io.bit

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovy.json.JsonSlurperClassic

// Возвращает строковое значения ключа по url из консула
//
// Параметры:
//  url - часть адреса, не включая сервер консула http://127.0.0.1:8500/v1/kv 
//  skip404Error (false by default) - если true, то вызывается исключение, если по url ничего не доступно
//
// Возвращаемое значение
//  String - значение по url. Если url неверный, то вызывается исключение
//
def queryVal(url, skip404Error = false) {

    validResponses = '200:600'

    consul_url = "http://127.0.0.1:8500/v1/kv/${url}"
    kvResponse = httpRequest acceptType: "TEXT_PLAIN", 
        url: consul_url,
        validResponseCodes: validResponses

    if (skip404Error && kvResponse.status == 404) {
        return null;
    } else if (kvResponse.status != 200) {
        utils = new Utils()
        utils.raiseError("Запрос в консул по адресу ${consul_url} выполнен с ошибкой. Статус: ${kvResponse.status}. Контент: ${kvResponse.content}")
    }

    kvJson = new JsonSlurperClassic().parseText(kvResponse.content)
    base64Val = kvJson[0].Value
    if (base64Val != null) {
        return new String(base64Val.decodeBase64())
    } else {
        return null
    }
}

// Удаляет значение ключа рекурсивно
//
// Параметры:
//  url - часть адреса, не включая сервер консула http://127.0.0.1:8500/v1/kv 
//  skip404Error (false by default) - если true, то вызывается исключение, если по url ничего не доступно
//
// Возвращаемое значение
//  String - ответ запроса. Всегда возвращается true
//
def deleteVal(url, skip404Error = false) {

    validResponses = '200:600'

    consul_url = "http://127.0.0.1:8500/v1/kv/${url}?recurse=true"
    kvResponse = httpRequest httpMode: 'DELETE', 
        acceptType: "TEXT_PLAIN", 
        url: consul_url,
        validResponseCodes: validResponses

    if (skip404Error && kvResponse.status == 404) {
        return null;
    } else if (kvResponse.status != 200) {
        utils = new Utils()
        utils.raiseError("Запрос в консул по адресу ${consul_url} выполнен с ошибкой. Статус: ${kvResponse.status}. Контент: ${kvResponse.content}")
    }
    
    return kvResponse.content
}

// Возвращает массив значений по url для проекта из консула
//
// Параметры:
//  url - часть адреса после адреса сервера
//  skip404Error (false by default) - если true, то вызывается исключение, если по url ничего не доступно
// 
// Возвращаемое значение
//  List - массив значений. Если url неверный, то вызывается исключение
//
def queryList(url, skip404Error = false) {
    
    validResponses = '200:600'

    consul_url = "http://127.0.0.1:8500/v1/kv/${url}/?keys=true&separator=/"
    kvResponse = httpRequest url: consul_url,
        validResponseCodes: validResponses

    if (skip404Error && kvResponse.status == 404) {
        return null;
    } else if (kvResponse.status != 200) {
        utils = new Utils()
        utils.raiseError("Запрос в консул по адресу ${consul_url} выполнен с ошибкой. Статус: ${kvResponse.status}. Контент: ${kvResponse.content}")
    }

    list kvJson = new JsonSlurperClassic().parseText(kvResponse.content)

    result = []
    for (def line : kvJson) {
        curLine = (String) line;
        val =  curLine.replaceAll(url, "").replaceAll("/", "");
        if (!val.isEmpty() && !result.contains(val))
            result.add(val.toLowerCase());
    }
    return result
}

// DEPRECATED Используйте вместо метод queryVal()
// Возвращает строковое значения ключа для проекта из консула
//
// Параметры:
//  projectKey - ключ проекта, для которого запрашивается параметр
//  url - часть адреса после ключа проекта
//
// Возвращаемое значение
//  String - значение по url. Если url неверный, то вызывается исключение
//
def queryValFromConsul(projectKey, url) {
    return queryVal("${projectKey}/${url}")
}

// Помещает строкове значение в консул по переданному url
//
// Параметры:
//  body - значение для помещение в консул
//  url - полный url в консуле для помещения значения
// 
def putVal(body, url) {
    validResponses = '200:600'

    consul_url = "http://127.0.0.1:8500/v1/kv"
    kvResponse = httpRequest httpMode: 'PUT', requestBody: body, 
        url: "${consul_url}/${url}",
        validResponseCodes: validResponses

    if(kvResponse.status != 200) {
        utils = new Utils()
        utils.raiseError("Запись в консул по адресу ${consul_url} выполнена с ошибкой. Статус: ${kvResponse.status}. Контент: ${kvResponse.content}")
    }
}

// Помещает строкове значение в консул по переданному url, только если подобный ключ не существует и он не пустой
//
// Параметры:
//  body - значение для помещение в консул
//  url - полный url в консуле для помещения значения
// 
def putValIfNotExists(body, url) {
    answer = queryVal(url, true)
    if (answer != null && !answer.isEmpty()) {
        return
    }
    putVal(body, url)
}

// Помещает JSON в консул по переданному url
//
// Параметры:
//  body - строка JSON для помещение в консул
//  url - полный url в консуле для помещения значения
// 
def putJSONToConsul(body, url) {
    httpRequest contentType: 'APPLICATION_JSON', httpMode: 'PUT', requestBody: body, url: url
}

//meta - строка в формате key1:value1,key2:value2...
def getNodes(meta = null){
	//meta = 'key1:value1,key2:value2'
	consul_url = "http://127.0.0.1:8500/v1/catalog/nodes?"
	if(meta != null){
		String[] m = meta.split(',')
		for (int i = 0; i < m.length; i++)
		{
			consul_url += "node-meta=" + m[i]+ "&"
		}
	}
	response = httpRequest url: consul_url, httpMode: 'GET'
	return response
}

def updateConfig(projectKey, role, node){
    JSONObject obj = new JSONObject()
	//TODO Переписать https://j.bit-erp.ru/browse/DEVOPS-578
	if(node=="nb010130"){
		obj.put("bind_addr", "172.16.250.50")
	}
	if(node=="devsespel"){
		obj.put("bind_addr", "172.16.50.37")
	}
    obj.put("datacenter", "dc1")
    obj.put("encrypt", "LZeK7bMuQ+O6DkOXgXi2Fw==")
    obj.put("data_dir", "var/consul")
    obj.put("log_level", "INFO")
    obj.put("enable_debug", true)
	obj.put("server", false)
    obj.put("leave_on_terminate", false)
    obj.put("skip_leave_on_interrupt", true)
    obj.put("rejoin_after_leave", true)
	//TODO Переделать на ДНС https://j.bit-erp.ru/browse/DEVOPS-578
    JSONArray joinAddress = new JSONArray()
    joinAddress.add("172.16.50.27")
    joinAddress.add("172.16.50.71")
    joinAddress.add("172.16.50.31")
    obj.put("retry_join", joinAddress)

	Map<String, List<String>> servicesForRole = new HashMap<String, List<String>>()
	servicesForRole.put("rabbitmq", null)
	List<String> prjServices = new ArrayList<String>()
	prjServices.add("templateBases")
	servicesForRole.put("prj", prjServices)
	servicesForRole.put("work", null)
	servicesForRole.put("plain", null)
	servicesForRole.put("sql", null)
    JSONArray services = new JSONArray()
	if(servicesForRole.containsKey(role)){
		for(String s in servicesForRole.get(role)){
			JSONObject service = new JSONObject()
			service.put("name", s)
			JSONArray checksForService = getChecksForService(s, node, projectKey)
			service.put("checks", checksForService)

			services.add(service)
		}
	}


    obj.put("services", services)

	return obj.toString()
}

JSONArray getChecksForService(String service, String node, String projectKey){
	JSONArray checksForService = new JSONArray()
	if(service == "templateBases"){
		def bases = queryList("${projectKey}/templatebases", true)
		for(def b in bases){
			JSONObject check = new JSONObject()
			check.put("id", "checkBase " + b)
			check.put("name", "checkBase " + b)
			def baseName = queryVal("${projectKey}/templatebases/${b}/base")
			def connectionString = queryVal("${projectKey}/templatebases/${b}/connection_string")
			def platform = queryVal("${projectKey}/project_server_platform")
			check.put("args", ['C:/consul/consulChecks/templateBasesCheck.bat', node, baseName, connectionString, platform])
			check.put("interval", "60s")
			checksForService.add(check)
		}
	}
	return checksForService
}
return this