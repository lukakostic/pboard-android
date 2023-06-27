package com.example.rmasapp.pages

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.rmasapp.MainActivity
import com.example.rmasapp.Placed
import com.example.rmasapp.Profile
import com.example.rmasapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MarkersFragment : Fragment() {
    lateinit var allTable : TableLayout
    lateinit var userTable : TableLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_markers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {

            allTable = view.findViewById(R.id.tableLeaderboard)
            userTable = view.findViewById(R.id.tableUser)

            populateTable(MainActivity.resultsPlaced,allTable)
            populateTable(MainActivity.placedMine,userTable)
        }
    }

    fun populateTable(markers: ArrayList<Placed>, table: TableLayout) {

        // Clear all previous entries
        table.removeAllViews()
        table.addView(createHeaderRow())

        var context = requireContext()

        for (marker in markers) {
            val row = TableRow(requireContext())

            val tvOwnerName = TextView(context).apply {
                text = marker.ownerName
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            val tvCoins = TextView(context).apply {
                text = marker.coins.toString()
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            val tvTitle = TextView(context).apply {
                text = marker.title
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            val tvDescription = TextView(context).apply {
                text = marker.description
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }


            val tvImage = TextView(context).apply {
                text = marker.image
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            row.addView(tvOwnerName)
            row.addView(tvCoins)
            row.addView(tvTitle)
            row.addView(tvDescription)
            row.addView(tvImage)

            row.setOnClickListener {
                MapsFragment.dest = marker.latLng
                //MainActivity.navController.navigate(R.id.nav_gallery)
            }
            table.addView(row)
        }
    }

    fun createHeaderRow(): TableRow {
        var context = requireContext()

        val row = TableRow(context)



        val tvOwnerName = TextView(context).apply {
            text = "Owner"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }


        val tvTitle = TextView(context).apply {
            text = "Title"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        val tvDescription = TextView(context).apply {
            text = "Description"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        val tvCoins = TextView(context).apply {
            text = "Coins"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        val tvImage = TextView(context).apply {
            text = "Image"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        row.addView(tvOwnerName)
        row.addView(tvCoins)
        row.addView(tvTitle)
        row.addView(tvDescription)
        row.addView(tvImage)

        return row
    }

}
