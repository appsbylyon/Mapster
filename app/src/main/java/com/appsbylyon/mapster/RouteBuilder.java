package com.appsbylyon.mapster;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.appsbylyon.mapster.custom.MapPoint;
import com.appsbylyon.mapster.custom.Route;
import com.appsbylyon.mapster.custom.ValueLabelAdapter;
import com.appsbylyon.mapster.map.Elevation;
import com.fima.chartview.ChartView;
import com.fima.chartview.LinearSeries;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;

import java.util.ArrayList;

/**
 * Created by infinite on 8/5/2014.
 */
public class RouteBuilder extends Activity implements GoogleMap.OnMapClickListener, View.OnClickListener,
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMyLocationButtonClickListener
{
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private static final String TAG = "Route Builder";

    private static final int ROUTE_WIDTH = 5;
    private static final int ROUTE_COLOR = Color.BLUE;

    private static final double METERS_TO_MILES = 0.000621371;

    private ToggleButton addPointsButton;
    private ToggleButton showMarkersButton;
    private ToggleButton autoButton;
    private Button undoButton;
    private Button saveButton;

    private TextView distanceLabel;

    private ChartView chartView;

    private LinearSeries elevationSeries = new LinearSeries();

    private GoogleMap mMap;

    private LocationClient mLocationClient;

    private InputMethodManager imm;

    private Route route = new Route();

    private Bitmap marker;

    private ArrayList<MarkerOptions> markers = new ArrayList<MarkerOptions>();

    PolylineOptions routeLine = new PolylineOptions().width(ROUTE_WIDTH).color(ROUTE_COLOR);

    private boolean addPointsEnabled = false;
    private boolean showMarkers = false;
    private boolean autoMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_build_route);

        Bitmap bigMarker = BitmapFactory.decodeResource(getResources(), R.drawable.blue_marker);
        marker = Bitmap.createScaledBitmap(bigMarker, 40, 80, false);

        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        chartView = (ChartView) findViewById(R.id.build_route_chart);

        elevationSeries.setLineColor(0xFF0099CC);
        elevationSeries.setLineWidth(2);

        distanceLabel = (TextView) findViewById(R.id.build_route_distance_label);
        distanceLabel.setText(getString(R.string.build_route_no_distance_text));

        addPointsButton = (ToggleButton) findViewById(R.id.build_route_add_points_toggle_button);
        addPointsButton.setOnClickListener(this);

        showMarkersButton = (ToggleButton) findViewById(R.id.build_route_show_markers_toggle_button);
        showMarkersButton.setOnClickListener(this);

        autoButton = (ToggleButton) findViewById(R.id.build_route_auto_toggle_button);
        autoButton.setOnClickListener(this);

        undoButton = (Button) findViewById(R.id.build_route_undo_button);
        undoButton.setOnClickListener(this);

        this.setUpMapIfNeeded();
    }

    @Override
    public void onClick(View view)
    {
        Log.i(TAG, "On Clicked Called");
        int id = view.getId();
        switch (id)
        {
            case R.id.build_route_add_points_toggle_button:
                Log.i(TAG, "Add Points Button Clicked");
                if (addPointsButton.isChecked()){addPointsEnabled = true;}
                else{addPointsEnabled = false;}
                break;
            case R.id.build_route_show_markers_toggle_button:
                if (showMarkersButton.isChecked())
                {
                    showMarkers = true;
                    placeMarkers();
                }
                else
                {
                    showMarkers = false;
                    clearMarkers();
                }
                break;
            case R.id.build_route_auto_toggle_button:
                if (autoButton.isChecked()){autoMode = true;}
                else{autoMode = false;}
                break;
            case R.id.build_route_undo_button:
                this.undoLastPoint();
                break;
        }
    }

    private void clearMarkers()
    {
        mMap.clear();
        if(markers.size() > 0)
        {
            mMap.addPolyline(routeLine);
            mMap.addMarker(markers.get(0));
            if(markers.size() > 1){mMap.addMarker(markers.get(markers.size()-1));}
        }
    }

    private void placeMarkers()
    {
        mMap.clear();
        mMap.addPolyline(routeLine);
        for (int i = 0; i < markers.size(); i++){mMap.addMarker(markers.get(i));}
    }

    private void updateChartElevation()
    {
        elevationSeries.addPoint(new LinearSeries.LinearPoint(route.getPoints().size(),
                route.getPoints().get(route.getPoints().size()-1).getElevation()));
        chartView.addSeries(elevationSeries);
        chartView.setLeftLabelAdapter(new ValueLabelAdapter(this, ValueLabelAdapter.LabelOrientation.VERTICAL));
        chartView.setBottomLabelAdapter(new ValueLabelAdapter(this, ValueLabelAdapter.LabelOrientation.HORIZONTAL));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mLocationClient != null) {mLocationClient.disconnect();}
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        mLocationClient.connect();
    }

    private void setUpMapIfNeeded()
    {
        if (mMap == null)
        {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(this);
                mMap.setOnMapClickListener(this);
            }
        }
    }

    private void setUpLocationClientIfNeeded()
    {
        if (mLocationClient == null){mLocationClient = new LocationClient(getApplicationContext(),this, this);}
    }

    @Override
    public void onMapClick(LatLng latLng)
    {
        if (!autoMode)
        {
            if (addPointsEnabled)
            {
                if (mMap != null)
                {
                    double latitude = latLng.latitude;
                    double longitude = latLng.longitude;
                    if (!showMarkers)
                    {
                        mMap.clear();
                        if (markers.size() > 1)
                        {
                            mMap.addMarker(markers.get(0));
                        }
                    }
                    routeLine.add(latLng);
                    mMap.addPolyline(routeLine);
                    route.getPoints().add(new MapPoint(latitude, longitude));
                    getDistance();
                    new GetElevationData().execute(latitude, longitude, (double) (route.getPoints().size() - 1));
                }
            }
        }
    }

    private void addMarker(MapPoint mapPoint)
    {
        MarkerOptions newMarker = new MarkerOptions()
                .title(route.getPoints().size()+": E:"+String.format("%.02f",mapPoint.getElevation())
                        +"ft D:"+String.format("%.03f", mapPoint.getDistance())+"mi")
                .position(mapPoint.getPoint())
                .icon(BitmapDescriptorFactory.fromBitmap(this.marker));
        markers.add(newMarker);
        if (markers.size() == 1)
        {
            mMap.addMarker(markers.get(0));
        }
        mMap.addMarker(newMarker);
    }

    private void undoLastPoint()
    {
        if (route.getPoints().size()>0)
        {
            route.getPoints().remove(route.getPoints().size()-1);
            markers.remove(markers.size() - 1);
            elevationSeries = new LinearSeries();
            elevationSeries.setLineColor(0xFF0099CC);
            elevationSeries.setLineWidth(2);
            routeLine = new PolylineOptions().width(ROUTE_WIDTH).color(ROUTE_COLOR);
            mMap.clear();
            chartView.clearSeries();
            if (route.getPoints().size() > 0)
            {
                for (int i = 0; i < route.getPoints().size(); i++)
                {
                    elevationSeries.addPoint(new LinearSeries.LinearPoint(i + 1,
                            route.getPoints().get(i).getElevation()));
                    routeLine.add(route.getPoints().get(i).getPoint());
                    if (showMarkers){mMap.addMarker(markers.get(i));}
                }
                if (!showMarkers)
                {
                    mMap.addMarker(markers.get(0));
                    if (markers.size()>1){mMap.addMarker(markers.get(markers.size()-1));}
                }
                chartView.addSeries(elevationSeries);
                chartView.setLeftLabelAdapter(new ValueLabelAdapter(this, ValueLabelAdapter.LabelOrientation.VERTICAL));
                chartView.setBottomLabelAdapter(new ValueLabelAdapter(this, ValueLabelAdapter.LabelOrientation.HORIZONTAL));
                mMap.addPolyline(routeLine);
            }
            setDistanceLabel();
        }
        else{Toast.makeText(this, "No Points To Undo!", Toast.LENGTH_SHORT).show();}
    }



    private void getDistance()
    {
        if (route.getPoints().size() >= 2 )
        {
            float[] results = new float[2];
            LatLng point1 = route.getPoints().get(route.getPoints().size()-1).getPoint();
            LatLng point2 = route.getPoints().get(route.getPoints().size()-2).getPoint();
            Location.distanceBetween(point1.latitude, point1.longitude,
                    point2.latitude, point2.longitude, results);
            double distance = results[0] * METERS_TO_MILES;
            route.getPoints().get(route.getPoints().size()-1).setDistance(distance);
            setDistanceLabel();
        }
    }

    private void setDistanceLabel()
    {
        distanceLabel.setText(getString(R.string.build_route_has_distance_text)+" "+String.format("%.02f", route.getDistance())+" miles");
    }

    private class GetElevationData extends AsyncTask<Double, Double, Void>
    {
        @Override
        protected Void doInBackground(Double... locData)
        {
            LatLng location = new LatLng(locData[0], locData[1]);
            double point = locData[2];
            double elevationVal = 0;
            Elevation elevation = new Elevation();
            Document results = elevation.getDocument(location);
            if (results != null)
            {
                if (elevation.isOk(results)){elevationVal = elevation.getElevation(results);}
                else{Log.e("Get Elevation", "Status NOT Okay");}
            }
            else{Log.e("Get Elevation", "Result Document is NULL");}

            this.publishProgress(elevationVal, point);
            return null;
        }

        public void onProgressUpdate(Double ... results)
        {
            double elevation = results[0];
            int point = (int) Math.floor(results[1]);
            route.getPoints().get(point).setElevation(elevation);
            updateChartElevation();
            addMarker(route.getPoints().get(point));
        }
    }


    @Override
    public void onConnected(Bundle bundle){}

    @Override
    public void onDisconnected(){}

    @Override
    public void onLocationChanged(Location location){}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){}


    @Override
    public boolean onMyLocationButtonClick(){return false;}

}
