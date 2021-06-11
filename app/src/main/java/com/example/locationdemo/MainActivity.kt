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
import androidx.documentfile.provider.DocumentFile
import com.example.locationdemo.geocoordinateconverter.GeoCoordinateConverter
import com.example.locationdemo.utils.SpUtil
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(),LocationListener {
    val LOGTAG = "MainActivity"
    val REQUEST_CODE = 12123
    private var mLocationManager: LocationManager? = null
    private var storageUri:Uri?=null
    val formatter = SimpleDateFormat("dd MMM yyyy HH:mm:ss")
    var fileName= "log to file ${formatter.format(Date())}.csv"
    var stringdata:String="Horizontal Accuracy,Vertical Accuracy,Latitude,Longitude,Altitude,Update Time,UTM Zone,Easting,Northing\n"
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        openDocumentTree()
        findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.VISIBLE
        findViewById<Button>(R.id.bt_turn_on_gps).setOnClickListener {
           // turnOnLocation()
            findViewById<RelativeLayout>(R.id.rl_gps_status).visibility= View.GONE
            findViewById<ConstraintLayout>(R.id.cl_progressbar).visibility=View.VISIBLE
        }

    }

    private fun openDocumentTree() {
        val uriString = SpUtil.getString(SpUtil.FOLDER_URI, "")
        when {
            uriString == "" -> {
                Log.w(LOGTAG, "uri not stored")
                askPermission()
            }
            arePermissionsGranted(uriString) -> {
                makeDoc(Uri.parse(uriString))
            }
            else -> {
                Log.w(LOGTAG, "uri permission not stored")
                askPermission()
            }
        }
    }

    private fun makeDoc(dirUri: Uri) {
        val dir = DocumentFile.fromTreeUri(this, dirUri)
        if (dir == null || !dir.exists()) {
            //the folder was probably deleted
            Log.e(LOGTAG, "no Dir")
            //according to Commonsware blog, the number of persisted uri permissions is limited
            //so we should release those we cannot use anymore
            //https://commonsware.com/blog/2020/06/13/count-your-saf-uri-permission-grants.html
            releasePermissions(dirUri)
            //ask user to choose another folder
            Toast.makeText(this,"Folder deleted, please choose another!",Toast.LENGTH_SHORT).show()
            openDocumentTree()
        } else {
           // alterDocument(dirUri)
            val file = dir.createFile("*/csv", fileName)
            if (file != null && file.canWrite()) {
                Log.d(LOGTAG, "file.uri = ${file.uri.toString()}")
                alterDocument(file.uri)
            } else {
                Log.d(LOGTAG, "no file or cannot write")
                //consider showing some more appropriate error message
                Toast.makeText(this,"Write error!",Toast.LENGTH_SHORT).show()

            }
        }
    }
    private fun releasePermissions(uri: Uri) {
        val flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.releasePersistableUriPermission(uri,flags)
        //we should remove this uri from our shared prefs, so we can start over again next time
        SpUtil.storeString(SpUtil.FOLDER_URI, "")
    }


    //Just a test function to write something into a file, from https://developer.android.com
    //Please note, that all file IO MUST be done on a background thread. It is not so in this
    //sample - for the sake of brevity.
    private fun alterDocument(uri: Uri) {
        try {
            storageUri=uri

            contentResolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
                FileOutputStream(parcelFileDescriptor.fileDescriptor).use {
                    it.write((stringdata).toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            if (data != null) {
                //this is the uri user has provided us
                val treeUri: Uri? = data.data
                if (treeUri != null) {
                    Log.i(LOGTAG, "got uri: ${treeUri.toString()}")
                    // here we should do some checks on the uri, we do not want root uri
                    // because it will not work on Android 11, or perhaps we have some specific
                    // folder name that we want, etc
                    if (Uri.decode(treeUri.toString()).endsWith(":")){
                        Toast.makeText(this,"Cannot use root folder!",Toast.LENGTH_SHORT).show()
                        // consider asking user to select another folder
                        return
                    }
                    // here we ask the content resolver to persist the permission for us
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(treeUri,
                            takeFlags)

                    // we should store the string fo further use
                    SpUtil.storeString(SpUtil.FOLDER_URI, treeUri.toString())

                    //Finally, we can do our file operations
                    //Please note, that all file IO MUST be done on a background thread. It is not so in this
                    //sample - for the sake of brevity.
                    makeDoc(treeUri)
                }
            }
        }
    }

    // this will present the user with folder browser to select a folder for our data
    private fun askPermission() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE)
    }

    private fun arePermissionsGranted(uriString: String): Boolean {
        // list of all persisted permissions for our app
        val list = contentResolver.persistedUriPermissions
        for (i in list.indices) {
            val persistedUriString = list[i].uri.toString()
            //Log.d(LOGTAG, "comparing $persistedUriString and $uriString")
            if (persistedUriString == uriString && list[i].isWritePermission && list[i].isReadPermission) {
                //Log.d(LOGTAG, "permission ok")
                return true
            }
        }
        return false
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


            mLocationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0f,this)
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
            findViewById<TextView>(R.id.tv_utm_value).text = "${GeoCoordinateConverter.instance!!.latLon2UTM(it.latitude,it.longitude)}"

            writeToCSVFile(it)
        }
    }

    fun writeToCSVFile(loc: Location) {
       stringdata="${stringdata}${loc.accuracy},${loc.getVerticalAccuracyMeters()},${loc.latitude},${loc.longitude},${loc.altitude},${formatter.format(Date())},${GeoCoordinateConverter.instance!!.latLon2UTMForCSV(loc.latitude, loc.longitude)}\n"
    if(storageUri!=null) {
        contentResolver.openFileDescriptor(storageUri!!, "rw")?.use { parcelFileDescriptor ->
            FileOutputStream(parcelFileDescriptor.fileDescriptor).use {
                it.write((stringdata).toByteArray())
            }
        }
    }
    }
}