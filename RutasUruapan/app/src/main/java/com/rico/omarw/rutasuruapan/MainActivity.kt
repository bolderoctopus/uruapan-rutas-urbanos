package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    //TODO: if user location is outside bounds move the map
    private val LOCATION_PERMISSION_REQUEST = 32
    private val LINE_WIDTH = 15f
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        drawRoute()

    }

    private fun drawRoute() {
        val line = mMap.addPolyline(PolylineOptions()
                .color(Color.RED)
                .add(LatLng(19.431943, -102.054023))
                .add(LatLng(19.431943, -102.054023))
                .add(LatLng(19.429472, -102.055083))
                .add(LatLng(19.428516, -102.055368))
                .add(LatLng(19.427960, -102.055389))
                .add(LatLng(19.426584, -102.055153))
                .add(LatLng(19.428646, -102.062079))
                .add(LatLng(19.428807, -102.062311))
                .add(LatLng(19.428782, -102.062518))
                .add(LatLng(19.426523, -102.063307))
                .add(LatLng(19.421268, -102.064965))
                .add(LatLng(19.420701, -102.062830))
                .add(LatLng(19.413740, -102.065112))
                .add(LatLng(19.414663, -102.068283))
                .add(LatLng(19.415672, -102.067988))
                .add(LatLng(19.416910, -102.072190))
                .add(LatLng(19.418949, -102.071530))
                .add(LatLng(19.419858, -102.074345))
                .add(LatLng(19.420219, -102.075115))
                .add(LatLng(19.420978, -102.076531))
                .add(LatLng(19.421701, -102.081423))
                .add(LatLng(19.421554, -102.081162))
                .add(LatLng(19.421146, -102.078542))
                .add(LatLng(19.421159, -102.078217))
                .add(LatLng(19.420948, -102.076810))
                .add(LatLng(19.420740, -102.076145))
                .add(LatLng(19.421159, -102.078217))
                .add(LatLng(19.419885, -102.074634))
                .add(LatLng(19.418914, -102.071469))
                .add(LatLng(19.416890, -102.072212))
                .add(LatLng(19.415044, -102.065796))
                .add(LatLng(19.420963, -102.063893))
                .add(LatLng(19.420406, -102.061849))
                .add(LatLng(19.420044, -102.060758))
                .add(LatLng(19.418906, -102.056715))
                .add(LatLng(19.418595, -102.055624))
                .add(LatLng(19.421400, -102.054701))
                .add(LatLng(19.422951, -102.054650))
                .add(LatLng(19.424504, -102.054632))
                .add(LatLng(19.426358, -102.055066))
                .add(LatLng(19.427990, -102.055348))
                .add(LatLng(19.428847, -102.055291))
                .add(LatLng(19.430598, -102.054583))
                .add(LatLng(19.432118, -102.053948))
                .add(LatLng(19.433102, -102.053502))
                .width(LINE_WIDTH)
                .jointType(JointType.ROUND)
        )
        Toast.makeText(this, "total points: " + line.points.count(), Toast.LENGTH_SHORT)

    }

    private fun locationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


    private fun askPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Location permission is necesary in order to show your location on the map", Toast.LENGTH_SHORT)
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
