package com.appsbylyon.mapster.custom;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ValueLabelAdapter extends com.fima.chartview.LabelAdapter {
    public enum LabelOrientation {
        HORIZONTAL, VERTICAL
    }

    private Context mContext;
    private LabelOrientation mOrientation;

    public ValueLabelAdapter(Context context, LabelOrientation orientation) {
        mContext = context;
        mOrientation = orientation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView labelTextView;
        if (convertView == null) {
            convertView = new TextView(mContext);
        }

        labelTextView = (TextView) convertView;
        String formatString = "%1.0f";
        int gravity = Gravity.CENTER;
        if (mOrientation == LabelOrientation.VERTICAL) {
            if (position == 0) {
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
            } else if (position == getCount() - 1) {
                gravity = Gravity.TOP | Gravity.RIGHT;
            } else {
                gravity = Gravity.CENTER | Gravity.RIGHT;
            }
        } else if (mOrientation == LabelOrientation.HORIZONTAL) {
            formatString = "%.1f";

            if (position == 0) {
                gravity = Gravity.CENTER | Gravity.LEFT;
            } else if (position == getCount() - 1) {
                gravity = Gravity.CENTER | Gravity.RIGHT;
            }
        }

        labelTextView.setGravity(gravity);
        labelTextView.setPadding(2, 0, 2, 0);
        labelTextView.setText(String.format(formatString, getItem(position)));

        return convertView;
    }
}
