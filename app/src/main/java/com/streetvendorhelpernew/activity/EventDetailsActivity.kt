package com.streetvendorhelpernew.activity

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_details.*
import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.database.FirebaseRecyclerOptions
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.streetvendorhelpernew.model.Image
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.event_image_item.view.*
import kotlinx.android.synthetic.main.review_item.view.*
import org.jetbrains.anko.*
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.kaiteki.toast
import com.streetvendorhelpernew.R
import com.streetvendorhelpernew.model.Event
import com.streetvendorhelpernew.model.Review
import com.streetvendorhelpernew.util.VendorUtils


class EventDetailsActivity : AppCompatActivity(), BottomSheetImagePicker.OnImagesSelectedListener   {

    var currentEvent: Event? = null
    private var adapter: FirebaseRecyclerAdapter<Review, ReviewHolder>? = null
    private var iAdapter: FirebaseRecyclerAdapter<Image, EventImageHolder>? = null
    private var uploadImages = arrayListOf<Uri>()
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(findViewById(R.id.my_details_toolbar))

        title = intent.getStringExtra("title")
        fetchData()

        button_feedback.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null){
                startActivity(intentFor<FeedbackActivity>("id" to intent.getStringExtra("id")))
            } else {
                alert(getString(R.string.login_required_feedback), getString(R.string.login_required)).show()
            }

        }

        button_picture.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null){
                pickMulti()
            } else {
                alert(getString(R.string.login_required_photo), getString(R.string.login_required)).show()
            }
        }

        val layoutManeger = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManeger

        val layoutManagerH = androidx.recyclerview.widget.LinearLayoutManager(this, LinearLayout.HORIZONTAL, false)
        images.layoutManager = layoutManagerH

        val snapHelper = androidx.recyclerview.widget.LinearSnapHelper()
        snapHelper.attachToRecyclerView(images)
        recyclerViewIndicator.setRecyclerView(images)
    }

    private fun pickMulti() {
        BottomSheetImagePicker.Builder("com.streetvendorhelper.fileprovider")
                .columnSize(R.dimen.imagePickerColumnSize)
                .multiSelect(1, 3)
                .multiSelectTitles(
                        R.plurals.imagePickerMulti,
                        R.plurals.imagePickerMultiMore,
                        R.string.imagePickerMultiLimit
                )
                .requestTag("multi")
                .show(supportFragmentManager, tag = "picker")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.details_menu, menu)
        return true
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        toast("Result from tag: $tag")
        uploadImages = arrayListOf()
        uploadImages.addAll(uris)
        performUpload()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item?.itemId == R.id.action_edit) {
            if (FirebaseAuth.getInstance().currentUser != null){
               startActivity(intentFor<EventEditorActivity>("id" to intent.getStringExtra("id"), "title" to intent.getStringExtra("title")))
            } else {
                alert(getString(R.string.login_required_edit), getString(R.string.login_required)).show()
            }
        }
        return true
    }

    private fun fetchData(){
        val key = intent.getStringExtra("id")
        val eRef = FirebaseDatabase.getInstance().reference.child("Event/$key")
        val postListener = object : ValueEventListener
         {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val value = p0.getValue(Event::class.java)
                if (value != null){
                    value.id= p0.key
                    currentEvent = value
                    renderData()
                }
            }

        }
        eRef.addValueEventListener(postListener)
    }

    fun renderData(){
        if (currentEvent!!.images.isNotEmpty()){

        } else {

        }
        val reviewsText = "${currentEvent!!.Reviews.count()}  reviews"
        val timeText = "When: " + currentEvent!!.eventFromDate
        val locationText = "Where: " + currentEvent!!.eventStreet
        val contactText = "Contact: " + currentEvent!!.eventContact
        val phoneText = "Phone: " + currentEvent!!.eventPhone
        val emailText = "Email: " + currentEvent!!.eventEmail
        val ratingText = VendorUtils.calculateRating(currentEvent!!)
        reviews.text = reviewsText
        name.text = currentEvent!!.eventName
        time.text = timeText
        location.text = locationText
        contact.text = contactText
        phone.text = phoneText
        email.text = emailText
        rating.rating = ratingText

        val key = intent.getStringExtra("id")

        val ref = FirebaseDatabase.getInstance().reference.child("Event/$key/Reviews")

        val options = FirebaseRecyclerOptions.Builder<Review>()
                .setQuery(ref, Review::class.java)
                .build()

        adapter = object : FirebaseRecyclerAdapter<Review, ReviewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewHolder {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.review_item, parent, false)

                return ReviewHolder(view)
            }

            override fun onBindViewHolder(holder: ReviewHolder, position: Int, model: Review) {
                holder.bind(model)
            }
        }

        recyclerView.adapter = adapter
        adapter?.startListening()

        if (currentEvent!!.images.isNotEmpty()){
            val iRef = FirebaseDatabase.getInstance().reference.child("Event/$key/images")

            val iOptions = FirebaseRecyclerOptions.Builder<Image>()
                    .setQuery(iRef, Image::class.java)
                    .build()

            iAdapter = object : FirebaseRecyclerAdapter<Image, EventImageHolder>(iOptions) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventImageHolder {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.event_image_item, parent, false)

                    return EventImageHolder(view)
                }

                override fun onBindViewHolder(holder: EventImageHolder, position: Int, model: Image) {
                    holder.bind(getRef(position).key)
                }
            }

            images.adapter = iAdapter
            iAdapter?.startListening()
        } else {
            val dAdapter = DefaultImageAdapter(currentEvent!!.eventCategory)
            images.adapter = dAdapter
            dAdapter.notifyDataSetChanged()
        }

    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
        iAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
        iAdapter?.stopListening()
    }

    class ReviewHolder(private val mView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        fun bind(item: Review){
            mView.name.text = item.Reviewer
            mView.description.text = item.Comment
            mView.rating.rating = item.Rating.toFloat()
        }
    }


    class DefaultImageAdapter(private val category: String) : androidx.recyclerview.widget.RecyclerView.Adapter<EventImageHolder>() {

        private var cat = "df"

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventImageHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.event_image_item, parent, false)

            return EventImageHolder(view)
        }

        override fun onBindViewHolder(holder: EventImageHolder, position: Int) {
            val context = holder.itemView.context
            val pos = position+1
            val resId = VendorUtils.getResId(context, cat+pos)
            if (cat == "df"){
                holder.bind(R.drawable.df)
            } else {
                holder.bind(resId)
            }

        }


        override fun getItemCount(): Int {
            cat = VendorUtils.getCategoryIdentifier(category)
            if (category == "df"){
                return 1
            }
            return 9
        }

    }

