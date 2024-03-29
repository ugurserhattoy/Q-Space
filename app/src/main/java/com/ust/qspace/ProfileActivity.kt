package com.ust.qspace

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.Toast.LENGTH_LONG
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import java.lang.NullPointerException

private lateinit var database: FirebaseDatabase
private lateinit var commsReference: DatabaseReference
private lateinit var userRef: DatabaseReference

class ProfileActivity : AppCompatActivity() {
    val TAG = "ProfileActivity"
    lateinit var mAdView : AdView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val stageList = listOf("Stage 1", "Stage 2", "Stage 3", "Stage 4",
            "Stage 5","Stage 6","Stage 7","Stage 8", "Stage 9", "Stage 10", "Stage 11", "Stage 12", "Stage 13",
            "Stage 14", "Stage 15", "Stage 16", "Stage 17", "Stage 18", "Stage 19", "Stage 20", "Stage 21", "Stage 22",
            "Stage 23","Stage 24", "Stage 25", "Stage 26", "Stage 27", "Stage 28", "Stage 29", "Stage 30")
        val statsList = mutableListOf<String>("","","","","","","","","","","","","","","","","","",
            "","","","","","","","","","","","")

        val constraintLayout = findViewById<ConstraintLayout>(R.id.layoutbg)
        val animationDrawable = constraintLayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()

        mAdView = findViewById(R.id.adViewProfile)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        database = FirebaseDatabase.getInstance()
        commsReference = database.reference.child("Posts")
        userRef = database.reference.child("Users/$uid")

        progressBarProfile.visibility = View.VISIBLE

