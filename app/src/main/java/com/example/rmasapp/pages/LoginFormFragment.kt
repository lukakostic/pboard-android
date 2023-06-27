package com.example.rmasapp.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.rmasapp.MainActivity
import com.example.rmasapp.Profile
import com.example.rmasapp.R
import com.example.rmasapp.databinding.FragmentSlideshowBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFormFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    // UI elements
    private lateinit var etEmailLogin: EditText
    private lateinit var etPasswordLogin: EditText

    private lateinit var formLogin: LinearLayout
    private lateinit var formRegister: LinearLayout
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText

    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var btnSwitchLogin: Button
    private lateinit var btnSwitchRegister: Button


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_login_form, container, false).also {

        }
        val root: View = binding.root


        //fragmentContainer = childFragmentManager.findFragmentById(R.id.fragmentContainer) as FrameLayout //root.findViewById(R.id.fragmentContainer)

        return root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.post {

            view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
                Snackbar.make(view, "Contact Support not available", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }

            // Initialize UI elements
            formLogin = view.findViewById(R.id.formLogin)
            formRegister = view.findViewById(R.id.formRegister)
            etEmailLogin = view.findViewById(R.id.etEmailLogin)
            etPasswordLogin = view.findViewById(R.id.etPasswordLogin)
            etEmail = view.findViewById(R.id.etEmail)
            etPassword = view.findViewById(R.id.etPassword)
            etPasswordConfirm = view.findViewById(R.id.etConfPassword)
            etFirstName = view.findViewById(R.id.etFirstName)
            etLastName = view.findViewById(R.id.etLastName)
            etPhone = view.findViewById(R.id.etPhone)
            btnLogin = view.findViewById(R.id.btnLogin)
            btnRegister = view.findViewById(R.id.btnRegister)
            btnSwitchLogin = view.findViewById(R.id.btnSwitchLogin)
            btnSwitchRegister = view.findViewById(R.id.btnSwitchRegister)

            // Set click listeners
            btnLogin.setOnClickListener {
                login()
            }
            btnRegister.setOnClickListener {
                register()
            }
            btnSwitchRegister.setOnClickListener {
                formLogin.visibility = View.GONE
                formRegister.visibility = View.VISIBLE
            }
            btnSwitchLogin.setOnClickListener {
                formRegister.visibility = View.GONE
                formLogin.visibility = View.VISIBLE
            }
        }
    }

    private fun login() {
        val email = etEmailLogin.text.toString()
        val password = etPasswordLogin.text.toString()

        MainActivity.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFormFragment", "signInWithEmail:success")
                    getProfile(callback = { profile ->
                        MainActivity.profile = profile
                        MainActivity.instance.updateNavigation()
                        Toast.makeText(requireContext(), "Welcome "+profile.email, Toast.LENGTH_SHORT).show()
                        MainActivity.navController.navigate(R.id.nav_home)
                    })
                    // Navigate to ProfileViewFragment or somewhere else as needed
                } else {
                    Toast.makeText(requireContext(), "Failed to log in: ${task.exception}", Toast.LENGTH_LONG).show()
                    Log.w("LoginFormFragment", "signInWithEmail:failure", task.exception)
                }
            }
    }

    private fun register() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        val password2 = etPasswordConfirm.text.toString()
        if(password != password2){
            Toast.makeText(requireContext(), "Passwords dont match!", Toast.LENGTH_LONG).show()
            return
        }
        MainActivity.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    newProfile()
                    Log.d("LoginFormFragment", "createUserWithEmail:success")
                    //MainActivity.Companion.user = auth.currentUser
                    // Navigate to ProfileViewFragment or somewhere else as needed
                } else {
                    Toast.makeText(requireContext(), "Register fail ${task.exception}", Toast.LENGTH_LONG).show()
                    Log.w("LoginFormFragment", "createUserWithEmail:failure", task.exception)
                }
            }
    }

    private fun newProfile() {
        val userId = MainActivity.user?.uid
        if (userId != null) {

            val profile = Profile(
                firstName = etFirstName.text.toString(),
                lastName = etLastName.text.toString(),
                email = etEmail.text.toString(),
                phone = etPhone.text.toString(),
                password = "", // we are not storing password in Firestore
                xp = 0,
                coins = 1000,
                markersPlaced = 0
            )
            MainActivity.db.collection("profiles").document(userId)
                .set(profile)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile successfully created!", Toast.LENGTH_SHORT).show()
                    Log.d("LoginFormFragment", "Profile successfully created!")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error creating profile $e", Toast.LENGTH_LONG).show()
                    Log.w("LoginFormFragment", "Error creating profile", e)
                }
        }
    }

    private fun getProfile(callback:(profile: Profile)->Unit, user_id:String? = null) {
        var userId = user_id
        if(userId == null) userId = MainActivity.user?.uid
        if(userId == null) throw Error("No userId passed or logged in")

        MainActivity.db.collection("profiles").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    document.toObject<Profile>(Profile::class.java)?.let { callback(it) }
                } else {
                    Log.d("INFO", "No such document")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error creating profile $e", Toast.LENGTH_LONG).show()
                Log.w("LoginFormFragment", "Error creating profile", e)
            }

    }

}
