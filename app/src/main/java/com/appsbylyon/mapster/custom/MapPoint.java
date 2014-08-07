package com.appsbylyon.mapster.custom;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Created by Adam Lyon on 8/5/2014.
 */
public class MapPoint implements Serializable
{
    private static final long serialVersionUID = 1L;

    private double lat;
    private double lng;
    private double elevation = 0;

    public double getDistance()
    {
        return distance;
    }

    public void setDistance(double distance)
    {
        this.distance = distance;
    }

    private double distance = 0;

    public MapPoint(double latitude, double longitude, double elevation)
    {
        this.lat = latitude;
        this.lng = longitude;
        this.elevation = elevation;
    }

    public MapPoint(double latitude, double longitude)
    {
        this.lat = latitude;
        this.lng = longitude;
    }

    public LatLng getPoint()
    {
        return new LatLng(lat, lng);
    }

    public double getElevation()
    {
        return elevation;
    }

    public void setElevation(double elevation)
    {
        this.elevation = elevation;
    }

    public double getLng()
    {

        return lng;
    }

    public void setLng(double lng)
    {
        this.lng = lng;
    }

    public double getLat()
    {

        return lat;
    }

    public void setLat(double lat)
    {
        this.lat = lat;
    }
}