        userRef.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, "Something's Wrong: User information get FAILED")
                val toast = Toast.makeText(baseContext, getString(R.string.listener_cancelled), LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
            @SuppressLint("SetTextI18n")//strings without "" suppress.
            override fun onDataChange(p0: DataSnapshot) {
                val userLevel = p0.child("level").value
                val userPoints = p0.child("points").value as Long
                uName = p0.child("nickName").value as String
                avatar = p0.child("avatar").value as String

                textName.text = uName
                textMail.text = email
                textLevel.text = userLevel.toString()
                textPoints.text = userPoints.toString()
                Picasso.get().load(avatar).into(ivAvatar_circle)

                if (userPoints>=40000){
                    etQuote.isFocusable = true
                    etQuote.isFocusableInTouchMode = true
                    if(p0.child("userQuote").exists()){
                        val uQuote = p0.child("userQuote").value as String
                        etQuote.setText(uQuote)
                    }
                }else{
                    etQuote.isFocusable = false
                    etQuote.isFocusableInTouchMode = false
                }

                Log.d(TAG, "user informations parsed")
                if(p0.child("stages").exists()){
                    val stages = p0.child("stages").value as HashMap<*, *>
                    val stage = stages.keys
                    animations()
                    Log.d(TAG, "STAGES; $stages STAGE= $stage")
                    for (item in stageList){
                        val index = stageList.indexOf(item)
                        if (stage.contains(item)){
                            val control = p0.child("stages/$item/control").value as Boolean
                            val point = p0.child("stages/$item/point").value as Long
                            if (control){
                                statsList[index] = "unfinished"
                                Log.d(TAG, "$item: unfinished")
                            }else{
                                statsList[index] = "$point points"
                                Log.d(TAG, "$item: Finished")
                            }
                            Log.d(TAG, "stageList: $item applied")
                        }else{
                            statsList[index] = "unseen"
                            Log.d(TAG, "$item unseen")
                        }
                    }
                    val stageAdapter = StageListAdapter(this@ProfileActivity, R.layout.layout_list_stages, stageList, statsList)
                    lvStages.adapter = stageAdapter
                }else{
                    animations()
                    Log.d(TAG, "stages doesn't exist yet!")
                }
                if(p0.child("upCount").exists()){
                    val upCount = p0.child("upCount").value as Long
                    when (upCount){
                        in 1..9 ->{
                            tvEarnedUpvote.text =getString(R.string.tv_earned_upvote) + upCount.toString() +
                                    getString(R.string.tv_earned_upvote1)
                        }
                        in 10..49 ->{
                            tvEarnedUpvote.text =getString(R.string.tv_earned_upvote) + upCount.toString() +
                                    getString(R.string.tv_earned_upvote2)
                        }
                        in 50..99 ->{
                            tvEarnedUpvote.text = getString(R.string.tv_earned_upvote) + upCount.toString() +
                                    getString(R.string.tv_earned_upvote3)
                        }
                        in 100..10000->{
                            tvEarnedUpvote.text = getString(R.string.tv_earned_upvote) + upCount.toString() +
                                    getString(R.string.tv_earned_upvote4)
                        }
                        else->{
                            tvEarnedUpvote.text = getString(R.string.tv_earned_upvote5)
                        }
                    }
                }else{
                    tvEarnedUpvote.text = getString(R.string.tv_earned_upvote5)
                }
            }
        })

        buttAvatar.setOnClickListener {
            Log.d(TAG, "avatar selector clicked")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
        val buttShare = findViewById<Button>(R.id.buttShare)
        buttShare.setOnClickListener {
            share()
        }
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        buttStats.setOnClickListener {
            showStats()
            stats_card.startAnimation(fadein)
        }
        buttProfile.setOnClickListener {
            profile_card.visibility = View.VISIBLE
            stats_card.visibility = View.INVISIBLE
            buttStats.isSelected = false
            buttProfile.isSelected = true
            profile_card.startAnimation(fadein)
        }
        val slidein = AnimationUtils.loadAnimation(this, R.anim.abc_slide_in_top)
        selectorButts.startAnimation(slidein)

        etQuote.onFocusChangeListener = object : View.OnFocusChangeListener{
            override fun onFocusChange(p0: View?, p1: Boolean) {
                if(p1){
                    buttSave.visibility = View.VISIBLE
                    buttSave.startAnimation(fadein)
                }else{
                    etQuote.isActivated = false
                    etQuote.isSelected = false
                    buttSave.visibility = View.GONE
                    //Hide the Fucking Keyboard!!
                    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    if (p0 != null) {
                        inputMethodManager.hideSoftInputFromWindow(p0.windowToken, 0)
                    }
                }
            }
        }
        buttSave.setOnClickListener {
            val uQuote = etQuote.text.toString()
            userRef.child("userQuote").setValue(uQuote)
            etQuote.isActivated = false
            etQuote.isSelected = false
            layoutbg.requestFocus()
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            //proceed and check what the selected image was...
            Log.d(TAG,"avatar was selected")

            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

            ivAvatar_circle.setImageBitmap(bitmap)

            buttAvatar.alpha = 0f

            /*val bitmapDrawable = BitmapDrawable(bitmap)
            buttAvatar.setBackgroundDrawable(bitmapDrawable)*/

            uploadImageToStorage()
        }
    }

    private fun uploadImageToStorage(){
        if (selectedPhotoUri == null) return
        var filename = uid
        val ref = FirebaseStorage.getInstance().getReference("/Images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d(TAG, "Avatar uploaded: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "Avatar location: $it")

                    saveImageToDatabase(it.toString())
                    avatar = it.toString()
                }
            }
            .addOnFailureListener{
                Log.d(TAG, "Avatar couldn't uploaded!")
            }
    }

    private fun saveImageToDatabase(avatarUrl: String){
        val ref = FirebaseDatabase.getInstance().getReference("/Users/$uid/avatar")

        ref.setValue(avatarUrl)
            .addOnSuccessListener {
                Log.d(TAG, "Avatar added to user database.")
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to set value to database!")
            }
    }

    private fun animations(){
        progressBarProfile.visibility = View.GONE
        val fadein = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        profile_card.visibility = View.VISIBLE
        profile_card.startAnimation(fadein)
    }

    fun showStats(){
        profile_card.visibility = View.INVISIBLE
        stats_card.visibility = View.VISIBLE
        buttStats.isSelected = true
        buttProfile.isSelected = false
    }

    fun share(){
        try {
            var imageUri = Uri.parse(
                MediaStore.Images.Media.insertImage(this.contentResolver,
                    BitmapFactory.decodeResource(resources, R.drawable.marsbutton100), null, null
                )
            )
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "image/*"
            sharingIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Konulu")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, "Bir varmış bir yokmuş")
            sharingIntent.putExtra(Intent.EXTRA_TITLE,  "Q Space")
            startActivity(Intent.createChooser(sharingIntent, "Share with..."))
        }catch (e:NullPointerException){}

    }

    override fun onBackPressed() {
        Log.d(TAG, "Back Pressed")
        val intent = Intent(this@ProfileActivity, MainActivity::class.java)
        this.finish()
        startActivity(intent)
    }
    //Custom stageList Adapter
    private class StageListAdapter(context: Context,val mLayout:Int , var list1:List<String>,
                                   var list2:MutableList<String>): BaseAdapter() {
        private val mContext: Context

        init {
            mContext = context
        }
        override fun getView(p0: Int, convertView: View?, viewGroup: ViewGroup?): View {
            //Rendering out each row
            val layoutInflater = LayoutInflater.from(mContext)
            val rowStage = layoutInflater.inflate(mLayout, viewGroup, false)

            val stagesText = rowStage.findViewById<TextView>(R.id.text1)
            stagesText.text = list1.get(p0)

            val stageStatText = rowStage.findViewById<TextView>(R.id.text2)
            stageStatText.text = list2.get(p0)
            if (list2[p0] != "unfinished" && list2[p0] != "unseen"){
                stageStatText.setTextColor(Color.parseColor("#669900"))
            }else{}

            return rowStage
        }
        override fun getItem(p0: Int): Any {return "SomeString"}
        override fun getItemId(p0: Int): Long {return p0.toLong()}
        override fun getCount(): Int {
            //for how many rows in the list
            return list1.size
        }
    }
}
