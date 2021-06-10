package com.example.locationdemo.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/*import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;*/

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * Created by m.prajapati on 06-04-2018.
 */

public abstract class BaseActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback{






    // LogCat tag
    private static final String TAG = BaseActivity.class.getSimpleName();

    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private Location mLastLocation;


    // Google client to interact with Google API
   // private GoogleApiClient mGoogleApiClient;
    // list of permissions
    ArrayList<String> permissions=new ArrayList<>();
    PermissionUtils permissionUtils;
    boolean isPermissionGranted;
   // private LocationRequest mLocationRequest;
    public boolean internetStatus;
    public static TextView log_network;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         initBaseActivityValues(true,true);
    }








    public void initBaseActivityValues(boolean isNeedPermissions,boolean isNeedLocation){


        if(isNeedPermissions){
            checkForPermission(true);
        }

        if(isNeedLocation){
            final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

            if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                updatedGpsStatus(false);
                checkGpsOnOFF();
            }else{
                initGoogleApiClient();
                checkGpsOnOFF();
            }

        }


    }



    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;



    public void initGoogleApiClient(){

                getLocation();

    }



    public void checkGpsOnOFF(){
        GpsChangeReceiver m_gpsChangeReceiver = new GpsChangeReceiver();
        getApplicationContext().registerReceiver(m_gpsChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    public class GpsChangeReceiver   extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent )
        {
            final LocationManager manager = (LocationManager) context.getSystemService( Context.LOCATION_SERVICE );
            if (manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                updatedGpsStatus(true);
            }
            else
            {
                updatedGpsStatus(false);
            }
        }
    }

    public void updatedGpsStatus(boolean isGpsOn) {
        if(!isGpsOn){
           // initGoogleApiClient();
          //  checkGpsOnOFF();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made

                        getFinalLoaction();

                        // btGPS.setTextColor(Color.parseColor("#008000"));
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        // btGPS.setTextColor(Color.RED);
                        break;
                    default:
                        //  btGPS.setTextColor(Color.RED);
                        break;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkGPSStatus() ;
    }

    private void checkGPSStatus() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE );
        final boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        updatedGpsStatus(statusOfGPS);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void getFinalLoaction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //if (mGoogleApiClient.isConnected()) {
                checkForPermission(true);
                getLocation(); /* commented by rajeev */
           // }

        } else {
            getLocation();
        }

    }

    public void checkForPermission(boolean isdialog) {
        permissionUtils=new PermissionUtils(BaseActivity.this);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionUtils.check_permission(permissions,"Please allow location permission",1);
    }



    // Permission check functions


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {// redirects to utils

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions != null) {
            permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }



    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (isPermissionGranted) {
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE );
            final boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            updatedGpsStatus(statusOfGPS);
            try
            {
              //  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                    LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setPowerRequirement(Criteria.POWER_HIGH);
                    criteria.setAltitudeRequired(false);
                    criteria.setSpeedRequired(false);
                    criteria.setCostAllowed(true);
                    criteria.setBearingRequired(false);
                    criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
                    criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

                GpsStatus.NmeaListener _nmeaListener = new GpsStatus.NmeaListener() {

                    @Override
                    public void onNmeaReceived(long timestamp, String nmea) {
                       /* String time = _elapsedTimer.convertMillisToMDYHMSS(timestamp);
                        _gpsNMEAText = "<b><font color='yellow'>GPS NMEA</b></font>" + "<br><b>Timestamp:</b> " + time + "<br><b>NMEA code:</b> " + nmea;
                        _gpsNMEATextView.setText(Html.fromHtml(_gpsNMEAText));*/
                    }
                };
                mLocationManager.addNmeaListener(_nmeaListener);
                GpsStatus.Listener _gpsStatusListener = new GpsStatus.Listener() {

                    String seconds;

                    String minutes;

                    String hours;

                    String ms;

                    String satelliteHMS;

                    String usedInFix = "false";

                    int t;

                    @Override
                    public void onGpsStatusChanged(int event) {
                      //  _satelliteList = "";
                        satelliteHMS = "N/A";
                        //Occasionally there may be null values if GPS hiccups
                        try {
                          /*  t = mLocationManager.getGpsStatus(null).getTimeToFirstFix();
                            //String seconds = String.format(_format, t/1000 % 60);
                            seconds = String.format(_format, TimeUnit.MILLISECONDS.toSeconds(t));
                            minutes = String.format(_format, TimeUnit.MILLISECONDS.toMinutes(t));
                            hours = String.format(_format, TimeUnit.MILLISECONDS.toHours(t));
                            ms = String.format(_format, t % 1000);
                            satelliteHMS = hours + ":" + minutes + ":" + seconds + ":" + ms;
                            _satellites = _locationManager.getGpsStatus(null).getSatellites();
                            if (_satellites != null) {
                                for (GpsSatellite sat : _satellites) {
                                    if (sat.usedInFix() == true) {
                                        usedInFix = "<font color='red'>true</font>";
                                    } else {
                                        usedInFix = "false";
                                    }
                                    _satelliteList = _satelliteList + "<br>" + sat.getPrn() + ", " + sat.getSnr() + ", " + usedInFix;
                                }
                            }*/
                        } catch (Exception exc) {
                            Log.d("GPSTester", "GPS Status error (onGpsStatusChanged): " + exc.getMessage());
                        }
                      /*  if (_satelliteList != "") {
                            _gpsSatelliteTextView.setText(Html.fromHtml("<b><font color='yellow'>GPS Satellite Info (No., SNR, Used in fix)</b></font>" + "<br><b>Time to 1st fix:</b> " + satelliteHMS + _satelliteList));
                        }*/
                    }
                };
                mLocationManager.addGpsStatusListener(_gpsStatusListener);
                try {
                   // long minDistance = Long.valueOf(_preferences.getString("pref_key_updateGPSMinDistance", "0"));
                   // long minTime = Long.valueOf(_preferences.getString("pref_key_updateGPSMinTime", "0"));
                    // Register the listener with the Location Manager to receive location updates
                    mLocationManager.requestLocationUpdates(500, 0, criteria,new LocationListener() {
                        @Override
                        public void onLocationChanged(final Location location) {
                            mLastLocation = location;
                            if (mLastLocation != null) {
                                updatedLocation(location);
                            } else {

                            }
                        }

                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {

                        }

                        @Override
                        public void onProviderEnabled(String s) {

                        }

                        @Override
                        public void onProviderDisabled(String s) {

                        }
                    },null);
                } catch (Exception exc) {
                    Log.d("GPSTester", "Unable to start GPS provider. Bad value. " + exc.getMessage());
                }

               /* }else{
                    FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,new LocationCallback(){
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            Location locationData = locationResult.getLastLocation();

                            mLastLocation = locationData ;
                            if (mLastLocation != null) {
                                updatedLocation(locationData );
                            } else {

                            }

                        }
                    },null);
                    *//*LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, new com.google.android.gms.location.LocationListener() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onLocationChanged(Location location) {

                        }
                    });*//*
                }*/
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }

        }else {
          //  permissionStatus(1);
        }

    }

    public void permissionStatus(int isPermissionGranted){

    }

    public void updatedLocation(Location location) {
    }


    @Override
    public void PermissionGranted(int request_code) {
         permissionStatus(0);
         isPermissionGranted=true;
         getLocation();
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {}

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION_CHECK","DENIED");
        permissionStatus(1);
    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION_CHECK","NEVER ASK AGAIN");
        permissionStatus(3);
    }


}
