package io.bit

import net.sf.json.JSONObject
import groovy.json.JsonSlurper
import org.apache.tools.ant.util.Base64Converter
import java.net.HttpURLConnection

class RabbitMQJava{
    private String _host
    private String _port
    private String _adminLogin
    private String _adminPass

    RabbitMQJava(String host, String port, String login, String pass){
        _host = host
        _port = port
        _adminLogin = login
        _adminPass = pass
    }

    JSONObject createUser(String login, String pass){
        String url = "/api/users/" + login
        JSONObject data = new JSONObject()
        data.put("password", pass)
        data.put("tags", "monitoring")
        return exec(url, "PUT", data)
    }

    JSONObject deleteUser(String login, String pass){
        String url = "/api/users/" + login
        JSONObject data = new JSONObject()
        data.put("password", pass)
        data.put("tags", "monitoring")
        return exec(url, "DELETE", data)
    }

    JSONObject createVHost(String name){
        String url = "/api/vhosts/" + name
        return exec(url, "PUT")
    }
    
    JSONObject deleteVHost(String name){
        String url = "/api/vhosts/" + name
        return exec(url, "DELETE")
    }

    JSONObject addUserToVHost(String vhost, String user){
        String url = "/api/permissions/"+vhost+"/" + user
        JSONObject data = new JSONObject()
        data.put("configure", ".*")
        data.put("write", ".*")
        data.put("read", ".*")
        return exec( url, "PUT", data)
    }

    JSONObject exec(String u, String m, JSONObject body = null){
        JSONObject result = new JSONObject()
        String host = "http://" + _host + ":" + _port
        Base64Converter b64 = new Base64Converter()
        String base64_cred = b64.encode(_adminLogin + ":" + _adminPass)
        URL url = new URL(host + u)
        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.setRequestMethod(m)
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("Authorization", "Basic " + base64_cred)

        if(body != null){
            con.setDoOutput(true)
            OutputStream os = con.getOutputStream()
            os.write(body.toString().getBytes("UTF-8"))
            os.close()
        }
        InputStream inputStream = con.getErrorStream()
        if (inputStream == null) {
            inputStream = con.getInputStream()
        }
        if (inputStream.available() != 0){
            result = new JsonSlurper().parse(inputStream)
        }
        con.disconnect()
        return result
    }
}