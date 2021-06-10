package com.vvse.geocoordinateconverter;

import java.io.FileWriter;

public class GeoCoordinateConverter
{
	private static GeoCoordinateConverter sharedConverter;
	  
    public GeoCoordinateConverter( )
    {

    }

    public static GeoCoordinateConverter getInstance( ) {
    	
        if ( GeoCoordinateConverter.sharedConverter == null ) {
        	
        	GeoCoordinateConverter.sharedConverter = new GeoCoordinateConverter( );
        }
        
        return GeoCoordinateConverter.sharedConverter;
    }
    
    public double[] utm2LatLon(String UTM)
    {
        UTM2LatLon c = new UTM2LatLon();
        return c.convertUTMToLatLong(UTM);
    }

    public String latLon2UTM(double latitude, double longitude)
    {
        LatLon2UTM c = new LatLon2UTM();
        return c.convertLatLonToUTM(latitude, longitude);

    }

    public String latLon2MGRS(double latitude, double longitude)
    {
        LatLon2MGRS c = new LatLon2MGRS();
        return c.convertLatLonToMGRS(latitude, longitude);
    }

    public double[] mgrs2LatLon(String MGRS)
    {
        MGRS2LatLon c = new MGRS2LatLon();
        return c.convertMGRSToLatLong(MGRS);
    }

    public double degreeToRadian(double degree)
    {
        return degree * Math.PI / 180;
    }

    public double radianToDegree(double radian)
    {
        return radian * 180 / Math.PI;
    }


    public void latLon2UTMForCSV(double latitude, double longitude, FileWriter fw) {
        LatLon2UTM c = new LatLon2UTM();
        c.convertLatLonToUTMForCSV(latitude, longitude,fw);
    }
}
