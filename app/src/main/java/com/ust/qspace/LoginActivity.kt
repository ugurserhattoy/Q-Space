package com.ust.qspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import android.widget.Toast.*
import androidx.annotation.ContentView
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.ust.qspace.models.ProgressActivity
import kotlinx.android.synthetic.main.activity_login.*
import java.lang.Exception
import android.widget.Button as Button

class LoginActivity : AppCompatActivity() {
    val TAG = "LoginActivity"

    private lateinit var auth:FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database:FirebaseDatabase

    private lateinit var nickName:String
    private lateinit var defaultAvatar: String

    private lateinit var googleSignInClient: GoogleSignInClient

    lateinit var mAdView : AdView

//    private lateinit var context:Context
//    private lateinit var file: File
//    private lateinit var path: File
//    private lateinit var letDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        MobileAds.initialize(this) {}//adMob initialize
        mAdView = findViewById(R.id.adViewLogin)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        val scrollView = findViewById<ScrollView>(R.id.layoutbg)
        val animationDrawable = scrollView.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(4000)
        animationDrawable.setExitFadeDuration(2000)
        animationDrawable.start()

        defaultAvatar = "https://firebasestorage.googleapis.com/v0/b/mathslayer-2771e.appspot.com/o/Images%2FplutoAvatarForUsers.jpg?alt=media&token=c4bccc9a-a11e-4c16-b20c-e076efca6aec"

//        context = this.applicationContext!!
//
//        path = context.filesDir
//        letDirectory = File(path, "LET")

        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference.child("Users")
        auth = FirebaseAuth.getInstance()

        val yes = findViewById<Button>(R.id.buttVar)
        val yok = findViewById<Button>(R.id.buttYok)
        val nick = findViewById<EditText>(R.id.etNick)
        val gir = findViewById<Button>(R.id.buttSign)
        val yarat = findViewById<Button>(R.id.buttLogin)
        val posta = findViewById<EditText>(R.id.etEmail)
        val sifre = findViewById<EditText>(R.id.etPassword)

        yok.isClickable = false
        yok.visibility = View.INVISIBLE
        gir.isClickable = false
        gir.visibility = View.INVISIBLE
        buttResetPass.isClickable = false
        buttResetPass.visibility = View.INVISIBLE

        yes.setOnClickListener {
            yok.isClickable = true
            yok.visibility = View.VISIBLE
            gir.isClickable = true
            gir.visibility = View.VISIBLE
            yes.isClickable = false
            yes.visibility = View.INVISIBLE
            yarat.isClickable = false
            yarat.visibility = View.INVISIBLE
            nick.isClickable = false
            nick.visibility = View.INVISIBLE
            buttResetPass.isClickable = true
            buttResetPass.visibility = View.VISIBLE
        }

        yok.setOnClickListener {
            yok.isClickable = false
            yok.visibility = View.INVISIBLE
            gir.isClickable = false
            gir.visibility = View.INVISIBLE
            yes.isClickable = true
            yes.visibility = View.VISIBLE
            yarat.isClickable = true
            yarat.visibility = View.VISIBLE
            nick.isClickable = true
            nick.visibility = View.VISIBLE
            buttResetPass.isClickable = false
            buttResetPass.visibility = View.INVISIBLE
        }

        val loginTextWatcher = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val usernameInput = nick.text.toString().trim()
                val mailInput = posta.text.toString().trim()
                val passwordInput = sifre.text.toString().trim()

