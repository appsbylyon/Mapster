package com.appsbylyon.mapster.custom;


import android.location.Address;

import java.util.ArrayList;

public class ResultBundle
{
    private ArrayList<Address> addresses;
    private ArrayList<String> searchResults;

    private Long bundleTime;

    public ArrayList<Address> getAddresses() {
        return addresses;
    }
    public void setAddresses(ArrayList<Address> addresses) {
        this.addresses = addresses;
    }
    public ArrayList<String> getSearchResults() {
        return searchResults;
    }
    public void setSearchResults(ArrayList<String> searchResults) {
        this.searchResults = searchResults;
    }
    public Long getBundleTime() {
        return bundleTime;
    }
    public void setBundleTime(Long bundleTime) {
        this.bundleTime = bundleTime;
    }


}

