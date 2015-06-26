package com.twilio.example.basicphone;

import android.util.Log;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class webConnection {
    user parentAct;
    private String TAG = "Ringer";
    public webConnection(user act) {
        this.parentAct = act;

    }

    public void post(String url,String parameters){


        // create client and post object
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
        nameValuePair.add(new BasicNameValuePair("parameters", parameters));

        //We need to encode our data into valid URL format before making HTTP request.

       //Encoding POST data
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));

        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        // execute the post
        try {
            HttpResponse response = httpClient.execute(httpPost);

            Log.d(TAG,"Response of GET request" + response.toString());

        }
        catch (ClientProtocolException e) {
            // Log exception
            e.printStackTrace();
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
        }

        // remember Make sure you add Internet permission to your app before making such requests else you might see some errors.
        // You can add Internet permissions by adding this line to your manifest file above application tag.
        // <uses-permission android:name="android.permission.INTERNET" />

    }
    public void get(String url,String query) {
        String response_text = null;

        HttpEntity entity = null;

        //Create an object of HttpClient

        HttpClient client = new DefaultHttpClient();
        // Create an object of HttpGet

        HttpGet request = new HttpGet(url + "?parameters=" + query);

        // Finally make HTTP request

        HttpResponse response;
        try {
            response = client.execute(request);
            entity = response.getEntity();

            String responseText = getResponseBody(entity);
            parentAct.handleWebResponse(responseText);

            //Log.d(TAG,"Response of GET request" + responseText);//response.getEntity().toString());
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }


    static String getResponseBody(final HttpEntity entity) throws IOException, ParseException {

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }

        InputStream instream = entity.getContent();

        if (instream == null) {
            return "";

        }

        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(

                "HTTP entity too large to be buffered in memory");
        }
        String charset = HTTP.DEFAULT_CONTENT_CHARSET;

/*
        String charset = getContentCharSet(entity);

        if (charset == null) {

            charset = HTTP.DEFAULT_CONTENT_CHARSET;

        }
*/
        Reader reader = new InputStreamReader(instream, charset);

        StringBuilder buffer = new StringBuilder();

        try {

            char[] tmp = new char[1024];

            int l;

            while ((l = reader.read(tmp)) != -1) {

                buffer.append(tmp, 0, l);

            }

        } finally {

            reader.close();

        }

        return buffer.toString();

    }

    public String getContentCharSet(final HttpEntity entity) throws ParseException {

        if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }

        String charset = null;

        if (entity.getContentType() != null) {

            HeaderElement values[] = entity.getContentType().getElements();

            if (values.length > 0) {

                NameValuePair param = values[0].getParameterByName("charset");

                if (param != null) {

                    charset = param.getValue();

                }

            }

        }

        return charset;

    }
}
