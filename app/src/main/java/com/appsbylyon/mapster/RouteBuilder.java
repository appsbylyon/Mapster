package com.appsbylyon.mapster;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.appsbylyon.mapster.custom.CustomAutoCompleteAdapter;
import com.appsbylyon.mapster.custom.MapPoint;
import com.appsbylyon.mapster.custom.ResultBundle;
import com.appsbylyon.mapster.custom.Route;
import com.appsbylyon.mapster.custom.ValueLabelAdapter;
import com.appsbylyon.mapster.map.Elevation;
import com.appsbylyon.mapster.map.GMapV2Direction;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by infinite on 8/5/2014.
 */
public class RouteBuilder extends Activity implements GoogleMap.OnMapClickListener, View.OnClickListener,
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMyLocationButtonClickListener, AdapterView.OnItemClickListener
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
    private ToggleButton searchButton;

    private Button undoButton;
    private Button saveButton;
    private Button clearButton;

    private TextView distanceLabel;

    private AutoCompleteTextView searchBar;

    private ChartView chartView;

    private LinearSeries elevationSeries = new LinearSeries();

    private GoogleMap mMap;

    private LocationClient mLocationClient;

    private Route route = new Route();

    private Bitmap marker;

    private ArrayList<MarkerOptions> markers = new ArrayList<MarkerOptions>();

    private List<Address> addressResults = new ArrayList<Address>();
    private ArrayList<String> results = new ArrayList<String>();

    private InputMethodManager imm;

    private long lastSearch;

    PolylineOptions routeLine = new PolylineOptions().width(ROUTE_WIDTH).color(ROUTE_COLOR);

    private boolean addPointsEnabled = false;
    private boolean showMarkers = false;
    private boolean autoMode = false;
    private boolean isAutoAddingPoints = false;

    private int currAutoPoint = 0;
    private int searchBarHeight = -1;
    private int clearButtonHeight = -1;

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

        searchButton = (ToggleButton) findViewById(R.id.build_route_search_toggle_button);
        searchButton.setOnClickListener(this);

        undoButton = (Button) findViewById(R.id.build_route_undo_button);
        undoButton.setOnClickListener(this);

        clearButton = (Button) findViewById(R.id.build_route_clear_button);
        clearButton.setOnClickListener(this);


        searchBar = (AutoCompleteTextView) findViewById(R.id.build_route_search_bar);
        searchBar.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void afterTextChanged(Editable view){}

            @Override
            public void beforeTextChanged(CharSequence text, int arg1, int arg2, int arg3){}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count)
            {
                lastSearch = System.currentTimeMillis();
                new SearchAddress().execute(text.toString().trim(), Long.toString(lastSearch));
            }

        });

        searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean b)
            {
                if (searchBar.hasFocus())
                {
                    String searchBarText = searchBar.getText().toString().trim();
                    if (searchBarText.length() > 0)
                    {
                        lastSearch = System.currentTimeMillis();
                        new SearchAddress().execute(searchBarText, Long.toString(lastSearch));
                    }
                }
            }
        });

        searchBar.setOnItemClickListener(this);

        searchBar.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);

        this.setUpMapIfNeeded();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long rowId)
    {
        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
        Address location = (Address) addressResults.get(position);
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
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
            case R.id.build_route_search_toggle_button:
                if (searchButton.isChecked())
                {
                    searchBar.setVisibility(View.VISIBLE);
                    clearButton.setVisibility(View.VISIBLE);
                    String searchBarText = searchBar.getText().toString().trim();
                    if (searchBarText.length() > 0)
                    {
                        lastSearch = System.currentTimeMillis();
                        new SearchAddress().execute(searchBarText, Long.toString(lastSearch));
                    }
                }
                else
                {
                    searchBar.setVisibility(View.GONE);
                    clearButton.setVisibility(View.GONE);
                }
                break;
            case R.id.build_route_clear_button:
                searchBar.setText("");
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
        elevationSeries.addPoint(new LinearSeries.LinearPoint(route.getDistance(),
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
        if ((!autoMode) || (markers.size() == 0))
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
        else // AUTOMODE
        {
            if (addPointsEnabled)
            {
                MapPoint point = route.getPoints().get(route.getPoints().size() - 1);
                findDirections(point.getLat(), point.getLng(), latLng.latitude, latLng.longitude, GMapV2Direction.MODE_WALKING);
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
        if (markers.size() > 1)
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

    private class GetGroupElevationData extends AsyncTask<ArrayList<MapPoint>, Integer, ArrayList<MapPoint>>
    {
        Elevation elevation = new Elevation();

        int mapSize = 0;

        ProgressDialog progress;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progress = new ProgressDialog(RouteBuilder.this);
            progress.setTitle("Auto Route Mode");
            progress.setMessage("Fetching Elevation Data");
            progress.setIndeterminate(false);
            progress.setMax(100);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.show();
        }

        @Override
        protected ArrayList<MapPoint> doInBackground(ArrayList<MapPoint>... locData)
        {
            ArrayList<MapPoint> mapPoints = locData[0];
            mapSize = mapPoints.size();
            for (int i = 0; i < mapPoints.size(); i++)
            {
                LatLng location = mapPoints.get(i).getPoint();
                Document results = elevation.getDocument(location);
                if (results != null)
                {
                    if (elevation.isOk(results))
                    {
                        mapPoints.get(i).setElevation(elevation.getElevation(results));
                    }
                    else
                    {
                        Log.e("Get Elevation", "Status NOT Okay");
                    }
                }
                else
                {
                    Log.e("Get Elevation", "Result Document is NULL");
                }
                onProgressUpdate(i);
            }
            return mapPoints;
        }

        public void onProgressUpdate(Integer ... results)
        {
            int percent = (int) (100 * ((double) results[0] / (double) mapSize));
            progress.setProgress(percent);
            Log.i(TAG, "Fetch Elevation Percent Complete: "+ percent);
        }

        protected void onPostExecute(ArrayList<MapPoint> newPoints)
        {
            progress.dismiss();
            mMap.clear();
            for (int i = 0; i < newPoints.size(); i++)
            {
                route.getPoints().add(newPoints.get(i));
                routeLine.add(newPoints.get(i).getPoint());
                elevationSeries.addPoint(new LinearSeries.LinearPoint(route.getDistance(),
                        route.getPoints().get(route.getPoints().size()-1).getElevation()));
                MarkerOptions newMarker = new MarkerOptions()
                        .title(route.getPoints().size()+": E:"+String.format("%.02f",newPoints.get(i).getElevation())
                                +"ft D:"+String.format("%.03f", newPoints.get(i).getDistance())+"mi")
                        .position(newPoints.get(i).getPoint())
                        .icon(BitmapDescriptorFactory.fromBitmap(RouteBuilder.this.marker));
                markers.add(newMarker);

            }
            if (showMarkers)
            {
                for (int i = 0; i< markers.size(); i++)
                {
                    mMap.addMarker(markers.get(i));
                }
            }
            else
            {
                mMap.addMarker(markers.get(0));
                mMap.addMarker(markers.get(markers.size()-1));
            }
            setDistanceLabel();
            mMap.addPolyline(routeLine);
            chartView.addSeries(elevationSeries);
            chartView.setLeftLabelAdapter(new ValueLabelAdapter(RouteBuilder.this, ValueLabelAdapter.LabelOrientation.VERTICAL));
            chartView.setBottomLabelAdapter(new ValueLabelAdapter(RouteBuilder.this, ValueLabelAdapter.LabelOrientation.HORIZONTAL));
        }
    }

    public void handleGetDirectionsResult(ArrayList<MapPoint> directionPoints)
    {
        new GetGroupElevationData().execute(directionPoints);
        /**
        //Polyline newPolyline;
        PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.RED);
        Log.i(TAG, "Number of points: "+directionPoints.size());
        for(int i = 0 ; i < directionPoints.size() ; i++)
        {
            rectLine.add(directionPoints.get(i));
        }
        mMap.addPolyline(rectLine);
         */
    }


    public void findDirections(double fromPositionDoubleLat, double fromPositionDoubleLong, double toPositionDoubleLat, double toPositionDoubleLong, String mode)
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, mode);

        GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask(this);
        asyncTask.execute(map);
    }

    public class GetDirectionsAsyncTask extends AsyncTask<Map<String, String>, Object,   ArrayList<MapPoint>> {

        public static final String USER_CURRENT_LAT = "user_current_lat";
        public static final String USER_CURRENT_LONG = "user_current_long";
        public static final String DESTINATION_LAT = "destination_lat";
        public static final String DESTINATION_LONG = "destination_long";
        public static final String DIRECTIONS_MODE = "directions_mode";
        private RouteBuilder activity;

        private Exception exception;

        public GetDirectionsAsyncTask(RouteBuilder activity /*String url*/)
        {
            super();
            this.activity = activity;
        }

        @Override
        public void onPostExecute(ArrayList<MapPoint> result)
        {
            if (exception == null)
            {
                activity.handleGetDirectionsResult(result);
            } else {
                processException();
            }
        }

        @Override
        protected ArrayList<MapPoint> doInBackground(Map<String, String>... params) {

            Map<String, String> paramMap = params[0];
            try{
                LatLng fromPosition = new LatLng(Double.valueOf(paramMap.get(USER_CURRENT_LAT)) , Double.valueOf(paramMap.get(USER_CURRENT_LONG)));
                LatLng toPosition = new LatLng(Double.valueOf(paramMap.get(DESTINATION_LAT)) , Double.valueOf(paramMap.get(DESTINATION_LONG)));
                GMapV2Direction md = new GMapV2Direction();
                Document doc = md.getDocument(fromPosition, toPosition, paramMap.get(DIRECTIONS_MODE));
                ArrayList<LatLng> directionPoints = md.getDirection(doc);
                ArrayList<MapPoint> mapPoints = new ArrayList<MapPoint>();
                ArrayList<MapPoint> finalPoints = new ArrayList<MapPoint>();
                if (directionPoints.size() > 0)
                {
                    mapPoints.add(new MapPoint(directionPoints.get(0).latitude, directionPoints.get(0).longitude));
                    mapPoints.get(0).setDistance(0);
                    Log.wtf(TAG, "Number Of Points Returned: "+directionPoints.size());
                    //ArrayList<Double> distances = new ArrayList<Double>();
                    for (int i = 1; i < directionPoints.size(); i++)
                    {
                            LatLng point1 = directionPoints.get(i-1);
                            LatLng point2 = directionPoints.get(i);
                            float[] results = new float[3];
                            Location.distanceBetween(point1.latitude, point1.longitude,
                                    point2.latitude, point2.longitude, results);
                            double distance = results[0] * METERS_TO_MILES;
                            //Log.wtf(TAG, "Point1: "+(i-1)+"\nPoint2: "+i+"\nDist: "+distance);
                            mapPoints.add(new MapPoint(point2.latitude, point2.longitude));
                            mapPoints.get(i).setDistance(distance);
                    }
                    directionPoints = null;
                    Log.wtf(TAG, "number of mapPoints: "+mapPoints.size());

                    boolean done = false;
                    int currPoint = 0;
                    finalPoints.add(mapPoints.get(0));
                    while (!done)
                    {
                        //MapPoint point = mapPoints.get(currPoint);
                        double totalDistance = 0;
                        boolean distanceReached = false;
                        for (int i = currPoint; i < mapPoints.size(); i++)
                        {
                            totalDistance += mapPoints.get(i).getDistance();
                            if (totalDistance >= 0.0189393)
                            {
                                finalPoints.add(mapPoints.get(i));
                                finalPoints.get(finalPoints.size()-1).setDistance(totalDistance);
                                currPoint = i+1;
                                distanceReached = true;
                                break;
                            }
                        }
                        //Log.i(TAG, "CurrPoint: "+currPoint+ " Size: "+mapPoints.size());
                        if (!distanceReached || (currPoint == mapPoints.size()-1))
                        {
                            if (!(mapPoints.get(mapPoints.size()-1).equals(finalPoints.get(finalPoints.size()-1))))
                            {
                                finalPoints.add(mapPoints.get(mapPoints.size() - 1));
                                float[] results = new float[3];
                                MapPoint point1 = finalPoints.get(finalPoints.size() - 2);
                                MapPoint point2 = finalPoints.get(finalPoints.size() - 1);
                                Location.distanceBetween(point1.getLat(), point1.getLng(), point2.getLat(), point2.getLng(), results);
                            }
                            done = true;
                        }

                    }

                }
                return finalPoints;

            }
            catch (Exception e) {
                exception = e;
                return null;
            }
        }

        private void processException() {
            Toast.makeText(activity, "Error getting directions!", Toast.LENGTH_SHORT).show();
        }

    }

    private class SearchAddress extends AsyncTask<String, String, ResultBundle>
    {
        @Override
        protected ResultBundle doInBackground(String... searchText)
        {
            String searchString = searchText[0];
            ResultBundle bundle = new ResultBundle();
            bundle.setBundleTime(Long.parseLong(searchText[1]));
            ArrayList<Address> addressResults =  new ArrayList<Address>();
            Geocoder localGeo = new Geocoder(RouteBuilder.this, Locale.US);
            ArrayList<String> localResults = new ArrayList<String>();
            {
                try
                {
                    addressResults = (ArrayList<Address>) localGeo.getFromLocationName(searchString, 6);
                }
                catch (Exception E)
                {
                    this.publishProgress("Error Searching For Address: "+E.getMessage());
                }

            }
            for (Address addressResult : addressResults)
            {
                String thisLine = "";
                boolean featureNameRepeated = false;
                if (addressResult.getMaxAddressLineIndex() > 0)
                {
                    if (thisLine.length() != 0) {
                        thisLine +=", ";
                    }
                    for (int i = 0; i < addressResult.getMaxAddressLineIndex(); i++)
                    {
                        if (addressResult.getAddressLine(i) != null)
                        {
                            if (!addressResult.getAddressLine(i).equalsIgnoreCase(addressResult.getFeatureName()))
                            {
                                thisLine += addressResult.getAddressLine(i);
                                if (i!=addressResult.getMaxAddressLineIndex()-1)
                                {
                                    thisLine += ", ";
                                }
                            }
                            else
                            {
                                featureNameRepeated = true;
                            }
                        }
                    }
                    if (featureNameRepeated)
                    {
                        if (addressResult.getFeatureName() != null)
                        {
                            thisLine = addressResult.getFeatureName() + ", " + thisLine;
                        }
                    }
                    localResults.add(thisLine);
                }


            }
            bundle.setAddresses(addressResults);
            bundle.setSearchResults(localResults);
            return bundle;


        }

        protected void onProgressUpdate(String ...strings)
        {
           // Toast.makeText(MapActivity.this, strings[0], Toast.LENGTH_SHORT).show();
        }

        protected void onPostExecute(ResultBundle results)
        {
            updateSearchBar(results);
        }
    }

    private synchronized void updateSearchBar(ResultBundle bundle)
    {
        results = bundle.getSearchResults();
        addressResults = bundle.getAddresses();
        if (bundle.getBundleTime() == lastSearch)
        {
            CustomAutoCompleteAdapter searchAdapter = new CustomAutoCompleteAdapter(RouteBuilder.this, results);
            searchBar.setAdapter(searchAdapter);

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
