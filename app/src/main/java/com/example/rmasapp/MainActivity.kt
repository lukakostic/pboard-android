package com.example.rmasapp

import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavController
import com.example.rmasapp.databinding.ActivityMainBinding
import com.example.rmasapp.pages.MapsFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var instance : MainActivity

        var profile : Profile? = null

        val resultsPlaced = ArrayList<Placed>()
        val placedMine = ArrayList<Placed>()
        var latLng  : LatLng? = null
        var searchRadius = 1000
        var searchTitle = ""

        lateinit var auth: FirebaseAuth
        val user: FirebaseUser? get() = auth.currentUser

        lateinit var db: FirebaseFirestore

        lateinit var navController : NavController
        lateinit var navView : NavigationView

        private fun fetchProfile(id:String,callback: (profile:Profile?)->Unit ) {

            MainActivity.db.collection("users").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val profile = document.toObject(Profile::class.java)
                        callback(profile)
                    } else {
                        Log.d("ProfileViewFragment", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("ProfileViewFragment", "get failed with ", exception)
                }

        }
        fun fetchMyProfile(callback: (profile:Profile?)->Unit ) {
            val userId = MainActivity.user!!.uid

            MainActivity.db.collection("profiles").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val profile = document.toObject(Profile::class.java)
                        MainActivity.profile = profile
                        callback(profile)
                    } else {
                        Log.d("ProfileViewFragment", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("ProfileViewFragment", "get failed with ", exception)
                }

        }

        fun getMarkers(purpose: Int){

            //purpose = 0 -> add to results
            //1 -> get only user
            //2 -> all
            var me = MainActivity.user!!.uid

            var q = MainActivity.db.collection("markers")
            var res = q.whereEqualTo("owner",me).get()
            //else if(purpose==0) q.whereNotEqualTo("owner",me).get()
            //else q.get()
            res.addOnSuccessListener { querySnapshot ->
                MainActivity.placedMine.clear()
                val markers = querySnapshot.documents.mapNotNull { doc ->
                    var retrievedMarker = getPlacedFromDoc(doc)
                    if(retrievedMarker != null){
                        //if(purpose==0)
                        //    resultsPlaced.add(retrievedMarker)
                        //else if(purpose==1)
                        MainActivity.placedMine.add((retrievedMarker))
                    }
                }

                MapsFragment.instance?.updateMarkersUI()
            }.addOnFailureListener { e ->
                Log.w("INFO", "Error getting documents: ", e)
            }

        }
        fun getMarkersRadius(){
            if(MainActivity.latLng==null) return
            var me = MainActivity.user!!.uid
            getMarkersWithinDistance(0, MainActivity.latLng!!, MainActivity.searchRadius.toDouble()).addOnSuccessListener { querySnapshot ->
                MainActivity.resultsPlaced.clear()
                val markers = querySnapshot.documents.mapNotNull { doc ->

                    var retrievedMarker = getPlacedFromDoc(doc)
                    if(retrievedMarker != null) {
                        if(retrievedMarker.owner==me) return@mapNotNull

                        if(searchTitle.length>0){
                            val title = retrievedMarker.title
                            val windowSize = searchTitle.length
                            var match = false

                            for (i in 0 until title.length - windowSize + 1) {
                                val window = title.substring(i, i + windowSize)
                                if (levenshtein(window, searchTitle) <= 2) {
                                    match = true
                                    break
                                }
                            }

                            if( match == false ) return@mapNotNull
                        }

                        var results = FloatArray(1)
                        Location.distanceBetween(
                            latLng!!.latitude,
                            latLng!!.longitude,
                            retrievedMarker.latLng.latitude,
                            retrievedMarker.latLng.longitude,
                            results
                        )
                        if (results[0] <= MainActivity.searchRadius.toDouble()) {
                            MainActivity.resultsPlaced.add(retrievedMarker)
                        } else {
                            null
                        }
                    }
                }
                MapsFragment.instance?.getNearbyMarkers()
                // Now 'markers' contains all markers within the specified distance
            }.addOnFailureListener { e ->
                Log.w("INFO", "Error getting documents: ", e)
            }
        }
        fun getMarkersWithinDistance(purpose: Int, latLng: LatLng, distance: Double): Task<QuerySnapshot> {
            //purpose = 0 -> add to results
            //1 -> get only user
            //2 -> all
            var me = MainActivity.user!!.uid

            val radiusInDegrees = distance / 111300f // Roughly converts meters to degrees

            val minLat = latLng.latitude - radiusInDegrees
            val maxLat = latLng.latitude + radiusInDegrees

            val minLon = latLng.longitude - radiusInDegrees
            val maxLon = latLng.longitude + radiusInDegrees

            var q = MainActivity.db.collection("markers")
                .whereGreaterThanOrEqualTo("latLng", latLngToArray(LatLng(minLat, minLon)))
                .whereLessThanOrEqualTo("latLng", latLngToArray(LatLng(maxLat, maxLon)))


            var res =
            //if(purpose==1) q.whereEqualTo("owner",me).get()
            //else if(purpose==0) q.whereNotEqualTo("owner",me).get()
                //else
                q.get()

            return res
        }

        fun levenshtein(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }

            for (i in 0 until a.length + 1) {
                for (j in 0 until b.length + 1) {
                    when {
                        i == 0 -> dp[0][j] = j
                        j == 0 -> dp[i][0] = i
                        else -> {
                            dp[i][j] = minOf(
                                dp[i - 1][j - 1] + costOfSubstitution(a, b, i, j),
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                            )
                        }
                    }
                }
            }

            return dp[a.length][b.length]
        }

        fun costOfSubstitution(a: String, b: String, i: Int, j: Int) = if (a[i - 1] == b[j - 1]) 0 else 1

    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    public fun updateNavigation(){
        var logged = (profile!=null)

        val menu = navView.menu
        menu.findItem(R.id.nav_login)?.isVisible = !logged //if (logged) View.GONE else View.VISIBLE
        menu.findItem(R.id.nav_home)?.isEnabled = logged
        menu.findItem(R.id.nav_gallery)?.isEnabled = logged
        menu.findItem(R.id.nav_markers)?.isEnabled = logged
        menu.findItem(R.id.nav_leaderboard)?.isEnabled = logged
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)


        val drawerLayout: DrawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_login, R.id.nav_home, R.id.nav_gallery, R.id.nav_markers, R.id.nav_leaderboard), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        ///~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~///
        instance = this
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        updateNavigation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onStart() {
        super.onStart()
        if(user != null){
            fetchMyProfile() {

                updateNavigation()
                navController.navigate(R.id.nav_gallery)
            }
        }

    }
}