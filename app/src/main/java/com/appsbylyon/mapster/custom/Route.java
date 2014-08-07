package com.appsbylyon.mapster.custom;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by infinite on 8/5/2014.
 */
public class Route implements Serializable
{
    private static final long serialVersionUID = 1L;

    private ArrayList<MapPoint> points = new ArrayList<MapPoint>();

    private String routeTitle;

    public double getDistance()
    {
        double distance = 0;
        for (MapPoint point: points)
        {
            distance += point.getDistance();
        }
        return distance;
    }

    public ArrayList<MapPoint> getPoints()
    {
        return points;
    }

    public String getRouteTitle()
    {
        return routeTitle;
    }

    public void setRouteTitle(String routeTitle)
    {
        this.routeTitle = routeTitle;
    }
}
