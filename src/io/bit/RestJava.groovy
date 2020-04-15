package io.bit

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import net.sf.json.JSONObject
import groovy.json.JsonSlurper

public class RestJava {

    public enum ResponseTypes {AS_JSON, AS_TEXT}

    public static JSONObject exec(String host, String base64_cred, String urlpath, String method, JSONObject body = null, ResponseTypes responseType = ResponseTypes.AS_JSON) {

        URL url = new URL(host + urlpath)

        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.setRequestMethod(method)
        con.setRequestProperty("Accept", "application/json")
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("Authorization", "Basic " + base64_cred)

        if (body != null) {
            con.setDoOutput(true)
            OutputStream os = con.getOutputStream()
            os.write(body.toString().getBytes("UTF-8"))
            os.close()
        }

        InputStream inputStream = con.getErrorStream()
        if (inputStream == null) {
            inputStream = con.getInputStream()
        }

        JSONObject result = null
        if (responseType == ResponseTypes.AS_JSON) {
            result = new JsonSlurper().parse(inputStream)
        } else if (responseType == ResponseTypes.AS_TEXT) {
            throw new IllegalArgumentException("Not implemented!")
        }

        con.disconnect()
        return result
    }
}