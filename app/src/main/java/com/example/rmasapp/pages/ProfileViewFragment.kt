package com.example.rmasapp.pages

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.rmasapp.MainActivity
import com.example.rmasapp.Profile
import com.example.rmasapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewFragment : Fragment() {

    companion object {
        var profile :Profile? = null
    }
    private var isEdit = false

    private var takePicturePending = false
    private var pickPicturePending = false

    private  lateinit var rootView :View

    private lateinit var profileImage: ImageView
    private lateinit var addImageText: TextView

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2

    // UI elements
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvXp: TextView
    private lateinit var tvCoins: TextView

    private lateinit var tvFirstNameEdit: TextView
    private lateinit var tvLastNameEdit: TextView
    private lateinit var tvEmailEdit: TextView
    private lateinit var tvPhoneEdit: TextView
    private lateinit var tvPasswordEdit: TextView
    private lateinit var btnEdit : Button
    private lateinit var btnLogout : Button


    val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        // Handle the returned bitmap
        updateImage(bitmap)
    }

    val pickPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

        // Handle the returned Uri
        val bitmap = uri?.let {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
        }
        updateImage(bitmap)


    }

    override fun onResume() {
        super.onResume()

        // Check the flags to launch the actions
    /*
        if (takePicturePending) {
            takePicture.launch(null)
            takePicturePending = false
        } else if (pickPicturePending) {
            pickPicture.launch("image/*")
            pickPicturePending = false
        }
     */
     */
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile_view, container, false)
    }
    private fun logout(){
        Toast.makeText(requireContext(), "Logout", Toast.LENGTH_SHORT).show()

        MainActivity.auth.signOut()
            MainActivity.profile = null

            MainActivity.instance.updateNavigation()
            MainActivity.navController.navigate(R.id.nav_login)

            // Optionally, you can navigate back to your Login Activity
            // val intent = Intent(this, LoginActivity::class.java)
            // startActivity(intent)
            // finish()

    }
    private fun login(profile: Profile){
        MainActivity.profile = profile

        //MainActivity.navController.navigate(R.id.nav_home)
    }
    private fun modeToggle(v:Boolean){
        var visible = v
        val container = rootView.findViewById<ViewGroup>(R.id.container)

        for (i in 0 until container.childCount) {
            val childView = container.getChildAt(i)

            if(childView.id == R.id.profileImageHolder) continue
            if(childView.id == R.id.loginForm) visible=!visible
            childView.isVisible = visible
        }
    }
    private fun goView(){
        isEdit = false
        modeToggle(true)
    }
    private fun goEdit(){
        isEdit = true
        tvFirstNameEdit.setText(MainActivity.profile!!.firstName)
        tvLastNameEdit.setText(MainActivity.profile!!.lastName)
        tvEmailEdit.setText(MainActivity.profile!!.email)
        tvPhoneEdit.setText(MainActivity.profile!!.phone)
        tvPasswordEdit.setText(MainActivity.profile!!.password)
        modeToggle(false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {

            rootView = view


            tvName = view.findViewById(R.id.tvName)
            tvEmail = view.findViewById(R.id.tvEmail)
            tvPhone = view.findViewById(R.id.tvPhone)
            tvXp = view.findViewById(R.id.tvXp)
            tvCoins = view.findViewById(R.id.tvCoins)
            tvFirstNameEdit = view.findViewById(R.id.tvFirstNameEdit)
            tvLastNameEdit = view.findViewById(R.id.tvLastNameEdit)
            tvEmailEdit = view.findViewById(R.id.tvEmailEdit)
            tvPhoneEdit = view.findViewById(R.id.tvPhoneEdit)
            tvPasswordEdit = view.findViewById(R.id.tvPasswordEdit)

            profileImage = view.findViewById(R.id.profileImage)
            addImageText = view.findViewById(R.id.addImageText)
            profileImage.setOnClickListener {
                showImageOptionDialog()
            }


            btnLogout = view.findViewById<Button>(R.id.btnLogout)
            btnLogout.setOnClickListener {
                logout()
            }

            btnEdit = view.findViewById<Button>(R.id.btnEditProfile)
            btnEdit.setOnClickListener {
                goEdit()
            }

            view.findViewById<Button>(R.id.btnSaveEditedProfile).setOnClickListener {
                updateProfile()
                goView()
            }
            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                goView()
            }

            goView()
            if(Companion.profile == null) {
                MainActivity.fetchMyProfile() {

                    draw(MainActivity.profile,true)
                }
            }else{
                draw(Companion.profile, false)
                Companion.profile = null
            }


        }
    }
    private fun updateProfile() {
        // get values from EditTexts
        val firstName = tvFirstNameEdit.text.toString()
        val lastName = tvLastNameEdit.text.toString()
        val email = tvEmailEdit.text.toString()
        val phone = tvPhoneEdit.text.toString()
        val password = tvPasswordEdit.text.toString()

        // update MainActivity.profile
        MainActivity.profile!!.firstName = firstName
        MainActivity.profile!!.lastName = lastName
        MainActivity.profile!!.email = email
        MainActivity.profile!!.phone = phone
        MainActivity.profile!!.password = password

        // update Firebase
        val profileMap = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to phone,
            "password" to password,
            "xp" to MainActivity.profile!!.xp,
            "coins" to MainActivity.profile!!.coins,
            "markersPlaced" to MainActivity.profile!!.markersPlaced
        )

        MainActivity.db.collection("profiles").document(MainActivity.user!!.uid)
            .set(profileMap)
            .addOnSuccessListener {
                draw(MainActivity.profile,true)
                Log.d("INFO", "Profile successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("INFO", "Error updating profile", e)
            }
    }

    private fun draw(profile: Profile?, min: Boolean) {
        var mine = min
        if(profile!!.email == MainActivity.profile!!.email) mine = true
        profile?.let {
            tvName.text = it.firstName + " " + it.lastName
            tvEmail.text = it.email
            tvXp.text = it.xp.toString()
            tvCoins.text = it.coins.toString()
            tvPhone.text = it.phone

            if (mine) {
                btnEdit.isVisible = true
                btnLogout.isVisible = true
                addImageText.isVisible = true

            } else {
                addImageText.isVisible = false
                btnEdit.isVisible = false
                btnLogout.isVisible = false
            }
        }
    }



    private fun showImageOptionDialog() {
        if(addImageText.isVisible == false) return
        val items = mutableListOf("Take image from camera", "Select image from storage")

        val imageDrawable = profileImage.drawable
        if (imageDrawable != null) {
            items.add("Remove image")
        }

        AlertDialog.Builder(requireContext()).setItems(items.toTypedArray()) { _, which ->
            when (which) {
                //0 -> takePicturePending = true
                //1 -> pickPicturePending = true
                0 -> takePicture.launch(null)
                1 -> pickPicture.launch("image/*")
                //0 -> takeImageFromCamera.launch(null)
                //1 -> pickImagesFromGallery.launch("image/*")
                2 -> updateImage(null)
            }

        }.show()
    }

    private fun updateImage(bitmap: Bitmap?) {
        profileImage.setImageBitmap(bitmap)
        addImageText.visibility = if (bitmap == null) View.VISIBLE else View.GONE
    }

}
