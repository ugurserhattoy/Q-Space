package com.ust.qspace

import android.animation.*
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnRepeat
import androidx.room.Room
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.ust.qspace.models.SettingsPrefs
import com.ust.qspace.models.metUfo
import com.ust.qspace.models.playMusic
import com.ust.qspace.room.AppRoomDatabase
import com.ust.qspace.room.AppRoomEntity
import com.ust.qspace.services.MusicService
import com.ust.qspace.stages.RandomActivity
import com.ust.qspace.trees.PrivacyActivity
import com.ust.qspace.trees.SettingsActivity
import com.ust.qspace.trees.TermsActivity
import kotlinx.android.synthetic.main.activity_main.*

lateinit var email: String
lateinit var uid: String
lateinit var avatar: String
lateinit var uName: String
lateinit var db:AppRoomDatabase
lateinit var level: String
var points: Long = 0
var verifiedCheck = false
lateinit var animSet: AnimatorSet
lateinit var ufoPauseAnimSet: AnimatorSet
var bgMusicIsRunning = false
var firstRunControl = true

private lateinit var firebaseAnalytics: FirebaseAnalytics
private lateinit var auth: FirebaseAuth
private lateinit var database: FirebaseDatabase
private lateinit var databaseReference: DatabaseReference
private lateinit var commsReference: DatabaseReference

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    var uidAssignedCheck = false
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var mediaPlayer: MediaPlayer
//    private lateinit var mInterstitialAd: InterstitialAd
    private lateinit var firstRunWindow: PopupWindow
    lateinit var userUidEmailAssignTask: Runnable
    lateinit var mainHandler: Handler
    lateinit var mAdView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mainHandler = Handler(Looper.getMainLooper())

        mAdView = findViewById(R.id.adViewMain)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        //InterstitialAd part
        /*mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-7262139641436003/2457796701"
        mInterstitialAd.loadAd(AdRequest.Builder().build())*/
        //

        val constraintLayout = findViewById<ConstraintLayout>(R.id.layoutbg)
        val animationDrawable = constraintLayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()

        animation()

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference.child("Users")

        val user = auth.currentUser

        userUidEmailAssignTaskHandling(user)
        //sayaç burada yatmaktadır yiğen
