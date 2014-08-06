package com.appsbylyon.mapster;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.appsbylyon.mapster.custom.ValueLabelAdapter;
import com.fima.chartview.ChartView;
import com.fima.chartview.LinearSeries;

/**
 * Created by infinite on 8/5/2014.
 */
public class RouteBuilder extends Activity implements View.OnClickListener
{
    private Button addPoint;

    private ChartView chartView;

    private int pointNum = 1;

    LinearSeries series = new LinearSeries();


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_build_route);

        addPoint = (Button) findViewById(R.id.add_point_button);
        addPoint.setOnClickListener(this);

        chartView = (ChartView) findViewById(R.id.build_route_chart);

        series.setLineColor(0xFF0099CC);
        series.setLineWidth(2);
    }

    @Override
    public void onClick(View view)
    {
        int val = ((int) (Math.random() * 500)) + 2500;
        series.addPoint(new LinearSeries.LinearPoint(pointNum, val));
        pointNum++;
        updateChart();
    }

    private void updateChart()
    {
        chartView.addSeries(series);
        chartView.setLeftLabelAdapter(new ValueLabelAdapter(this, ValueLabelAdapter.LabelOrientation.VERTICAL));
        chartView.setBottomLabelAdapter(new ValueLabelAdapter(this, ValueLabelAdapter.LabelOrientation.HORIZONTAL));
    }
}