private fun performUpload(){
        alert("Do you want to upload?", "Confirm"){
            yesButton {
                val dialog = indeterminateProgressDialog(message = "Please wait a bit…", title = "Saving data")
                val imgRef= FirebaseStorage.getInstance().reference.child("images")
                uploadImages(imgRef, 0) {
                    dialog.dismiss()
                    finish()
                    toast("Saved")
                }
            }
            cancelButton {  }
        }.show()
    }

    private fun uploadImages(ref: StorageReference, pos: Int, callback: () -> Unit){
        if (pos == uploadImages.size) {
            callback()
            return
        }
        val eventId = intent.getStringExtra("id")
        val iRef = FirebaseDatabase.getInstance().reference.child("Event/$eventId/images")
        val key = iRef.push().key

        val stream = contentResolver.openInputStream(uploadImages[pos])
        val uploadTask = key?.let { stream?.let { it1 -> ref.child(it).putStream(it1) } }

        uploadTask?.addOnFailureListener {
        }?.addOnCompleteListener {
            key.let { it1 ->
                ref.child(it1).downloadUrl.addOnSuccessListener { uri ->
                    val a = Image(imageDownloadURL = uri.toString())
                    iRef.child(key).setValue(a)
                }
            }
            uploadImages(ref, pos+1, callback)
        }
    }

    class EventImageHolder(itemView: android.view.View?) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView!!) {
        fun bind(path: String?){
            val imageRef = FirebaseStorage.getInstance().reference.child("images/$path")
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Picasso.get().load(uri).resize(1024, 768).into(itemView.image)
            }
        }

        fun bind (res: Int){
            itemView.image.setImageResource(res)
        }
    }

}
