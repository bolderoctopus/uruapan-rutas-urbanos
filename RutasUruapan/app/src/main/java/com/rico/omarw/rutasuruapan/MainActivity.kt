package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.rico.omarw.rutasuruapan.Database.AppDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), OnMapReadyCallback, RouteListAdapter.ListCallback{
    //TODO: if user location is outside bounds move the map
    private val LOCATION_PERMISSION_REQUEST = 32
    private val LINE_WIDTH = 15f
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        InitDatabaseTask(WeakReference(applicationContext)).execute()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        recyclerView.post { fillList() }
    }

    private fun fillList() {
        AsyncTask.execute{
            val routesDao = AppDatabase.getInstance(this)?.routesDAO()
            val dbRoutes = routesDao?.getRoutes()
            val adapterItems = arrayListOf<RouteModel>()
            dbRoutes?.forEach{
                adapterItems.add(RouteModel(it))
            }

            runOnUiThread {
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = RouteListAdapter(adapterItems, this, this)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val uruapan = LatLng(19.411843, -102.051518)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uruapan, 13f))
        if (locationPermissionEnabled()){
            mMap.isMyLocationEnabled = true
        }else {
            askPermission()
        }
    }

    override fun drawRoute(model: RouteModel) {
        if (model.polyline != null){
            model.polyline?.isVisible = !model.isDrawed
            model.isDrawed = model.isDrawed.not()

        }else{
            AsyncTask.execute {
                val points = AppDatabase.getInstance(this)?.routesDAO()?.getPointsFrom( model.id)
                val polylineOptions = PolylineOptions()
                Log.d(DEBUG_TAG, model.color);
                polylineOptions.color(Color.parseColor(model.color))
                        .width(LINE_WIDTH)
                        .jointType(JointType.ROUND)

                points?.forEach{
                    polylineOptions.add(LatLng(it.lat, it.lng))
                }


                runOnUiThread{
                    model.polyline = mMap.addPolyline(polylineOptions)
                    model.isDrawed = true
                }
            }
        }
    }

    private fun locationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Location permission is necesary in order to show your location on the map", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            LOCATION_PERMISSION_REQUEST ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mMap.isMyLocationEnabled = true
                }
            }
        }
    }
}
