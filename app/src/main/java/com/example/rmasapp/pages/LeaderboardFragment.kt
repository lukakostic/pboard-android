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
import com.example.rmasapp.Profile
import com.example.rmasapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeaderboardFragment : Fragment() {
    lateinit var allTable : TableLayout
    lateinit var userTable : TableLayout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase instances

        view.post {

            allTable = view.findViewById(R.id.tableLeaderboard)
            userTable = view.findViewById(R.id.tableUser)

            getTop100Profiles()
            getCurrentUserRank()
        }
    }
/*
    fun populateTable(profiles: List<Profile>) {
        val sortedProfiles = profiles.sortedByDescending { it.xp }


        // Clear all previous entries
        table.removeAllViews()

        for (profile in sortedProfiles) {
            val row = TableRow(this)

            val tvXp = TextView(this).apply {
                text = profile.xp.toString()
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            val tvName = TextView(this).apply {
                text = "${profile.firstName} ${profile.lastName}"
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            val tvEmail = TextView(this).apply {
                text = profile.email
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            row.addView(tvXp)
            row.addView(tvName)
            row.addView(tvEmail)

            table.addView(row)
        }
    }
*/
    fun getTop100Profiles() {
        MainActivity.db.collection("profiles")
            .orderBy("xp", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { result ->
                val profiles = result.map { doc -> doc.toObject(Profile::class.java) }
                populateTable(profiles)
            }
    }

    fun getCurrentUserRank() {
        if(MainActivity.profile == null){
            userTable.removeAllViews()
            return
        }
        val query = MainActivity.db.collection("profiles")
            .orderBy("xp", Query.Direction.DESCENDING)

        query.get().addOnSuccessListener { result ->
            var rank = 1
            for (doc in result) {
                if (doc["email"] == MainActivity.profile?.email) {
                    break
                }
                rank++
            }
            populateUserRankTable(rank, MainActivity.profile!!)
        }
    }

    fun populateTable(profiles: List<Profile>) {

        // Clear all previous entries
        allTable.removeAllViews()
        allTable.addView(createHeaderRow())


        var context = requireContext()

        var rank = 1
        for (profile in profiles) {
            val row = TableRow(requireContext())

            val tvRank = TextView(context).apply {
                text = rank.toString()
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            val tvXp = TextView(context).apply {
                text = profile.xp.toString()
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            val tvName = TextView(context).apply {
                text = "${profile.firstName} ${profile.lastName}"
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            val tvEmail = TextView(context).apply {
                text = profile.email
                textSize = 18f
                setPadding(8, 8, 8, 8)
            }

            row.addView(tvRank)
            row.addView(tvXp)
            row.addView(tvName)
            row.addView(tvEmail)
            row.setOnClickListener {
                openProfile(profile)
            }
            allTable.addView(row)
            rank++
        }
    }
    fun openProfile(profile: Profile) {
        ProfileViewFragment.profile = profile
        MainActivity.navController.navigate(R.id.nav_home)
    }
    fun populateUserRankTable(rank: Int, profile: Profile) {

        // Clear all previous entries
        userTable.removeAllViews()
        userTable.addView(createHeaderRow())


        var context = requireContext()
        val row = TableRow(context)

        val tvRank = TextView(context).apply {
            text = rank.toString()
            textSize = 18f
            setPadding(8, 8, 8, 8)
        }

        val tvXp = TextView(context).apply {
            text = profile.xp.toString()
            textSize = 18f
            setPadding(8, 8, 8, 8)
        }

        val tvName = TextView(context).apply {
            text = "${profile.firstName} ${profile.lastName}"
            textSize = 18f
            setPadding(8, 8, 8, 8)
        }

        val tvEmail = TextView(context).apply {
            text = profile.email
            textSize = 18f
            setPadding(8, 8, 8, 8)
        }

        row.addView(tvRank)
        row.addView(tvXp)
        row.addView(tvName)
        row.addView(tvEmail)

        userTable.addView(row)
    }

    fun createHeaderRow(): TableRow {
        var context = requireContext()

        val row = TableRow(context)

        val tvRank = TextView(context).apply {
            text = "Rank"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        val tvXp = TextView(context).apply {
            text = "XP"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        val tvName = TextView(context).apply {
            text = "Name"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        val tvEmail = TextView(context).apply {
            text = "Email"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }

        row.addView(tvRank)
        row.addView(tvXp)
        row.addView(tvName)
        row.addView(tvEmail)

        return row
    }
}
