package com.streetvendorhelpernew.activity

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import com.streetvendorhelpernew.model.Image
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_feedback.*
import kotlinx.android.synthetic.main.event_image_item.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.*
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.kaiteki.toast
import com.streetvendorhelpernew.R
import kotlinx.android.synthetic.main.activity_feedback.button_picture
import kotlinx.android.synthetic.main.activity_feedback.name
import kotlinx.android.synthetic.main.activity_feedback.rating

class FeedbackActivity : AppCompatActivity(), BottomSheetImagePicker.OnImagesSelectedListener  {
    private var images = arrayListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        name.isFocusable = false
        name.isClickable = true
        name.setText(FirebaseAuth.getInstance().currentUser?.displayName)

        button_save.setOnClickListener {
            if (validation()){
                val key = intent.getStringExtra("id")
                val rKey = key + " " + FirebaseAuth.getInstance().currentUser?.displayName
                val eRef = FirebaseDatabase.getInstance().reference.child("Event/$key/Reviews/$rKey")
                eRef.child("Reviewer").setValue(name.text.toString())
                eRef.child("Comment").setValue(comment.text.toString())
                eRef.child("Rating").setValue(rating.rating.toLong())
                performUpload()
            }
        }


        button_picture.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null){
                pickMulti()
            } else {
                alert(getString(R.string.login_required_photo), getString(R.string.login_required)).show()
            }
        }

        val layoutManagerH = androidx.recyclerview.widget.LinearLayoutManager(this, LinearLayout.HORIZONTAL, false)
        imagesList.layoutManager = layoutManagerH

        val snapHelper = androidx.recyclerview.widget.LinearSnapHelper()
        snapHelper.attachToRecyclerView(imagesList)

    }

    private fun pickMulti() {
        BottomSheetImagePicker.Builder("com.bigchopaccessories.vendorhelper.fileprovider")
                .columnSize(R.dimen.imagePickerColumnSize)
                .multiSelect(3, 6)
                .multiSelectTitles(
                        R.plurals.imagePickerMulti,
                        R.plurals.imagePickerMultiMore,
                        R.string.imagePickerMultiLimit
                )
                .requestTag("multi")
                .show(supportFragmentManager, tag = "picker")
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        toast("Result from tag: $tag")
        images = arrayListOf()
        images.addAll(uris)
        imagesList.adapter = EventEditorActivity.DefaultImageAdapter(images)
        (imagesList.adapter as EventEditorActivity.DefaultImageAdapter).notifyDataSetChanged()
    }


    private fun validation(): Boolean {

        return true
    }

    class EventImageHolder(itemView: android.view.View?) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView!!) {
        fun bind(path: Uri){
            Picasso.get().load(path).resize(1024, 768).into(itemView.image)
        }
    }

    class DefaultImageAdapter(val allImages: ArrayList<Uri>) : androidx.recyclerview.widget.RecyclerView.Adapter<EventImageHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventImageHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.event_image_item, parent, false)

            return EventImageHolder(view)
        }

        override fun onBindViewHolder(holder: EventImageHolder, position: Int) {
            holder.bind(allImages[position])
        }


        override fun getItemCount(): Int {
            return allImages.count()
        }

    }


    private fun performUpload(){
        val dialog = indeterminateProgressDialog(message = "Please wait a bit…", title = "Saving data")
        val imgRef= FirebaseStorage.getInstance().reference.child("images")
        uploadImages(imgRef, 0) {
            toast("Feedback success")
            EventBus.getDefault().post("reload")
            finish()
        }
    }

    private fun uploadImages(ref: StorageReference, pos: Int, callback: () -> Unit){
        if (pos == images.size) {
            callback()
            return
        }
        val eventId = intent.getStringExtra("id")
        val iRef = FirebaseDatabase.getInstance().reference.child("Event/$eventId/images")
        val key = iRef.push().key

        val stream = contentResolver.openInputStream(images[pos])
        val uploadTask = ref.child(key!!).putStream(stream!!)

        uploadTask.addOnFailureListener {
        }.addOnCompleteListener {
            ref.child(key).downloadUrl.addOnSuccessListener { uri ->
                val a = Image(imageDownloadURL = uri.toString())
                iRef.child(key).setValue(a)
            }
            uploadImages(ref, pos+1, callback)
        }
    }


}