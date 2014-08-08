package com.appsbylyon.mapster.custom;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by infinite on 8/1/14.
 */
public class CustomAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable
{
    Context _context;
    ArrayList<String> _items = new ArrayList<String>();

    public CustomAutoCompleteAdapter(Context context, ArrayList<String> items) {
        super(context, android.R.layout.simple_list_item_1, items);
        _context = context;

        _items = items;
        filter = new ItemFilter();

    }

    @Override
    public int getCount() {
        if (_items != null)
            return _items.size();
        else
            return 0;
    }

    @Override
    public String getItem(int arg0) {
        return _items.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {

        return 0;
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup parent) {
        TextView text = (TextView)super.getView(arg0, arg1, parent);
        text.setText((String)_items.get(arg0));
        // LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // View rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);

        return text;
        /**
         View gv;

         if (arg1 == null){

         gv = new View(_context);
         }else {
         gv = (View) arg1;
         }
         return gv;
         */
    }



    @Override

    public Filter getFilter() {

        return filter;

    }

    private ItemFilter filter;

    ArrayList<String> orig = new ArrayList<String>();

    private class ItemFilter extends Filter {

        public ItemFilter() {

        }

        ArrayList<String> results = new ArrayList<String>();
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            //if(constraint != null)
            // Logging.TraceMessage(constraint.toString(), "**", "1");
            FilterResults oReturn = new FilterResults();
            /**
             if (orig == null){
             for (int i = 0; i < _items.size(); i++) {
             orig.add(new String(_items.get(i)));
             }
             }

             //if (constraint != null){
             results.clear();
             if (orig != null && orig.size() > 0) {
             for (String i : orig) {
             results.add(i);
             }
             }
             // Logging.TraceMessage(String.valueOf(results.size()), "**", "2");
             */
            oReturn.values = _items;
            //}
            oReturn.count = results.size();
            return oReturn;
        }


        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //_items.clear();
            // for(int i = 0; i < ((ArrayList<String>)results.values).size(); i++){
            //    _items.add(new String(((ArrayList<String>)results.values).get(i)));
            // }
            notifyDataSetChanged();
        }

    }

}

