package com.borisjerev.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by user on 21/06/2016.
 */
public class PlacesInfo {

    private Geometry geometry;
    private String name;
    private String id;
    @JsonProperty("place_id")
    private String placeID;

    // this is calculated manually
    // not sent from the server
    private int distanceBetweenCurrentLocation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaceID() {
        return placeID;
    }

    public void setPlaceID(String placeID) {
        this.placeID = placeID;
    }

    public int getDistanceBetweenCurrentLocation() {
        return distanceBetweenCurrentLocation;
    }

    public void setDistanceBetweenCurrentLocation(int distanceBetweenCurrentLocation) {
        this.distanceBetweenCurrentLocation = distanceBetweenCurrentLocation;
    }

    public static class Geometry {
        private Location location;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }

    public static class Location {
        private double lat;
        private double lng;

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }
    }
}
