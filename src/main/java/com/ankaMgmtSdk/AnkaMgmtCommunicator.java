package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by asafgur on 09/05/2017.
 */
public class AnkaMgmtCommunicator {


    private final String host;
    private final String port;
    private String scheme;

    public AnkaMgmtCommunicator(String host, String port) throws AnkaMgmtException {
        this.host = host;
        this.port = port;
        this.scheme = "https";
        try {
            String url = String.format("%s://%s:%s", this.scheme, this.host, this.port);
            this.doRequest(RequestMethod.GET, url);
        } catch (SSLException e) {
            this.scheme = "http";
        } catch (IOException e) {
            e.printStackTrace();
            throw new AnkaMgmtException(e);
        }
        this.listTemplates();
    }

    public List<AnkaVmTemplate> listTemplates() throws AnkaMgmtException {
        List<AnkaVmTemplate> templates = new ArrayList<AnkaVmTemplate>();
        String url = String.format("%s://%s:%s/api/v1/registry/vm", this.scheme, this.host, this.port);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("body");
                for (Object j: vmsJson) {
                    JSONObject jsonObj = (JSONObject) j;
                    String vmId = jsonObj.getString("id");
                    String name = jsonObj.getString("name");
                    AnkaVmTemplate vm = new AnkaVmTemplate(vmId, name);
                    templates.add(vm);
                }
            }
        } catch (IOException e) {
            return templates;
        }
        return templates;
    }

    public List<String> getTemplateTags(String templateId) throws AnkaMgmtException {
        List<String> tags = new ArrayList<String>();
        String url = String.format("%s://%s:%s/api/v1/registry/vm?id=%s", this.scheme, this.host, this.port, templateId);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject templateVm = jsonResponse.getJSONObject("body");
                JSONArray vmsJson = templateVm.getJSONArray("versions");
                for (Object j: vmsJson) {
                    JSONObject jsonObj = (JSONObject) j;
                    String tag = jsonObj.getString("tag");
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            return tags;
        }
        return tags;
    }

    public String startVm(String templateId, String tag) throws AnkaMgmtException {
        String url = String.format("%s://%s:%s/api/v1/vm", this.scheme, this.host, this.port);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("vmid", templateId);
        if (tag != null) {
            jsonObject.put("tag", tag);
        }
        JSONObject jsonResponse = null;
        try {
            jsonResponse = this.doRequest(RequestMethod.POST, url, jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String logicalResult = jsonResponse.getString("status");
        if (logicalResult.equals("OK")) {
            JSONArray uuidsJson = jsonResponse.getJSONArray("body");
            if (uuidsJson.length() >= 1 ){
                return uuidsJson.getString(0);
            }

//            return jsonResponse.getString("body");
        }
        return null;
    }

    public AnkaVmSession showVm(String sessionId) throws AnkaMgmtException {
        String url = String.format("%s://%s:%s/api/v1/vm?id=%s", this.scheme, this.host, this.port, sessionId);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                JSONObject body = jsonResponse.getJSONObject("body");
                return new AnkaVmSession(sessionId, body);
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean terminateVm(String sessionId) throws AnkaMgmtException {
        String url = String.format("%s://%s:%s/api/v1/vm", this.scheme, this.host, this.port);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", sessionId);
            JSONObject jsonResponse = this.doRequest(RequestMethod.DELETE, url, jsonObject);
            String logicalResult = jsonResponse.getString("status");
            if (logicalResult.equals("OK")) {
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private List<String> list() throws AnkaMgmtException {
        List<String> vmIds = new ArrayList<String>();
        String url = String.format("%s://%s:%s/list", this.scheme, this.host, this.port);
        try {
            JSONObject jsonResponse = this.doRequest(RequestMethod.GET, url);
            String logicalResult = jsonResponse.getString("result");
            if (logicalResult.equals("OK")) {
                JSONArray vmsJson = jsonResponse.getJSONArray("instance_id");
                for (int i = 0; i < vmsJson.length(); i++) {
                    String vmId = vmsJson.getString(i);
                    vmIds.add(vmId);
                }
            }
            return vmIds;
        } catch (IOException e) {
            return vmIds;
        }
    }

    private enum RequestMethod {
        GET, POST, DELETE
    }

    private JSONObject doRequest(RequestMethod method, String url) throws IOException, AnkaMgmtException {
        return doRequest(method, url, null);
    }

    private JSONObject doRequest(RequestMethod method, String url, JSONObject requestBody) throws IOException, AnkaMgmtException {
    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    HttpRequestBase request;
    try {
        switch (method) {
            case POST:
                HttpPost postRequest = new HttpPost(url);
                request = setBody(postRequest, requestBody);
                break;
            case DELETE:
                HttpDeleteWithBody delRequest = new HttpDeleteWithBody(url);
                request = setBody(delRequest, requestBody);
                break;
            case GET:
                request = new HttpGet(url);
                break;
            default:
                request = new HttpGet(url);
                break;
        }

        HttpResponse response = httpClient.execute(request);
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != 200) {
            System.out.println(response.toString());
            return null;
        }
        HttpEntity entity = response.getEntity();
        if ( entity != null ) {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(entity.getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonResponse = new JSONObject(result.toString());
            return jsonResponse;
        }

    } catch (HttpHostConnectException e) {
        throw new AnkaMgmtException(e);
    } catch (SSLException e) {
        throw e;
    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    } catch (IOException e) {
        e.printStackTrace();
        throw new AnkaMgmtException(e);
    } finally {
        httpClient.close();
    }
    return null;
}

    private HttpRequestBase setBody(HttpEntityEnclosingRequestBase request, JSONObject requestBody) throws UnsupportedEncodingException {
        request.setHeader("content-type", "application/json");
        StringEntity body = new StringEntity(requestBody.toString());
        request.setEntity(body);
        return request;
    }

    class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }


}
