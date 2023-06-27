package com.example.rmasapp.pages

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.maps.model.CircleOptions
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.location.Location
import android.location.Location.distanceBetween
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.rmasapp.MainActivity
import com.example.rmasapp.Placed
import com.example.rmasapp.R
import com.example.rmasapp.getPlacedFromDoc
import com.example.rmasapp.latLngToArray
import com.example.rmasapp.placedToMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback

import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot


enum class MapsState {
    Watch,
    AddingMarker,
    EditingMarker,
    ViewingMarker,

}
class CustomInfoWindowAdapter(private val inflater: LayoutInflater) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        return null // Use the default window frame/background if null
    }

    override fun getInfoContents(marker: Marker): View? {
        // Inflate your custom view here
        val view = inflater.inflate(R.layout.custom_info_window, null)
        val title = view.findViewById<TextView>(R.id.title)
        val snippet = view.findViewById<TextView>(R.id.snippet)

        title.text = marker.title
        snippet.text = marker.snippet
        // Set other views in your custom info window

        return view
    }
}
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    companion object {
        var instance : MapsFragment? = null
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        var dest : LatLng? = null
    }

    var state = MapsState.Watch
    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var mapInteract = true
    private var mapLocked = false
//    private var markerFocused : Marker? = null

    private val markers = ArrayList<Marker>()

    private var startedLocationUpdates = false
    private  var firstMapLoad = false

    private lateinit var btnToggleLock : FloatingActionButton
    private lateinit var btnAddMarker : FloatingActionButton
    private lateinit var btnCollect : FloatingActionButton


    private lateinit var edSearchRadius: EditText
    private lateinit var edSearchTitle: EditText

    private lateinit var addEditMarkerDialog: ScrollView

    private lateinit var tvPlacedBy: TextView
    private lateinit var edtTitle: EditText
    private lateinit var edtScore: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnPlace: Button
    private lateinit var btnCancel: Button

    private var radiusCircle: Circle? = null
    private var collectCircle: Circle? = null


    private var imageUri: Uri? = null
    private var markerToEdit: Placed? = null // Pass this in if you are editing a marker
   private var nearbyMarkers = ArrayList<Placed>()


    private fun updateUI(){
        addEditMarkerDialog.isVisible = false
        edSearchRadius.isVisible = false
        btnToggleLock.isVisible = false
        btnAddMarker.isVisible = false
        tvPlacedBy.isVisible = false
        btnCollect.isVisible = false


        if(state==MapsState.AddingMarker){
            addEditMarkerDialog.isVisible = true
        }else if(state==MapsState.EditingMarker){
            addEditMarkerDialog.isVisible = true
        }else if(state==MapsState.ViewingMarker){
            addEditMarkerDialog.isVisible = true
            tvPlacedBy.isVisible = true
        }else if(state==MapsState.Watch){
            edSearchRadius.isVisible = true
            btnToggleLock.isVisible = true
            btnAddMarker.isVisible = true

            btnCollect.isVisible = (nearbyMarkers.size>0)
        }

        if (!mapInteract) {
            btnToggleLock.setImageResource(androidx.appcompat.R.drawable.btn_radio_off_mtrl)
        } else {
            btnToggleLock.setImageResource(androidx.appcompat.R.drawable.abc_ic_commit_search_api_mtrl_alpha)
        }

        if (!mapInteract || mapLocked) {
            mMap.uiSettings.isZoomGesturesEnabled = true
            mMap.uiSettings.isScrollGesturesEnabled = false
            mMap.uiSettings.isTiltGesturesEnabled = false
            mMap.uiSettings.isRotateGesturesEnabled = true

        } else {
            mMap.uiSettings.isZoomGesturesEnabled = true
            mMap.uiSettings.isScrollGesturesEnabled = true
            mMap.uiSettings.isTiltGesturesEnabled = true
            mMap.uiSettings.isRotateGesturesEnabled = true
        }

        if(mapInteract && dest != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dest, 15f))
        }
        dest = null
    }
    private fun placeMarker(coins: Int) {
        val userRef = MainActivity.db.collection("profiles").document(MainActivity.user!!.uid)
        val newMarkerRef = MainActivity.db.collection("markers").document(MainActivity.user!!.uid + "_" + MainActivity.profile!!.markersPlaced.toString())

        MainActivity.db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            var currentCoins = snapshot.getLong("coins") ?: 0

            if (currentCoins < coins) {
                throw FirebaseFirestoreException("Insufficient coins", FirebaseFirestoreException.Code.ABORTED)
            }


            val marker = Placed(
                MainActivity.user!!.uid +"_"+ MainActivity.profile!!.markersPlaced,
                MainActivity.user!!.uid,
                MainActivity.profile!!.firstName + " " + MainActivity.profile!!.lastName,
                MainActivity.profile!!.markersPlaced,
                MainActivity.latLng!!,
                edtTitle.text.toString(),
                edtDescription.text.toString(),
                coins,
                ""
            )
            val markerMap = placedToMap(marker)

            currentCoins -= coins
            MainActivity.profile!!.markersPlaced++
            MainActivity.profile!!.coins = currentCoins.toInt()


            transaction.set(newMarkerRef, markerMap)
            transaction.update(userRef, "markersPlaced", MainActivity.profile!!.markersPlaced, "coins", MainActivity.profile!!.coins)
            return@runTransaction null
        }.addOnSuccessListener {
            MainActivity.getMarkers(1)
            resetMarkerDialog()
            Toast.makeText(requireContext(), "Marker placed!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error placing marker $e", Toast.LENGTH_LONG).show()
            Log.w("LoginFormFragment", "Error placing marker", e)
        }
    }
    private fun resetMarkerDialog(){
        edtDescription.setText("")
        edtScore.setText("1")
        edtTitle.setText("")
        mapLocked = false
        goState(MapsState.Watch)
    }
    fun updateMarkersUI(){
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
        var i = 0
        if(state == MapsState.Watch) {
            i = 0
            for (p in MainActivity.resultsPlaced) {
                val title = p.title
                val markerIcon = makeMarkerIcon(this, title, false)
                val marker = mMap.addMarker(
                    MarkerOptions().position(p.latLng)
                        .title("Placed by ${p.ownerName}\nWorth ${p.coins}")
                        .icon(markerIcon)
                )
                marker?.tag = "o"+(i++).toString() // index
                //marker.showInfoWindow()
                markers.add(marker)
            }
        }
            i = 0
            for (p in MainActivity.placedMine) {
                val title = p.title
                val markerIcon = makeMarkerIcon(this, title, true)
                val marker = mMap.addMarker(
                    MarkerOptions().position(p.latLng)
                        .title("Placed by You\nWorth ${p.coins}")
                        .icon(markerIcon)
                )
                marker?.tag = "m"+(i++).toString() // index
                //marker.showInfoWindow()
                markers.add(marker)
            }


    }
    fun updateMapUI(){
        if(MainActivity.latLng==null) return
        radiusCircle?.remove()
        collectCircle?.remove()
        if(state == MapsState.Watch) {


            radiusCircle = mMap.addCircle(CircleOptions()
                .center(MainActivity.latLng)
                .radius(MainActivity.searchRadius.toDouble())
                .fillColor(
                    Color.argb(
                        44,
                        0,
                        0,
                        255
                    )
                )
                .strokeWidth(0f)  // no border
            )
            collectCircle = mMap.addCircle(CircleOptions()
                .center(MainActivity.latLng)
                .radius(100.0)
                .fillColor(
                    Color.argb(
                        40,
                        250,
                        0,
                        0
                    )
                )
                .strokeWidth(0f)  // no border
            )

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View{
        return inflater.inflate(R.layout.fragment_gallery, container, false).also {
            mapView = it.findViewById(R.id.map)
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this)

        }


        //fragmentContainer = childFragmentManager.findFragmentById(R.id.fragmentContainer) as FrameLayout //root.findViewById(R.id.fragmentContainer)

    }

    private fun goState(s:  MapsState){
        state = s
        updateUI()
        updateMapUI()
        updateMarkersUI()
    }
    private fun showAddEditMarkerFragment() {
        mapLocked = true
        lockMap(mapInteract)
        goState(MapsState.AddingMarker)
        // If you're editing a marker:
        // addEditMarkerFragment.markerToEdit = marker
      //  childFragmentManager.beginTransaction().replace(R.id.fragmentContainer, addEditMarkerFragment).addToBackStack(null).commit()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Retrieve the data from the marker.
        val tag = marker.tag!! as String
        //var whose = if(tag[0]=='m') MainActivity.placedMine else MainActivity.resultsPlaced
        var idx = tag.subSequence(startIndex = 1, endIndex = tag.length).toString().toInt()

        // Make a toast with the marker's tag
        Toast.makeText(requireContext(), "CLICKED ${tag}", Toast.LENGTH_SHORT).show()

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }

    private fun collect(marker: Placed) {
        btnCollect.isVisible = false
        val userRef = MainActivity.db.collection("profiles").document(MainActivity.user!!.uid)
        val markerRef = MainActivity.db.collection("markers").document(marker.id)

        MainActivity.db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            var currentXp = snapshot.getLong("xp") ?: 0

            currentXp += marker.coins
            MainActivity.profile!!.xp = currentXp.toInt()

            transaction.update(userRef, "xp", MainActivity.profile!!.xp)
            transaction.delete(markerRef)
            return@runTransaction null
        }.addOnSuccessListener {
            MainActivity.getMarkers(1)
            Toast.makeText(requireContext(), "Marker ${marker.title} collected!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error collecting marker $e", Toast.LENGTH_LONG).show()
            Log.w("LoginFormFragment", "Error collecting marker", e)
        }
    }
    private fun removeNearby(){
        var minDistance = Float.MAX_VALUE
        var idx = -1
        val thisLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = MainActivity.latLng!!.latitude
            longitude = MainActivity.latLng!!.longitude
        }
        var i = 0
        markers.forEach { marker ->
            val markerLocation = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = marker.position.latitude
                longitude = marker.position.longitude
            }

            val distance = thisLocation.distanceTo(markerLocation)
            if (distance < minDistance) {
                minDistance = distance
                idx = i
            }
            i++
        }

        if (idx!=-1){
            collect(nearbyMarkers[idx])
        }
    }
    private fun getSearchRadius() {
        try {
            val number = edSearchRadius.text.toString().toInt()
            MainActivity.searchRadius = number
        } catch (e: NumberFormatException) { }
        if(MainActivity.searchRadius<100) MainActivity.searchRadius = 100
    }
    private fun lockMap(force:Boolean?){
        if(force!=null) mapInteract = force
        else mapInteract = !mapInteract;
        updateUI()
        lockMapCamera()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {

             addEditMarkerDialog = view.findViewById<ScrollView>(R.id.addEditMarker) //root.findViewById(R.id.fragmentContainer)

            btnToggleLock = view.findViewById<FloatingActionButton>(R.id.btn_toggle_lock)
            btnAddMarker = view.findViewById<FloatingActionButton>(R.id.btn_add_marker)

            btnToggleLock.setOnClickListener {
                lockMap(null)
            }

            btnAddMarker.setOnClickListener{
                showAddEditMarkerFragment()
            }


            edSearchRadius = view.findViewById(R.id.searchRadius)
            edSearchRadius.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    // This method is called to notify you that, within s, the count characters
                    // beginning at start are about to be replaced by new text with length after.
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    // This method is called to notify you that, within s, the count characters
                    // beginning at start have just replaced old text that had length before.
                }

                override fun afterTextChanged(s: Editable) {
                    // This method is called to notify you that, somewhere within s, the text has
                    // been changed.
                    getSearchRadius()
                    updateMapUI()
                    MainActivity.getMarkersRadius()
                }
            })
            edSearchTitle = view.findViewById(R.id.searchTitle)
            edSearchTitle.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    // This method is called to notify you that, within s, the count characters
                    // beginning at start are about to be replaced by new text with length after.
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    // This method is called to notify you that, within s, the count characters
                    // beginning at start have just replaced old text that had length before.
                }

                override fun afterTextChanged(s: Editable) {
                    // This method is called to notify you that, somewhere within s, the text has
                    // been changed.

                    MainActivity.searchTitle = edSearchTitle.text.toString()
                    MainActivity.getMarkersRadius()
                }
            })

            tvPlacedBy = view.findViewById(R.id.tvPlacedBy)
            edtScore = view.findViewById(R.id.edtScore)
            edtTitle = view.findViewById(R.id.edtTitle)
            edtDescription = view.findViewById(R.id.edtDescription)
            btnPlace = view.findViewById(R.id.btnPlace)
            btnCancel = view.findViewById(R.id.btnCancel)
            btnCollect = view.findViewById(R.id.btn_collect)

            btnCollect.setOnClickListener {
                removeNearby()
            }
            btnPlace.setOnClickListener {
                var score = edtScore.text.toString().toIntOrNull()
                if(score == null){
                    Toast.makeText(requireContext(), "Please enter Coins as a number.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }else{
                    if(score<1){
                        Toast.makeText(requireContext(), "Coins needs to be at least 1.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }else{
                        if(score>MainActivity.profile!!.coins){
                            Toast.makeText(requireContext(), "Insufficient coins. Your coins: ${MainActivity.profile!!.coins}", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }
                }

                if(edtTitle.text.length<3){
                    Toast.makeText(requireContext(), "Title needs to be at least 3 chars long.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if(edtDescription.text.length<3){
                    Toast.makeText(requireContext(), "Description needs to be at least 3 chars long.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                placeMarker(score)
            }

            btnCancel.setOnClickListener {
                resetMarkerDialog()
            }

            getSearchRadius()
            updateMapUI()
            MainActivity.getMarkers(1)
            MainActivity.getMarkersRadius()
            updateUI()

        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                //locationResult ?: return

                for (location in locationResult.locations){
                    MainActivity.latLng = LatLng(location.latitude, location.longitude)
                    if(firstMapLoad==false){
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MainActivity.latLng, 13f))
                        firstMapLoad = true
                    }else{
                        updateMapUI()
                    }
                }
                locationResult.lastLocation?.let { location ->
                    MainActivity.getMarkersRadius()
                    MainActivity.getMarkers(1)
                }
            }
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
    }
    private fun lockMapCamera(){
        if(MainActivity.latLng!=null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MainActivity.latLng, 15f))
    }
    fun makeMarkerIcon(mapsFragment: MapsFragment, text: String, mine:Boolean): BitmapDescriptor {
        val mDensity = mapsFragment.resources.displayMetrics.density
        val mTextPaint = Paint().apply {
            color = (if(mine) Color.BLACK else Color.CYAN)
            textSize = (if(mine) 15 else 18) * mDensity
            flags = Paint.ANTI_ALIAS_FLAG
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val mBackgroundPaint = Paint().apply {
            color = (if(mine) Color.WHITE else Color.BLACK)
        }

        val mTextBounds = Rect()
        mTextPaint.getTextBounds(text, 0, text.length, mTextBounds)

        val mMarkerWidth = mTextBounds.width() + mDensity * 6
        val mMarkerHeight = mTextBounds.height() + mDensity * 3

        val bitmap = Bitmap.createBitmap(mMarkerWidth.toInt(), mMarkerHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawRect(0f, 0f, mMarkerWidth, mMarkerHeight, mBackgroundPaint)

        // Draw text centered on bitmap
        val textBottom = bitmap.height - mDensity
        val textLeft = (bitmap.width - mTextBounds.width()) / 2f
        canvas.drawText(text, textLeft, textBottom, mTextPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.google_style))
        mMap.setOnMarkerClickListener(this)
        val adapter = CustomInfoWindowAdapter(LayoutInflater.from(context))
        mMap.setInfoWindowAdapter(adapter)

/*
        mMap.uiSettings.isZoomGesturesEnabled = false
        mMap.uiSettings.isScrollGesturesEnabled = false
        mMap.uiSettings.isTiltGesturesEnabled = false
        mMap.uiSettings.isRotateGesturesEnabled = false
*/
        // Enable My Location layer
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        mMap.isMyLocationEnabled = true
        startLocationUpdates();
    }


    fun getNearbyMarkers() {
        val thisLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = MainActivity.latLng!!.latitude
            longitude = MainActivity.latLng!!.longitude
        }
        nearbyMarkers.clear()

        val iterator = MainActivity.resultsPlaced.iterator()
        while (iterator.hasNext()) {
            val marker = iterator.next()
            val markerLocation = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = marker.latLng.latitude
                longitude = marker.latLng.longitude
            }

            val distance = thisLocation.distanceTo(markerLocation)
            if (distance < 100) { //less than 10m
                nearbyMarkers.add(marker)
            }
        }
        updateUI()
        updateMarkersUI()
    }

    private fun displayNearestMarkerDistance(location: Location) {
        var minDistance = Float.MAX_VALUE
        markers.forEach { marker ->
            val markerLocation = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = marker.position.latitude
                longitude = marker.position.longitude
            }

            val distance = location.distanceTo(markerLocation)
            if (distance < minDistance) {
                minDistance = distance
            }
        }

        if (minDistance == Float.MAX_VALUE) {
            // No markers added yet
            //Toast.makeText(requireContext(), "No markers added yet.", Toast.LENGTH_LONG).show()
        } else {
            //Toast.makeText(requireContext(), "Distance to the nearest marker: $minDistance m", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {

        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )



        //val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        // fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())


        startedLocationUpdates = true
    }

    private fun stopLocationUpdates() {
        startedLocationUpdates = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }
    private fun hasLocationPermission(): Boolean {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return fineLocationPermission && coarseLocationPermission
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, initialize the location updates.
                    startLocationUpdates()
                } else {
                    // Permission denied, disable functionality or ask again.
                    Toast.makeText(requireContext(), "Location permission is necessary to use requireContext() feature", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
    override fun onStart() {
        super.onStart()
        mapView.onStart()
        instance = this
    }
    override fun onStop() {
        mapView.onStop()
        super.onStop()
        instance = null
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}
