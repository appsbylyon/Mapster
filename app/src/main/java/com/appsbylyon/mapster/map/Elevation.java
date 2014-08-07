package com.appsbylyon.mapster.map;


import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by infinite on 8/5/2014.
 */
public class Elevation
{
    private static final String API_KEY = "AIzaSyAbj50kLhQPCpEQfNusUAZFgo38DtqPQXQ";
    private static final String E_URL_FRONT = "https://maps.googleapis.com/maps/api/elevation/xml?locations=";
    private static final String E_URL_END = "&key="+API_KEY;

    private static final double METERS_TO_FEET = 3.28084;

    private String resultText;

    public Document getDocument(LatLng pos)
    {
        String url = E_URL_FRONT+pos.latitude+","+pos.longitude+E_URL_END;
        Log.d("Get Elevation Data", url);
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost(url);
            HttpResponse response = httpClient.execute(httpPost, localContext);
            InputStream in = response.getEntity().getContent();
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(in);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean isOk(Document doc)
    {
        NodeList nodeList1 = doc.getElementsByTagName("status");
        Node node1 = nodeList1.item(0);
        resultText = node1.getTextContent();
        Log.e("Get Elevation", "Return Status: "+resultText);
        if (resultText.equalsIgnoreCase("ok"))
        {
            return true;
        }
        return false;
    }

    public double getElevation(Document doc)
    {
        NodeList nodeList1 = doc.getElementsByTagName("elevation");
        Node node1 = nodeList1.item(0);
        double elevation = -15000;
        try
        {
            elevation = round ((METERS_TO_FEET *Double.parseDouble(node1.getTextContent())), 2, BigDecimal.ROUND_HALF_UP);
            Log.i("Get Elevation", "Elevation Value: "+elevation);
        }
        catch (NumberFormatException NFE)
        {
            Log.e("Get Elevation", "Could Not Parse Elevation Data: "+node1.getTextContent());
        }
        return elevation;
    }

    public static double round(double unrounded, int precision, int roundingMode)
    {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }
}