//        view_timer.base = SystemClock.elapsedRealtime()
//        view_timer.start()
//
//        buttDeneme.setOnClickListener {
//            view_timer.stop()
//            var zaman = view_timer.text.turnToString()
//            textDeneme.text = zaman
//        }
        mediaPlayer = MediaPlayer.create(this, R.raw.ufo)
        mediaPlayer.isLooping = false
        mediaPlayer.setVolume(60f, 60f)
    }

    override fun onStart() {
        if (!bgMusicIsRunning){
            val settings = SettingsPrefs(this)
            val bgMusic = settings.getSetting(playMusic)
            if (bgMusic){
                startService(Intent(this, MusicService::class.java))
                bgMusicIsRunning = true
            }
        }
        super.onStart()
    }

    private fun userUidEmailAssignTaskHandling(user: FirebaseUser?){
        userUidEmailAssignTask = object : Runnable{
            override fun run() {
                var tryCounts = 1
                userUidEmailAssignReload(user)
                if (user == null && tryCounts<5){
                    tryCounts++
//                    Log.d(TAG, "userUidEmailAssignTaskHandling: postDelayed || tryCounts = $tryCounts")
                    mainHandler.postDelayed(this, 3000)
                }else{
                    if (!uidAssignedCheck){
                        mainHandler.removeCallbacks(userUidEmailAssignTask)
                        signOut()
//                        Log.d(TAG, "userUidEmailAssignTaskHandling: removedCallback after 3 tries called signOut")
                    }
                }
            }
        }
        mainHandler.post(userUidEmailAssignTask)
    }

    private fun userUidEmailAssignReload(user: FirebaseUser?){
        try{//uid kotlin.nullPointException
            uid = user!!.uid
            email = user.email.toString()
            user.reload()
            if (uid != null){
                uidAssignedCheck = true
//                Log.d(TAG, "userUidEmailAssignReload: uidAssignedCheck = true ")
            }
        }catch (ex:Exception){
//            Log.d(TAG, "userUidEmailAssignReload: Assign ex: $ex")
        }
        if (uidAssignedCheck){
            buildRoomDBIfUserIdAssigned()
            parseUserProfileInformationsFromFirebase()
            checkIfUserVerifiedOrNot(user)  //Verified or Not Warning!
            googleSignInOptionsClient()
            try {
                mainHandler.removeCallbacks(userUidEmailAssignTask)
            }catch (ex:Exception){
//                Log.d(TAG, "userUidEmailAssignReload: removeCallBacks ex: $ex")
            }
        }
    }

    private fun buildRoomDBIfUserIdAssigned(){
        Thread{
            db=
                Room.databaseBuilder(
                    applicationContext,
                    AppRoomDatabase::class.java,
                    "RoomDB:$uid"
                ).build()
        }.start()
    }

    private fun parseUserProfileInformationsFromFirebase(){
        val userReference = databaseReference.child(uid)
        /*userName.text  = userReference.orderByChild("nickName").turnToString()*/
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                uName = p0.child("nickName").value as String
                avatar = p0.child("avatar").value as String
                points = p0.child("points").value as Long
                Picasso.get().load(avatar).into(iv_avatar_circle)
                tvName.text = uName
//                Log.d(TAG, "onCreate: avatar and uName assigned")
                animationTop()
                levelTagClarification()
            }
            override fun onCancelled(p0: DatabaseError) {
//                Log.d(TAG, "Something's Wrong: User information get FAILED")
                val toast = makeText(baseContext, getString(R.string.listener_cancelled), LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        })
    }

    private fun checkIfUserVerifiedOrNot(user: FirebaseUser?){
        if (user != null) {////uid kotlin.nullPointException addings
            verifiedCheck = user.isEmailVerified
        }
        if (verifiedCheck){
//            Log.d(TAG, "verifiedCheck = ${user?.isEmailVerified}")
            groupWarn.visibility = View.GONE
        }else{
//            Log.d(TAG, "verifiedCheck = ${user?.isEmailVerified}")
            groupWarn.visibility = View.VISIBLE
        }
    }

    private fun googleSignInOptionsClient(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        commsReference = database.reference.child("Posts")
    }

    fun onStartAnimation(){
        val ufo = findViewById<ConstraintLayout>(R.id.ufo_layout)
        var transX = -300f
        var transX1 = -500f
        ufo.visibility = View.VISIBLE
        //ObjectAnimator
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X,0f,1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f,1f)
        val scaleufo = ObjectAnimator.ofPropertyValuesHolder(ufo, scaleX, scaleY).apply {
            duration = 15000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val translationX = ObjectAnimator.ofFloat(ufo,View.TRANSLATION_X, transX).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val translationX1 = ObjectAnimator.ofFloat(ufo, View.TRANSLATION_X, transX1).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val translationY = ObjectAnimator.ofFloat(ufo,View.TRANSLATION_Y, -1000f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 30000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        animSet = AnimatorSet().apply {
            play(scaleufo).with(translationX).with(translationY)
            play(translationX1).after(translationX)
        }
        animSet.start()

        fun doTranslationVariationOnRepeat(){
            translationY.doOnRepeat {
                if (transX <= -100f) {
                    transX += 50f
                    translationX.setFloatValues(transX)
                } else {
                    transX -= 200f
                    translationX.setFloatValues(transX)
                }
                if (transX1 >= -700f) {
                    transX1 -= 50f
                    translationX.setFloatValues(transX1)
                } else {
                    transX1 += 200f
                    translationX.setFloatValues(transX1)
                }
            }
        }
        doTranslationVariationOnRepeat()
        translationY.addListener(object : Animator.AnimatorListener{
            override fun onAnimationEnd(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationStart(p0: Animator?) {}
            override fun onAnimationRepeat(p0: Animator?) {
                doTranslationVariationOnRepeat()
//                Log.d(TAG, "onStartAnimation: Values assigned again; transx = $transX, transX1 = $transX1")
            }
        })
    }

    fun ufoPauseAnimation(){
        val ufo = findViewById<ConstraintLayout>(R.id.ufo_layout)
        val ufoLeft = ObjectAnimator.ofFloat(ufo, View.TRANSLATION_X, ufo.translationX+3f).apply {
            duration = 750
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val ufoUp = ObjectAnimator.ofFloat(ufo, View.TRANSLATION_Y, ufo.translationY+3f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        ufoPauseAnimSet = AnimatorSet().apply {
            play(ufoLeft).with(ufoUp)
        }
        ufoPauseAnimSet.start()
    }

    fun ufoDisappearAnimation(){
        mediaPlayer = MediaPlayer.create(this, R.raw.ufodisappear)
        mediaPlayer.setVolume(100f, 100f)
        val ufo = findViewById<ConstraintLayout>(R.id.ufo_layout)
        val translateX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, ufo.translationX +100f)
        val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, ufo.translationY +100f)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)
        val scaleUfo = ObjectAnimator.ofPropertyValuesHolder(ufo, translateX, translateY, scaleX, scaleY).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            startDelay = 100
        }
        scaleUfo.start()
        mediaPlayer.start()
        scaleUfo.doOnEnd {
            ufo.visibility = View.GONE
            mediaPlayer = MediaPlayer.create(this, R.raw.ufo)
            mediaPlayer.setVolume(60f, 60f)
        }
    }

    fun ufoClickAction(view:View?){
//        Log.d(TAG, "UFO CLICKED!!!")
        animSet.pause()
        ufoPauseAnimation()
        mediaPlayer.start()
        tv_ufo.visibility = View.VISIBLE
        when (points){
            in 0..49999 ->{
                tv_ufo.postDelayed(Runnable {
                    tv_ufo.visibility = View.INVISIBLE
                    ufoPauseAnimSet.end()
                    animSet.resume()
                }, 4500)
            }
            in 50000..599999 ->{
                val settings = SettingsPrefs(this)
                val metUfoBefore = settings.getSetting(metUfo)
                val window = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                val show = layoutInflater.inflate(R.layout.ufo_popup_hello, null, false)
                val fadein = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                val buttPositive = show.findViewById<Button>(R.id.butt_ufo_positive)
                val buttNegative= show.findViewById<Button>(R.id.butt_ufo_negative)
                val tvUfo = show.findViewById<TextView>(R.id.tv_ufo_screen)
                if (metUfoBefore){ //check if the player gave its nick to ufo
                    tv_ufo.text = getString(R.string.ufo_met_before, uName)
                    adViewMain.visibility = View.INVISIBLE
                    window.contentView = show
                    window.showAtLocation(layoutbg, Gravity.BOTTOM, 0, 0)
                    show.startAnimation(fadein)
                    tvUfo.text = getString(R.string.ufo_met_before, uName)
                    buttPositive.visibility = View.GONE
                    buttNegative.visibility = View.GONE
                    if (points in 50000..149999){
                        tv_ufo.postDelayed({
                            tv_ufo.text = getString(R.string.ufo_met_before_low_points)
                            tvUfo.text = getString(R.string.ufo_met_before_low_points)
                            ufoPauseAnimSet.end()
                            animSet.resume()
                            tv_ufo.postDelayed({
                                tv_ufo.visibility = View.INVISIBLE
                                window.dismiss()
                                adViewMain.visibility = View.VISIBLE
                                ufoDisappearAnimation()
                            }, 6000)
                        }, 4500)
                    }else if (points in 150000..599999){//hidden stage entrance
                        tv_ufo.postDelayed({
                            tv_ufo.text = getString(R.string.ufo_met_before_high_points)
                            tvUfo.text = getString(R.string.ufo_met_before_high_points)
                            buttPositive.visibility = View.VISIBLE
                            buttNegative.visibility = View.VISIBLE
                            buttPositive.text = getString(R.string.ufo_high_point_positive)
                            buttNegative.text = getString(R.string.ufo_high_point_negative)
                            buttPositive.setOnClickListener {
//                                Log.d(TAG, "stage ufo positive")
                                window.dismiss()
                                adViewMain.visibility = View.VISIBLE
                                val levelKey = "Stage Ufo"
                                val intent = Intent(this, RandomActivity::class.java)
                                intent.putExtra("levelKey", levelKey)
                                intent.putExtra("tvName", uName)
                                startActivity(intent)
                            }
                            buttNegative.setOnClickListener {
//                                Log.d(TAG, "stage ufo negative")
                                tv_ufo.text = getString(R.string.ufo_hello_negative_answer)
                                tvUfo.text = getString(R.string.ufo_hello_negative_answer)
                                buttPositive.visibility = View.GONE
                                buttNegative.visibility = View.GONE
                                ufoPauseAnimSet.end()
                                animSet.resume()
                                tv_ufo.postDelayed(Runnable {
                                    tv_ufo.visibility = View.INVISIBLE
                                    ufoDisappearAnimation()
                                    window.dismiss()
                                    adViewMain.visibility = View.VISIBLE
                                }, 3000)
                            }
                        },4500)
                    }

                }else{
                    tv_ufo.text = getString(R.string.ufo_hello)
                    adViewMain.visibility = View.INVISIBLE
                    window.contentView = show
                    window.showAtLocation(layoutbg, Gravity.BOTTOM, 0, 0)
                    show.startAnimation(fadein)
                    buttPositive.text = "\"$uName\""
                    buttNegative.setOnClickListener {
                        tv_ufo.text = getString(R.string.ufo_hello_negative_answer)
                        tvUfo.text = getString(R.string.ufo_hello_negative_answer)
                        buttPositive.visibility = View.GONE
                        buttNegative.visibility = View.GONE
                        ufoPauseAnimSet.end()
                        animSet.resume()
                        tv_ufo.postDelayed(Runnable {
                            tv_ufo.visibility = View.INVISIBLE
                            ufoDisappearAnimation()
                            window.dismiss()
                            adViewMain.visibility = View.VISIBLE
                        }, 3000)
                    }
                    buttPositive.setOnClickListener {
                        tv_ufo.text = getString(R.string.ufo_hello_positive_answer, uName)
                        tvUfo.text = getString(R.string.ufo_hello_positive_answer, uName)
                        buttNegative.visibility = View.GONE
                        buttPositive.visibility = View.GONE
                        settings.setSetting(metUfo, true) //met ufo before setting updated
                        tv_ufo.postDelayed(Runnable {
                            tv_ufo.visibility = View.INVISIBLE
                            ufoPauseAnimSet.end()
//                            animSet.resume()
                            window.dismiss()
                            adViewMain.visibility = View.VISIBLE
                            ufoDisappearAnimation()
                        }, 4500)
                    }
                }
            }
        }
    }

    fun levelTagClarification(){
        val userReference = databaseReference.child(uid)
        when (points) {
            in 1..19999 -> {userReference.child("level").setValue("Epimetheus")}
            in 20000..39999 -> {userReference.child("level").setValue("Atlas")}
            in 40000..59999 -> {userReference.child("level").setValue("Hyperion")}
            in 60000..84999 -> {userReference.child("level").setValue("Charon")}
            in 85000..109999 -> {userReference.child("level").setValue("Mimas")}
            in 110000..139999 -> {userReference.child("level").setValue("Triton")}
            in 140000..189999 -> {userReference.child("level").setValue("Callisto")}
            in 170000..204999 -> {userReference.child("level").setValue("Ganymede")}
            in 205000..239999 -> {userReference.child("level").setValue("Europa")}
            in 240000..279999 -> {userReference.child("level").setValue("Titan")}
            in 280000..324999 -> {userReference.child("level").setValue("Moon")}
            in 325000..374999 -> {userReference.child("level").setValue("Enceladus")}
            in 375000..429999 -> {userReference.child("level").setValue("Pluto")}
            in 430000..599999 -> {userReference.child("level").setValue("Mars")}
            else -> {}
        }
    }

    private fun animationTop(){
        firstRunWindow = PopupWindow(this)
        val show = layoutInflater.inflate(R.layout.layout_popup_internet, null)
        firstRunWindow.isOutsideTouchable = true
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val atf = AnimationUtils.loadAnimation(this, R.anim.atf)
        val rtl = AnimationUtils.loadAnimation(this, R.anim.rtl)
        iv_avatar_circle.visibility = View.VISIBLE
        iv_avatar_circle.startAnimation(atf)

        atf.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                if (firstRunControl){
                    firstRunWindow.contentView = show
                    firstRunWindow.showAtLocation(layoutbg,1,0,0)
                    show.startAnimation(fadein)
                    firstRunControl = false
                }else{
//                    Log.d(TAG, "animationTop: Not first run!")
                }
                tvName.visibility = View.VISIBLE
                tvName.startAnimation(rtl)
                onStartAnimation()
                synchronRoomDb()
                mainButtonsStatusUpdate(true)
            }
        })
    }

    private fun animation(){
        val stf = AnimationUtils.loadAnimation(this, R.anim.stf)
        val atf = AnimationUtils.loadAnimation(this, R.anim.atf)
        ivPlay.visibility = View.VISIBLE
        buttOyna.visibility = View.VISIBLE
        ivProfile.visibility = View.VISIBLE
        buttProfil.visibility = View.VISIBLE
        ivSuggestQ.visibility = View.VISIBLE
        buttSoru.visibility = View.VISIBLE
        ivInfo.visibility = View.VISIBLE
        buttInfo.visibility = View.VISIBLE
        ivPlay.startAnimation(stf)
        ivProfile.startAnimation(stf)
        ivSuggestQ.startAnimation(stf)
        ivInfo.startAnimation(stf)
        buttOyna.startAnimation(atf)
        buttProfil.startAnimation(atf)
        buttSoru.startAnimation(atf)
        buttInfo.startAnimation(atf)
    }

    private fun mainButtonsStatusUpdate(canClick:Boolean){
        if(canClick){
            buttOyna.isEnabled = true
            buttOyna.isClickable = true
            buttProfil.isEnabled = true
            buttProfil.isClickable = true
            buttSoru.isEnabled = true
            buttSoru.isClickable = true
            buttInfo.isEnabled = true
            buttInfo.isClickable = true
        }else{
            buttOyna.isEnabled = false
            buttOyna.isClickable = false
            buttProfil.isEnabled = false
            buttProfil.isClickable = false
            buttSoru.isEnabled = false
            buttSoru.isClickable = false
            buttInfo.isEnabled = false
            buttInfo.isClickable = false
        }
    }

    private fun synchronRoomDb(){
//        Log.d(TAG, "synchronRoomDb: Starts")
        val stagesReference= databaseReference.child("$uid/stages")
        stagesReference.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
//                Log.d(TAG, "synchronRoomDb: stageRef couldn't read...")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.hasChildren()){
                    p0.children.forEach {
//                        Log.d(TAG, "synchronRoomDb: child = $it")
                        if (!it.child("/control").exists() && it.child("/point").exists()){
                            it.ref.child("/control").setValue(true).addOnSuccessListener { kopek ->
                                synchronDBsIfElse(stagesReference, it)
                            }
                        }else{
                            synchronDBsIfElse(stagesReference, it)
                        }
                    }
                }else{
                    Thread{
                        db.stageDao().getAll().forEach {
                            val roomName = it.db_stage_name
                            val roomPoint = it.db_stage_points
                            val roomControl = it.db_stage_control
                            stagesReference.child("/$roomName/point").setValue(roomPoint)
                            stagesReference.child("/$roomName/control").setValue(roomControl)
//                            Log.d(TAG, "synchronRoomDb: p0 doesn't have children: roomDB added to fireDB")
                        }
                    }.start()
                }
            }
        })
    }

    private fun synchronDBsIfElse(stagesReference: DatabaseReference, it:DataSnapshot){
        Thread{
            val dbStage = db.stageDao().getOne(it.key.toString())
            val lastInd = it.key.toString().length
            val id =
                if (it.key.toString() == "Stage Ufo"){
                    1000
                }else{
                    it.key.toString().substring(6, lastInd).toInt()
                }
            val fireName = it.key.toString()
            val sPoint = it.child("/point").value
            val firePoint = sPoint.toString().toInt()
            val fireControl = it.child("/control").value as Boolean
            val stageEnt = AppRoomEntity(id, fireName, firePoint, fireControl)
            if (dbStage != null){
                val roomName = dbStage.db_stage_name
                val roomPoint = dbStage.db_stage_points
                val roomControl = dbStage.db_stage_control
                if (roomName == fireName){
                    if (fireControl && roomControl && firePoint>roomPoint){
                        stagesReference.child("/$fireName/point").setValue(roomPoint)
//                        Log.d(TAG, "synchronDBsIfElse: $fireName: Points updated in fireDB")
                    }
                    else if (fireControl && roomControl && firePoint<roomPoint){
                        db.stageDao().update(stageEnt)
//                        Log.d(TAG, "synchronDBsIfElse: $fireName: Points updated in RoomDB")
                    }
                    else if (fireControl && !roomControl){
                        stagesReference.child("/$fireName/point").setValue(roomPoint)
                        stagesReference.child("/$fireName/control").setValue(roomControl)
//                        Log.d(TAG, "synchronDBsIfElse: $fireName: Points + Control updated in fireDB")
                    }
                    else if (!fireControl && roomControl){
                        db.stageDao().update(stageEnt)
//                        Log.d(TAG, "synchronDBsIfElse: $fireName: Points + Control updated in RoomDB")
                    }
                    else if (!fireControl && !roomControl && roomPoint != firePoint){
                        db.stageDao().update(stageEnt)
//                        Log.d(TAG, "synchronDBsIfElse: !!$fireName: Points + Control updated in RoomDB")
                    }
                    else{
//                        Log.d(TAG, "synchronDBsIfElse: No need to update $fireName! $fireControl")
                    }
                }
                else{
//                    Log.d(TAG, "synchronDBsIfElse: $fireName couldn't found in RoomDB = $roomName")
                }
            }else{
                db.stageDao().insert(stageEnt)
//                Log.d(TAG, "synchronDBsIfElse: $fireName: $fireControl inserted to RoomDB")
            }
        }.start()
    }
    //Activity Transitions Start
    fun showLvl(view: View?) {
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "showLvl: firstRunWindow.dismiss() ex: $ex")
        }
        if(uidAssignedCheck){
            if (animSet.isStarted){ animSet.pause() }
            val intent = Intent(this@MainActivity, LvlActivity::class.java)
            val gfo = AnimationUtils.loadAnimation(this, R.anim.gfo)
            val fo = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            ivPlay.startAnimation(gfo)
            buttOyna.startAnimation(fo)
            fo.setAnimationListener(object : Animation.AnimationListener{
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {buttOyna.visibility = View.INVISIBLE}
            })
            gfo.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(arg0: Animation) {
                    intent.putExtra("tvName", tvName.text.toString())
                }
                override fun onAnimationRepeat(arg0: Animation) {}
                override fun onAnimationEnd(arg0: Animation) {
                    ivPlay.visibility = View.INVISIBLE
                    startActivity(intent)
                    /*if (mInterstitialAd.isLoaded) {
//                        Log.d(TAG, "Ad Must be showed!!!")
                        mInterstitialAd.show()
                        mInterstitialAd.adListener = object : AdListener() {
                            override fun onAdClosed() {
                                mInterstitialAd.loadAd(AdRequest.Builder().build())
                                startActivity(intent)
                            }
                        }
                    } else {
//                        Log.d(TAG, "The interstitial wasn't loaded yet.")
                        startActivity(intent)
                    }*/
                }
            })
        }else{
            Snackbar.make(layoutbg, "Slow down, still getting the info! Try again after 3 seconds...", Snackbar.LENGTH_LONG).show()
        }
    }

    fun showProfile(view: View?){
//        Log.d(TAG, "Profile pressed")
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "showProfile: firstRunWindow.dismiss() ex: $ex")
        }
        if(uidAssignedCheck){
            if (animSet.isStarted){ animSet.pause() }
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            val gfo2 = AnimationUtils.loadAnimation(this, R.anim.gfo2)
            val fo = AnimationUtils.loadAnimation(this, R.anim.fade_out)
//        val profil = AnimationUtils.loadAnimation(this, R.anim.profil)
            ivProfile.startAnimation(gfo2)
            buttProfil.startAnimation(fo)
            fo.setAnimationListener(object : Animation.AnimationListener{
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {buttProfil.visibility = View.INVISIBLE}
            })
            gfo2.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(arg0: Animation) {
                    intent.putExtra("tvName", tvName.text.toString())
                }
                override fun onAnimationRepeat(arg0: Animation) {}
                override fun onAnimationEnd(arg0: Animation) {
                    startActivity(intent)
                    ivProfile.visibility = View.INVISIBLE
                    animation()
                }
            })
        }else{
            Snackbar.make(layoutbg, "Slow down, still getting the info! Try again after 3 seconds...", Snackbar.LENGTH_LONG).show()
        }
    }

    fun showSuggestQ(view: View?){
//        Log.d(TAG, "Suggest-Q pressed")
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "showSuggestQ: firstRunWindow.dismiss() ex: $ex")
        }
        if(uidAssignedCheck){
            val intent = Intent(this@MainActivity,SuggestActivity::class.java)
            val gfo = AnimationUtils.loadAnimation(this, R.anim.gfo)
            val fo = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            ivSuggestQ.startAnimation(gfo)
            buttSoru.startAnimation(fo)
            fo.setAnimationListener(object : Animation.AnimationListener{
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {buttSoru.visibility = View.INVISIBLE}
            })
            gfo.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(arg0: Animation) {}
                override fun onAnimationRepeat(arg0: Animation) {}
                override fun onAnimationEnd(arg0: Animation) {
                    startActivity(intent)
                    ivSuggestQ.visibility = View.INVISIBLE
                    animation()
                }
            })
        }else{
            Snackbar.make(layoutbg, "Slow down, still getting the info! Try again after 3 seconds...", Snackbar.LENGTH_LONG).show()
        }

    }

    fun showInfo(view:View?){
//        Log.d(TAG, "Info pressed")
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "showSettings: firstRunWindow.dismiss() ex: $ex")
        }
        if(uidAssignedCheck){
            val intent = Intent(this@MainActivity,InfoActivity::class.java)
            val gfo = AnimationUtils.loadAnimation(this, R.anim.gfo2)
            val fo = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            ivInfo.startAnimation(gfo)
            buttInfo.startAnimation(fo)
            fo.setAnimationListener(object : Animation.AnimationListener{
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {buttInfo.visibility = View.INVISIBLE}
            })
            gfo.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(arg0: Animation) {}
                override fun onAnimationRepeat(arg0: Animation) {}
                override fun onAnimationEnd(arg0: Animation) {
                    startActivity(intent)
                    ivInfo.visibility = View.INVISIBLE
                    animation()
                }
            })
        }else{
            Snackbar.make(layoutbg, "Slow down, still getting the info! Try again after 3 seconds...", Snackbar.LENGTH_LONG).show()
        }

    }

    private fun showSettings(view:View?){
//        Log.d(TAG, "action_settings pressed!")
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "showSettings: firstRunWindow.dismiss() ex: $ex")
        }
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun mainMenu(view: View?){
//        Log.d(TAG, "mainMenu pressed..")
        val intent = Intent(this@MainActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun privacyPolicy(){
//        Log.d(TAG, "privacyPolicy pressed..")
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "privacyPolicy: firstRunWindow.dismiss() ex: $ex")
        }
        val intent = Intent(this, PrivacyActivity::class.java)
        startActivity(intent)
    }

    private fun termsConditions(){
//        Log.d(TAG, "privacyPolicy pressed..")
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "termsConditions: firstRunWindow.dismiss() ex: $ex")
        }
        val intent = Intent(this, TermsActivity::class.java)
        startActivity(intent)
    }
    //Activity Transitions End
    private fun signOut(){
        if (animSet.isStarted){ animSet.pause() }
        try { firstRunWindow.dismiss()
        }catch (ex:Exception){
//            Log.d(TAG, "signOut: firstRunWindow.dismiss() ex: $ex")
        }
        auth.signOut()
        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        this@MainActivity.finish()
//        view_timer.stop()
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
            if (googleSignInClient.revokeAccess().isSuccessful){
                LoginActivity().updateUI(auth.currentUser)
            }
        }
    }

    override fun onBackPressed() {
        mainMenu(null)
    }

    override fun onDestroy() {
        val intent = Intent(this, MusicService::class.java)
        stopService(intent)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when {
            item.itemId == R.id.action_out -> signOut()
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