                if (yok.visibility == View.INVISIBLE){
                    buttLogin.isEnabled = !usernameInput.isEmpty() && !passwordInput.isEmpty() && !mailInput.isEmpty()
                }else{
                    buttSign.isEnabled = !mailInput.isEmpty() && passwordInput.isNotEmpty()
                }

            }

            override fun afterTextChanged(s: Editable) {
            }
        }
        posta.addTextChangedListener(loginTextWatcher)
        sifre.addTextChangedListener(loginTextWatcher)
        nick.addTextChangedListener(loginTextWatcher)
        //Start of config_signin
        //Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        //End of config_signin
        googleSignInClient = GoogleSignIn.getClient(this, gso)

    }

    public override fun onStart() {
        super.onStart()
        //Check user sign(non null); update UI accordingly
        var currentUser = auth.currentUser
        updateUI(currentUser)
    }

    fun buttLoginEvent(view:View){
        Log.d(TAG, "Login button pressed!")
        progressBarLogin.visibility = View.VISIBLE
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if(view != null){
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
        val username = etNick.text.toString()
        var checkNick:Boolean = true

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, "dblistenervalueevent cancelled")
            }

            override fun onDataChange(p0: DataSnapshot) {

                p0.children.forEach {
                    Log.d(TAG, it.toString())
                    var nickName = it.child("nickName").value as String
                    if (nickName == username){
                        checkNick = false
                        Log.d(TAG, "checkNick = $checkNick")
                    }else{
                        Log.d(TAG,"checkNick = $checkNick")
                    }
                }
                if (checkNick){
                    Log.d(TAG, "username is available")
                    tvNickError.visibility = View.INVISIBLE
                    LoginToSystem(etEmail.text.toString(), etPassword.text.toString())
                }else{
                    Log.d(TAG, "usernameError; username is already taken $checkNick")
                    progressBarLogin.visibility = View.GONE
                    tvNickError.text = getString(R.string.nick_error)
                    tvNickError.visibility = View.VISIBLE
                    val toast = makeText(baseContext, getString(R.string.nick_error_toast), LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                }
            }
        })
    }

    fun LoginToSystem(email:String, password:String){
        nickName = etNick.text.toString()
        Log.d(TAG, "createAccount:$email")
        if(!validateForm()){
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this){task ->
            if (task.isSuccessful){
                //Success sign in -> update UI
                Log.d(TAG, "createUserWithEmail:Success")
                progressBarLogin.visibility = View.GONE
                val user = auth.currentUser

                val userId = user!!.uid
                val userDb = databaseReference.child(userId)
                userDb.child("nickName").setValue(nickName)
                userDb.child("eMail").setValue(user.email)
                userDb.child("points").setValue(0)
                userDb.child("level").setValue("Dactyl")
                userDb.child("avatar").setValue(defaultAvatar)

//                letDirectory.mkdirs()
//                file = File(letDirectory, "$nickName.txt")
//                file.writeText("100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" +
//                        "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n" + "100\n")

                updateUI(user)
                sendEmailVerification()
            }else{
                //Fail -> display message below
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                progressBarLogin.visibility = View.GONE
                val toast = makeText(baseContext, getString(R.string.auth_fail), LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                updateUI(null)
            }
        }
    }

    fun buttSigninEvent(view: View){
        progressBarLogin.visibility = View.VISIBLE
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if(view != null){
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
        SignInToSystem(etEmail.text.toString(), etPassword.text.toString())
    }

    fun SignInToSystem(email:String, password: String){
        Log.d(TAG, "signIn:$email")

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this){task->
            if (task.isSuccessful){
                Log.d(TAG, "signInWithEmail:success")
                progressBarLogin.visibility = View.GONE
                val user = auth.currentUser
                updateUI(user)
            }else{
                Log.w(TAG, "signInWithEmail:failure!",task.exception)
                progressBarLogin.visibility = View.GONE
                val toast = makeText(baseContext, getString(R.string.auth_fail), LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                updateUI(null)
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = etEmail.text.toString()
        if(TextUtils.isEmpty(email)) {
            etEmail.error="Required."
            valid = false
        }else{
            etEmail.error = null
        }

        val password = etPassword.text.toString()
        if (TextUtils.isEmpty(password)){
            etPassword.error = "Required."
            valid = false
        }else {
            etPassword.error = null
        }

        return valid
    }
    //Resetting password by e-mail
    fun resetPassword(view: View){
        val emailAddress = etEmail.text.toString()

        if (emailAddress.contains("@",ignoreCase = true)){
                try {
                    auth.sendPasswordResetEmail(emailAddress)
                        .addOnCompleteListener {
                            if (it.isSuccessful){
                                Log.d(TAG, "Reset password e-mail sent!")
                                val toast = makeText(this, getString(R.string.reset_email_sent,emailAddress), LENGTH_LONG)
                                toast.setGravity(Gravity.TOP,0,150)
                                toast.show()
                            }else{
                                Log.d(TAG, "Reset pw e-mail couldn't sent!")
                                val toast = makeText(this, getString(R.string.reset_email_failed), LENGTH_LONG)
                                toast.setGravity(Gravity.TOP,0,150)
                                toast.show()
                            }
                        }
                }catch (e:Exception){
                    Log.w(TAG, "Check connection or...",e)
                }
            }else{
            val toast = makeText(this, getString(R.string.enter_email), LENGTH_LONG)
            toast.setGravity(Gravity.TOP,0,150)
            toast.show()
        }
    }

    fun updateUI(user: FirebaseUser?){
//        hideProgressDialog()
        if (user != null) {

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }else{
            val toast = makeText(baseContext, "Log in or Create an Account.", LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    companion object{
        private const val TAG = "EmailPassword"

        private const val RC_SIGN_IN = 9001
    }

    //GOOGLE
    fun signInWithGoogle(view: View) {
        progressBarLogin.visibility = View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            progressBarLogin.visibility = View.GONE
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                Log.d(TAG, "requestCode = $requestCode ///// RC_SIGN_IN = $RC_SIGN_IN")
                Snackbar.make(layoutbg, getString(R.string.auth_fail), Snackbar.LENGTH_LONG).show()
                // ...
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    progressBarLogin.visibility = View.GONE
                    val user = auth.currentUser

                    val userId = user!!.uid
                    val userDb = databaseReference.child(userId)
                    userDb.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {}
                        override fun onDataChange(p0: DataSnapshot) {
                            if (p0.hasChildren()){
                                updateUI(user)
                            }else{
                                userDb.child("nickName").setValue(acct.displayName)
                                Log.d(TAG, "nickname = ${acct.displayName}")
                                userDb.child("eMail").setValue(user.email)
                                Log.d(TAG, "email = ${user.email}")
                                userDb.child("points").setValue(0)
                                userDb.child("level").setValue("Dactyl")
                                userDb.child("avatar").setValue(acct.photoUrl.toString())
                                Log.d(TAG, "avatar url = ${acct.photoUrl}")
                                updateUI(user)
                            }
                        }
                    })
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    progressBarLogin.visibility = View.GONE
                    Snackbar.make(layoutbg, getString(R.string.auth_fail), Snackbar.LENGTH_LONG).show()
                    updateUI(null)
                }
                // ...
            }
    }
    //Google End
    private fun sendEmailVerification() {
        // Disable button
//        verifyEmailButton.isEnabled = false

        // [START send_email_verification]
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                // [START_EXCLUDE]
                // Re-enable button
//                verifyEmailButton.isEnabled = true

                if (task.isSuccessful) {
                    val toast = makeText(baseContext, getString(R.string.verify_email_sent), LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                } else {
                    Log.e(TAG, "sendEmailVerification", task.exception)
                    val toast = makeText(baseContext, getString(R.string.failed_verify_email), LENGTH_SHORT)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                }
                // [END_EXCLUDE]
            }
        // [END send_email_verification]
    }
}
