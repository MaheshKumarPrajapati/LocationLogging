package com.example.locationdemo

import android.Manifest
import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileBrowser
import com.vvse.geocoordinateconverter.GeoCoordinateConverter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(),LocationListener {
    private var mLocationManager: LocationManager? = null
    var fileName= "log to file.csv"
    val formatter = SimpleDateFormat("dd MMM yyyy HH:mm:ss")
    private var mProvider: LocationProvider? = null
    private var mStarted: Boolean= false
    private var mUserDeniedPermission: Boolean= false
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_COARSE_LOCATION,
            permission.WRITE_EXTERNAL_STORAGE,
            permission.READ_EXTERNAL_STORAGE
    )
    private val LOCATION_PERMISSION_REQUEST = 1
    /**
     * Android M (6.0.1) and below status and listener
     */
    private val mLegacyStatus: GpsStatus? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        initLocation()
        findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.VISIBLE
        findViewById<Button>(R.id.bt_turn_on_gps).setOnClickListener {
           // turnOnLocation()
            findViewById<RelativeLayout>(R.id.rl_gps_status).visibility= View.GONE
            findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.VISIBLE
        }

        findViewById<Button>(R.id.bt_open_log_folder).setOnClickListener {
            val i = Intent(this, FileBrowser::class.java)
            //i.putExtra(Constants.INITIAL_DIRECTORY, File(Environment.getExternalStorageDirectory().absolutePath, "/${getString(R.string.app_name)}").absolutePath)
            i.putExtra(Constants.INITIAL_DIRECTORY, File(getPath(), "/${getString(R.string.app_name)}").absolutePath)
            i.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal);
            startActivityForResult(i, 10);
        }
    }

    private fun initLocation() {
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mProvider = mLocationManager!!.getProvider(LocationManager.GPS_PROVIDER)
        if (mProvider == null) {
            Log.e(
                "com.android.gpstest.GpsTestActivity.TAG",
                "Unable to get GPS_PROVIDER"
            )
            Toast.makeText(
                this, getString(R.string.gps_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (!mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptEnableGps()
        }else {
            findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.VISIBLE
            startGPS()
        }

    }

    override fun onResume() {
        super.onResume()
        if (!mUserDeniedPermission) {
            requestPermissionAndInit(this)
        } else {
            // Explain permission to user (don't request permission here directly to avoid infinite
            // loop if user selects "Don't ask again") in system permission prompt
            showLocationPermissionDialog()
        }

    }
    private fun showLocationPermissionDialog() {
        val builder =
            AlertDialog.Builder(this)
                .setTitle(R.string.title_location_permission)
                .setMessage(R.string.text_location_permission)
                .setCancelable(false)
                .setPositiveButton(
                    R.string.ok
                ) { dialog, which -> // Request permissions from the user
                    ActivityCompat.requestPermissions(
                       this,
                        REQUIRED_PERMISSIONS,
                        LOCATION_PERMISSION_REQUEST
                    )
                }
                .setNegativeButton(
                    R.string.exit
                ) { dialog, which -> // Exit app
                    finish()
                }
        builder.create().show()
    }
    private fun requestPermissionAndInit(mainActivity: MainActivity) {
        if (hasGrantedPermissions(
                mainActivity,
                REQUIRED_PERMISSIONS
            )
        ) {
            initLocation()
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(
                mainActivity,
                REQUIRED_PERMISSIONS,
                LOCATION_PERMISSION_REQUEST
            )
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mUserDeniedPermission = false
                initLocation()
            } else {
                mUserDeniedPermission = true
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    fun hasGrantedPermissions(
        activity: Activity?,
        requiredPermissions: Array<String>
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Permissions granted at install time
            return true
        }
        for (p in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    activity!!,
                    p!!
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
    @SuppressLint("MissingPermission")
    private fun startGPS() {
        if (mLocationManager == null || mProvider == null) {
            return
        }

        if (!mStarted) {
            val now = Date()
            try {
                //val root = Environment.getExternalStorageDirectory()
             //   val dir = File(root.absolutePath + "/${getString(R.string.app_name)}")
                val dir = File(getPath() + "/${getString(R.string.app_name)}")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val gpxfile = File(dir, fileName)
                var fw:FileWriter= FileWriter(gpxfile);

                fw!!.append("Horizontal Accuracy")
                fw!!.append(',')

                fw!!.append("Vertical Accuracy")
                fw!!.append(',')

                fw!!.append("Latitude")
                fw!!.append(',')

                fw!!.append("Longitude")
                fw!!.append(',')

                fw!!.append("Altitude")
                fw!!.append(',')

                fw!!.append("Update Time")
                fw!!.append(',')

                fw!!.append("UTM Zone")
                fw!!.append(',')

                fw!!.append("Easting")
                fw!!.append(',')

                fw!!.append("Northing")
                fw!!.append(',')
                fw!!.append('\n');
                fw!!.close();

            }catch (e:Exception)   {}
            /*  mLocationManager!!
                  .requestLocationUpdates(mProvider!!.name, 500, 0.0f, this)*/

            // long minDistance = Long.valueOf(_preferences.getString("pref_key_updateGPSMinDistance", "0"));
            // long minTime = Long.valueOf(_preferences.getString("pref_key_updateGPSMinTime", "0"));
            // Register the listener with the Location Manager to receive location updates0
            mLocationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0f,this
               //criteria,
                /*object : LocationListener {
                    override fun onLocationChanged(it: Location) {
                        if(it!=null) {
                            findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.GONE
                            findViewById<TextView>(R.id.tv_accuracy_value).text = "${it.accuracy} / ${it.getVerticalAccuracyMeters()} m"
                            findViewById<TextView>(R.id.tv_latitude_value).text = "${it.latitude}"
                            findViewById<TextView>(R.id.tv_longitude_value).text = "${it.longitude}"
                            findViewById<TextView>(R.id.tv_altitude_value).text = "${it.altitude}"
                            findViewById<TextView>(R.id.tv_utm_value).text = "${GeoCoordinateConverter.getInstance().latLon2UTM(it.latitude,it.longitude)}"

                        }
                    }

                    override fun onStatusChanged(
                        s: String,
                        i: Int,
                        bundle: Bundle
                    ) {
                       print("onStatusChanged")
                    }

                    override fun onProviderEnabled(s: String) {
                        print("onProviderEnabled")
                    }
                    override fun onProviderDisabled(s: String) {
                        print("onProviderDisabled")
                    }
                }*///,null
            )
            mStarted = true


      }

    }

    private fun promptEnableGps() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.enable_gps_message))
            .setPositiveButton(
                getString(R.string.enable_gps_positive_button)
            ) { dialog: DialogInterface?, which: Int ->
                val intent = Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
                startActivity(intent)
            }
            .setNegativeButton(
                getString(R.string.enable_gps_negative_button)
            ) { dialog: DialogInterface?, which: Int -> }
            .show()
    }



    override fun onLocationChanged(it: Location) {
        if(it!=null) {
            findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.GONE
            findViewById<TextView>(R.id.tv_accuracy_value).text = "${it.accuracy} / ${it.getVerticalAccuracyMeters()} m"
            findViewById<TextView>(R.id.tv_latitude_value).text = "${it.latitude}"
            findViewById<TextView>(R.id.tv_longitude_value).text = "${it.longitude}"
            findViewById<TextView>(R.id.tv_altitude_value).text = "${it.altitude} ${getString(R.string.above_ellipsoide)}"
            findViewById<TextView>(R.id.tv_utm_value).text = "${GeoCoordinateConverter.getInstance().latLon2UTM(it.latitude,it.longitude)}"
         /*   var jsonObject=JSONObject();
            jsonObject.put("horizontal_accuracy" ,"${it.accuracy}")
            jsonObject.put("vertical_accuracy" ,"${it.getVerticalAccuracyMeters()}")
            jsonObject.put("latitude" ,"${it.latitude}")
            jsonObject.put("longitude" ,"${it.longitude}")
            jsonObject.put("altitude" ,"${it.altitude}")
            jsonObject.put("update_time" ,"${formatter.format(Date())}")
            jsonObject.put("utm" ,"${GeoCoordinateConverter.getInstance().latLon2UTM(it.latitude,it.longitude)}")
            jsonArray.put(jsonObject)*/

            var data= "\n\nHorizontal Accuracy : ${it.accuracy},\n" +
                    "Vertical Accuracy : ${it.getVerticalAccuracyMeters()},\n" +
                    "Latitude : ${it.latitude},\n" +
                    "Longitude : ${it.longitude},\n" +
                    "Altitude : ${it.altitude},\n" +
                    "Update Time : ${formatter.format(Date())},\n" +
                    "${GeoCoordinateConverter.getInstance().latLon2UTM(it.latitude, it.longitude)}"
            generateNoteOnSD(this,fileName,data,it)
        }
    }

    fun generateNoteOnSD(context: Context?, sFileName: String?, sBody: String?, it: Location) {
try{
  //  val root = Environment.getExternalStorageDirectory()
   // val dir = File(root.absolutePath + "/${getString(R.string.app_name)}")
    val dir = File(getPath() + "/${getString(R.string.app_name)}")
       val gpxfile = File(dir, fileName)
        var fw = FileWriter(gpxfile,true);
        fw!!.append("${it.accuracy}")
        fw!!.append(',')

        fw!!.append("${it.getVerticalAccuracyMeters()}")
        fw!!.append(',')

        fw!!.append("${it.latitude}")
        fw!!.append(',')

        fw!!.append("${it.longitude}")
        fw!!.append(',')

        fw!!.append("${it.altitude}")
        fw!!.append(',')

        fw!!.append("${formatter.format(Date())}")
        fw!!.append(',')

        GeoCoordinateConverter.getInstance().latLon2UTMForCSV(it.latitude, it.longitude,fw!!)


        fw!!.append('\n');
        fw!!.close();

           /* val writer = FileWriter(gpxfile, true)

            writer.append("""
    ${sBody.toString()}
    
    
    """.trimIndent())
            writer.flush()
            writer.close()*/
           // Toast.makeText(this, "Data has been written to Report File", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()

        }
    }

    @Throws(IOException::class)
    private fun getPath(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
           return applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath;
        } else {
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        }
    }
}