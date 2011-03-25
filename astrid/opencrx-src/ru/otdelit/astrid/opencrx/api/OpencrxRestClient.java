package ru.otdelit.astrid.opencrx.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * RestClient allows Android to consume web requests.
 * <p>
 * Portions by Praeda:
 * http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple
 * -restful-client-at-android/
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OpencrxRestClient{

    private static final int HTTP_OK = 200;

    private static final int TIMEOUT_MILLIS = 30000;

    private static HttpClient httpClient = null;

     private synchronized static void initializeHttpClient() {
        if (httpClient == null) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MILLIS);
            HttpConnectionParams.setSoTimeout(params, TIMEOUT_MILLIS);
            httpClient = new DefaultHttpClient(params);
        }
    }

    // IKARI
    private synchronized static void addCredentials(String login, String password){
        if (httpClient == null)
            return;

        ((AbstractHttpClient) httpClient).getCredentialsProvider().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(login, password));

    }

    /**
     * ATTENTION: Don't forget to close InputStream before call RestClient methods!!!
     * @param response
     * @return
     * @throws IOException
     * @throws ApiServiceException
     */
    private InputStream processHttpResponse(HttpResponse response) throws IOException, ApiServiceException {
        HttpEntity entity = response.getEntity();
        InputStream contentStream = null;
        if (entity != null) {
            contentStream = entity.getContent();
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode != HTTP_OK ) {
            ApiServiceException error;

            if (statusCode == 204)
                return new ByteArrayInputStream(new byte[0]);

            if(statusCode == 403)
                error = new ApiServiceException(response.getStatusLine().toString());
            else if(statusCode == 401)
                error = new ApiAuthenticationException(response.getStatusLine().getReasonPhrase());
            else
                error = new ApiServiceException(response.getStatusLine().toString());

            throw error;
        }

        if (contentStream != null)
            return contentStream;
        else
            return new ByteArrayInputStream(new byte[0]);
    }

    /**
     * Issue an HTTP GET for the given URL, return the response
     * using given authorization credentials.
     *
     * NOTE: close stream before using RestClient again
     *
     * @param url url with url-encoded params
     * @param opencrxLogin login for OpenCRX REST service
     * @param opencrxPassword password for OpenCRX REST service
     * @return response stream, or null if there was no response
     * @throws IOException
     */
    public synchronized InputStream get(String url, String opencrxLogin, String opencrxPassword) throws IOException {
        initializeHttpClient();

        addCredentials(opencrxLogin, opencrxPassword);

        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);

            return processHttpResponse(response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    @SuppressWarnings("nls")
    public synchronized InputStream post(String url, String data, String login, String password) throws IOException {
        initializeHttpClient();

        addCredentials(login, password);

        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/xml;charset=UTF-8");
            httpPost.setEntity(new StringEntity(data, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPost);

            return processHttpResponse(response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }


    public synchronized InputStream delete(String url , String login, String password) throws IOException{
        initializeHttpClient();

        addCredentials(login, password);

        try{
            HttpDelete httpDelete = new HttpDelete(url);
            HttpResponse resp = httpClient.execute(httpDelete);

            return processHttpResponse(resp);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    @SuppressWarnings("nls")
    public synchronized InputStream put(String url, String data, String login, String password) throws IOException {
        initializeHttpClient();

        addCredentials(login, password);

        try {
            HttpPut httpPut = new HttpPut(url);
            httpPut.addHeader("Content-Type", "application/xml;charset=UTF-8");
            httpPut.setEntity(new StringEntity(data, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPut);

            return processHttpResponse(response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }


    /**
     * Destroy and re-create http client
     */
    public void reset() {
        httpClient = null;
    }

}
