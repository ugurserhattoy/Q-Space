package com.ust.qspace

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ust.qspace.stages.OrderedActivity
import com.ust.qspace.stages.RandomActivity
import com.ust.qspace.trees.PrivacyActivity
import com.ust.qspace.trees.SettingsActivity
import com.ust.qspace.trees.TermsActivity
import kotlinx.android.synthetic.main.activity_lvl.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class LvlActivity : AppCompatActivity() {
    val TAG = "LvLActivity"
    private lateinit var mAdView : AdView
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var nick:String
    private lateinit var levelKey: String
    private lateinit var stageList: Map<String,Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lvl)
        setSupportActionBar(toolbar)

        MobileAds.initialize(this) {}//adMob initialize
        if (Build.VERSION.SDK_INT >= 25){
            mAdView = findViewById(R.id.adViewLvL)
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
        }
        stageList = mapOf("Stage 1" to buttL1, "Stage 2" to buttL2, "Stage 3" to buttL3, "Stage 4" to buttL4,
            "Stage 5" to buttL5, "Stage 6" to buttL6, "Stage 7" to buttL7, "Stage 8" to buttL8,"Stage 9" to buttL9,
            "Stage 10" to buttL10, "Stage 11" to buttL11, "Stage 12" to buttL12, "Stage 13" to buttL13,
            "Stage 14" to buttL14, "Stage 15" to buttL15, "Stage 16" to buttL16, "Stage 17" to buttL17,
            "Stage 18" to buttL18, "Stage 19" to buttL19, "Stage 20" to buttL20, "Stage 21" to buttL21,
            "Stage 22" to buttL22, "Stage 23" to buttL23, "Stage 24" to buttL24, "Stage 25" to buttL25,
            "Stage 26" to buttL26, "Stage 27" to buttL27, "Stage 28" to buttL28, "Stage 29" to buttL29,
            "Stage 30" to buttL30)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference.child("Users")
        nick = intent.getStringExtra("tvName")

        Thread{
            val checkRoomDb = db.stageDao().getAll()
            if (checkRoomDb != null){
                for (item in stageList.keys){
                    val dbStage = db.stageDao().getOne(item)
                    val buton = stageList[item]
                    if (dbStage != null){
                        val roomPoint = dbStage.db_stage_points
                        val roomControl = dbStage.db_stage_control
                        if (roomControl){
                            Log.d(TAG, "$item: Pass")
                        }else{
                            if (roomPoint == 0){
                                buton?.setBackgroundResource(R.drawable.custom_butt_lvl)
                                Log.d(TAG, "$item: Given Up")
                            }else{
                                buton?.setBackgroundResource(R.drawable.custom_butt_stagewon)
                                Log.d(TAG, "$item: Finished")
                            }
                        }
                        Log.d(TAG, "stageList: $item applied")
                    }
                }
            }else{
                Log.d(TAG, "stages doesn't exist yet!")
            }
        }.start()
    }

    override fun onStart() {
        if (Build.VERSION.SDK_INT >= 25){
            animations()
        }else{
            nevermindAndShowTheStages()
        }
        super.onStart()
    }

    private fun nevermindAndShowTheStages(){
        stage_column1.visibility = View.VISIBLE
        stage_column2.visibility = View.VISIBLE
        stage_column3.visibility = View.VISIBLE
        stage_column4.visibility = View.VISIBLE
        stage_column5.visibility = View.VISIBLE
        stage_column6.visibility = View.VISIBLE
        if(points >= 24000){
            buttL6.isEnabled = true
            buttL7.isEnabled = true
            buttL8.isEnabled = true
            buttL9.isEnabled = true
            buttL10.isEnabled = true
            if(points >= 45000){
                buttL11.isEnabled = true
                buttL12.isEnabled = true
                buttL13.isEnabled = true
                buttL14.isEnabled = true
                buttL15.isEnabled = true
                if (points >= 70000){
                    buttL16.isEnabled = true
                    buttL17.isEnabled = true
                    buttL18.isEnabled = true
                    buttL19.isEnabled = true
                    buttL20.isEnabled = true
                    if (points >= 95000){
                        buttL21.isEnabled = true
                        buttL22.isEnabled = true
                        buttL23.isEnabled = true
                        buttL24.isEnabled = true
                        buttL25.isEnabled = true
                        if (points >= 120000){
                            buttL26.isEnabled = true
                            buttL27.isEnabled = true
                            buttL28.isEnabled = true
                            buttL29.isEnabled = true
                            buttL30.isEnabled = true
                        }
                    }
                }
            }

        }

    }

    private fun animations(){
        val ltr = AnimationUtils.loadAnimation(this, R.anim.ltr)
//        val ltr6 = AnimationUtils.loadAnimation(this, R.anim.ltr6)
        runBlocking(Dispatchers.Default) {
            stage_column1.visibility = View.VISIBLE
            stage_column1.startAnimation(ltr)
            ltr.setAnimationListener(object: Animation.AnimationListener{
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {
                    val ltr1 = AnimationUtils.loadAnimation(baseContext, R.anim.ltr1)
                    stage_column2.visibility = View.VISIBLE
                    stage_column2.startAnimation(ltr1)
                    ltr1.setAnimationListener(object: Animation.AnimationListener{
                        override fun onAnimationRepeat(p0: Animation?) {}
                        override fun onAnimationStart(p0: Animation?) {
                            if(points >= 24000){
                                buttL6.isEnabled = true
                                buttL7.isEnabled = true
                                buttL8.isEnabled = true
                                buttL9.isEnabled = true
                                buttL10.isEnabled = true
                            }
                        }
                        override fun onAnimationEnd(p0: Animation?) {
                            val ltr2 = AnimationUtils.loadAnimation(baseContext, R.anim.ltr2)
                            stage_column3.visibility = View.VISIBLE
                            stage_column3.startAnimation(ltr2)
                            ltr2.setAnimationListener(object: Animation.AnimationListener{
                                override fun onAnimationRepeat(p0: Animation?) {}
                                override fun onAnimationStart(p0: Animation?) {
                                    if(points >= 45000){
                                        buttL11.isEnabled = true
                                        buttL12.isEnabled = true
                                        buttL13.isEnabled = true
                                        buttL14.isEnabled = true
                                        buttL15.isEnabled = true
                                    }
                                }
                                override fun onAnimationEnd(p0: Animation?) {
                                    val ltr3 = AnimationUtils.loadAnimation(baseContext, R.anim.ltr3)
                                    stage_column4.visibility = View.VISIBLE
                                    stage_column4.startAnimation(ltr3)
                                    ltr3.setAnimationListener(object: Animation.AnimationListener{
                                        override fun onAnimationRepeat(p0: Animation?) {}
                                        override fun onAnimationStart(p0: Animation?) {
                                            if (points >= 70000){
                                                buttL16.isEnabled = true
                                                buttL17.isEnabled = true
                                                buttL18.isEnabled = true
                                                buttL19.isEnabled = true
                                                buttL20.isEnabled = true
                                            }
                                        }
                                        override fun onAnimationEnd(p0: Animation?) {
                                            val ltr4 = AnimationUtils.loadAnimation(baseContext, R.anim.ltr4)
                                            stage_column5.visibility = View.VISIBLE
                                            stage_column5.startAnimation(ltr4)
                                            ltr4.setAnimationListener(object: Animation.AnimationListener{
                                                override fun onAnimationRepeat(p0: Animation?) {}
                                                override fun onAnimationStart(p0: Animation?) {
                                                    if (points >= 95000){
                                                        buttL21.isEnabled = true
                                                        buttL22.isEnabled = true
                                                        buttL23.isEnabled = true
                                                        buttL24.isEnabled = true
                                                        buttL25.isEnabled = true
                                                    }
                                                }
                                                override fun onAnimationEnd(p0: Animation?) {
                                                    if (points >= 120000){
                                                        buttL26.isEnabled = true
                                                        buttL27.isEnabled = true
                                                        buttL28.isEnabled = true
                                                        buttL29.isEnabled = true
                                                        buttL30.isEnabled = true
                                                    }
                                                    val ltr5 = AnimationUtils.loadAnimation(baseContext, R.anim.ltr5)
                                                    stage_column6.visibility = View.VISIBLE
                                                    stage_column6.startAnimation(ltr5)
                                                }
                                            })
                                        }
                                    })
                                }
                            })
                        }
                    })
                }
            })
        }
    }

    fun showStage(view: View?){
        val buttonStage = (view as Button).text.toString()
        levelKey = "Stage $buttonStage"
        Log.d(TAG, "buttL$buttonStage pressed")
        if(buttonStage == "3" || buttonStage == "8" || buttonStage == "23"){
            val intent = Intent(this@LvlActivity, RandomActivity::class.java)
            intent.putExtra("tvName", nick)
            intent.putExtra("levelKey", levelKey)
            startActivity(intent)
        }else{
            val intent = Intent(this@LvlActivity, OrderedActivity::class.java)
            intent.putExtra("tvName", nick)
            intent.putExtra("levelKey", levelKey)
            startActivity(intent)
        }
    }

    private fun mainMenu(view: View?){
        Log.d(TAG, "mainMenu pressed..")
        val intent = Intent(this@LvlActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun showSettings(view:View?){
        Log.d(TAG, "action_settings pressed!")
        val intent = Intent(this@LvlActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun showProfile(view: View?){
        Log.d(TAG, "Profile pressed..")
        val intent = Intent(this@LvlActivity, ProfileActivity::class.java)
        intent.putExtra("tvName", nick)
        startActivity(intent)
    }

    private fun privacyPolicy(){
        Log.d(TAG, "privacyPolicy pressed..")
        val intent = Intent(this, PrivacyActivity::class.java)
        startActivity(intent)
    }

    private fun termsConditions(){
        Log.d(TAG, "privacyPolicy pressed..")
        val intent = Intent(this, TermsActivity::class.java)
        startActivity(intent)
    }

    fun signOut(view: View?){
        Log.d(TAG, "signOut pressed..")
        auth.signOut()
        startActivity(Intent(this@LvlActivity, LoginActivity::class.java))
        this@LvlActivity.finish()
    }

    override fun onBackPressed() {
        this.finish()
        mainMenu(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_lvl, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when {
            item.itemId == R.id.action_out -> signOut(null)
            item.itemId == R.id.action_profile -> showProfile(null)
            item.itemId == R.id.action_home -> mainMenu(null)
            item.itemId == R.id.action_settings -> showSettings(null)
            item.itemId == R.id.pivacy_policy -> privacyPolicy()
            item.itemId == R.id.terms_condition -> termsConditions()
            else -> {
            }
        }

        return when (item.itemId) {
            R.id.action_out -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}