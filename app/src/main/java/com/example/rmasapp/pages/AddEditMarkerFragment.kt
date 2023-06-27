package com.example.rmasapp.pages

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rmasapp.Placed
import com.example.rmasapp.R

class AddEditMarkerFragment : Fragment() {

    private var imageUri: Uri? = null
    private var markerToEdit: Placed? = null // Pass this in if you are editing a marker

    // Other variables here like views

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_edit_marker, container, false)

        // Initialize your views here like btnPlace = view.findViewById(R.id.btnPlace)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (markerToEdit != null) {
            // Populate fields with data from markerToEdit
            // Change text of btnPlace to "Save Edit"
        }

        // Setup listeners for your buttons like btnPlace.setOnClickListener { /* Do something */ }
    }

    private fun pickImage() {
        // Logic for picking image from gallery or taking a photo
    }
}
