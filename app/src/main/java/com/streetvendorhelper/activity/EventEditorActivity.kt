package com.streetvendorhelper.activity

import android.app.DatePickerDialog
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.streetvendorhelper.model.Event
import com.streetvendorhelper.model.GPSLocation
import com.streetvendorhelper.model.Image
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.kaiteki.toast
import com.squareup.picasso.Picasso
import com.streetvendorhelper.R
import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.synthetic.main.event_image_item.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.selector
import org.jetbrains.anko.uiThread
import java.text.SimpleDateFormat
import java.util.*

class EventEditorActivity : AppCompatActivity(), BottomSheetImagePicker.OnImagesSelectedListener {


    var currentEvent: Event? = null
    private var eventId: String? = null
    private var images = arrayListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        setSupportActionBar(findViewById(R.id.my_editor_toolbar))
        val title = intent.getStringExtra("title")
        eventId = intent.getStringExtra("id")

        your_name.isFocusable = false
        your_name.isClickable = true

        if (title.isNullOrEmpty()){
            setTitle("New Event")
            your_name.setText(FirebaseAuth.getInstance().currentUser?.displayName)
        } else {
            setTitle(title)
            name.isEnabled = false
            eventId = intent.getStringExtra("id")
            val eRef = FirebaseDatabase.getInstance().reference.child("Event/$eventId")
            val postListener = object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    val value = p0.getValue(Event::class.java)
                    if (value != null){
                        value.id= p0.key
                        currentEvent = value

                        name.setText(value.eventName)
                        type.setText(value.eventCategory)
                        from_date.setText(value.eventDate)
                        to_date.setText(value.eventToDate)
                        contact.setText(value.eventContact)
                        phone.setText(value.eventPhone)
                        email.setText(value.eventEmail)
                        website.setText(value.eventWebSite)
                        street.setText(value.eventStreet)
                        city.setText(value.eventCity)
                        state.setText(value.eventState)
                        country.setText(value.eventCountry)
                        your_name.setText(value.eventCreator)
                        frequency.setText(value.frequency)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
//                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                    // ...
                }
            }
            eRef.addValueEventListener(postListener)
//
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

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)



        state.isFocusable = false
        state.isClickable = true
        state.setOnClickListener {
            val statesList = resources.getStringArray(R.array.states).toList()
            selector("Select sate?", statesList) { dialogInterface, i ->
                val stateName = statesList[i]
                if (stateName == "Other"){
                    country.isEnabled = true
                } else {
                    country.isEnabled = false
                    val defaultCountry = "United States"
                    country.setText(defaultCountry)
                }
                state.setText(stateName)
            }
        }

        type.isFocusable = false
        type.isClickable = true
        type.setOnClickListener {
            val typeList = resources.getStringArray(R.array.types).toList()
            selector("Select category?", typeList) { dialogInterface, i ->
                type.setText(typeList[i])
            }
        }


        from_date.isFocusable = false
        from_date.isClickable = true
        from_date.setOnClickListener {
            val dpd = DatePickerDialog(this@EventEditorActivity, DatePickerDialog.OnDateSetListener { view2, thisYear, thisMonth, thisDay ->
                val newDate: Calendar =Calendar.getInstance()
                newDate.set(thisYear, thisMonth + 1, thisDay)
                from_date.setText(sdf.format(newDate.time))
            }, year, month, day)
            dpd.show()
        }

        to_date.isFocusable = false
        to_date.isClickable = true
        to_date.setOnClickListener {
            val dpd = DatePickerDialog(this@EventEditorActivity, DatePickerDialog.OnDateSetListener { view2, thisYear, thisMonth, thisDay ->
                val newDate: Calendar =Calendar.getInstance()
                newDate.set(thisYear, thisMonth + 1, thisDay)
                to_date.setText(sdf.format(newDate.time))
            }, year, month, day)
            dpd.show()
        }
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

override fun onImagesSelected(uris: List<Uri>, tag: String?) {
    images = arrayListOf()
    images.addAll(uris)
    imagesList.adapter = DefaultImageAdapter(images)
    (imagesList.adapter as DefaultImageAdapter).notifyDataSetChanged()
}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save){
           if (validate()){
//               val dialog = indeterminateProgressDialog(message = "Please wait a bit…", title = "Saving data")
               val data = Event(
                       eventName = name.text.toString(),
                       eventCategory = type.text.toString(),
                       eventDate = from_date.text.toString(),
                       eventToDate = to_date.text.toString(),
                       eventContact = contact.text.toString(),
                       eventEmail = email.text.toString(),
                       eventPhone = phone.text.toString(),
                       eventWebSite = website.text.toString(),
                       eventStreet = street.text.toString(),
                       eventCity = city.text.toString(),
                       eventState = state.text.toString(),
                       eventCountry = country.text.toString(),
                       eventCreator = your_name.text.toString(),
                       frequency = frequency.text.toString()
               )

               val oldReviews = currentEvent?.Reviews ?: hashMapOf()
               val images = currentEvent?.images ?: hashMapOf()
               val ref = FirebaseDatabase.getInstance().reference.child("Event")

               if (eventId == null){
                   eventId = name.text.toString() + " " + city.text.toString()
               }

               ref.child(eventId!!).setValue(data)
               ref.child(eventId!!).child("Reviews").setValue(oldReviews)
               ref.child(eventId!!).child("images").setValue(images)

               doAsync {
                   val lRef = FirebaseDatabase.getInstance().reference.child("Location")
                   val key = data.eventCity.trim() + ", " + data.eventState.trim() + ", " + data.eventCountry.trim()
                   val geocoder = Geocoder(this@EventEditorActivity, Locale.getDefault())
                   val addresses = geocoder.getFromLocationName(key, 1)
                   val address = addresses[0]
                   val location = GPSLocation(data.eventCity, data.eventState, address.longitude, address.latitude)
                   lRef.child(key).setValue(location)

                   val loc = GeoLocation(address.latitude, address.longitude)
                   val gRef = FirebaseDatabase.getInstance().reference.child("eventLocs")
                   val geoFire = GeoFire(gRef)

                   geoFire.setLocation(eventId, loc) { key, error ->
                       uiThread {
                           val imgRef= FirebaseStorage.getInstance().reference.child("images")
                           uploadImages(imgRef, 0) {
//                               dialog.dismiss()
                               finish()
                               toast("Saved")
                               EventBus.getDefault().post("reload")
                           }
                       }
                   }
               }
           }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun validate(): Boolean{
        name.error = null
        from_date.error = null
        city.error = null
        state.error = null
        type.error = null

        if (name.text.isNullOrEmpty()){
            name.error = "Name is required"
            return false
        }

        if (type.text.isNullOrEmpty()){
            type.error = "Category is required"
            return false
        }


        if (from_date.text.isNullOrEmpty()){
            from_date.error = "From date is required"
            return false
        }

        if (city.text.isNullOrEmpty()){
            city.error = "City is required"
            return false
        }

        if (state.text.isNullOrEmpty()){
            state.error = "State is required"
            return false
        }


        return true
    }

    private fun uploadImages(ref: StorageReference, pos: Int, callback: () -> Unit){
        if (pos == images.size) {
            callback()
            return
        }

        val iRef = FirebaseDatabase.getInstance().reference.child("Event/$eventId/images")
        val key = iRef.push().key

        val stream = contentResolver.openInputStream(images[pos])
        val uploadTask = ref.child(key!!).putStream(stream!!)

        uploadTask.addOnFailureListener {
        }.addOnCompleteListener {
            ref.child(key).downloadUrl.addOnSuccessListener { uri ->
                val a = Image(imageDownloadURL = uri.toString())
                iRef.child(key).setValue(ref.child(key))
            }
            uploadImages(ref, pos+1, callback)
        }
    }

    class DefaultImageAdapter(private val allImages: ArrayList<Uri>) : androidx.recyclerview.widget.RecyclerView.Adapter<EventImageHolder>() {

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

    class EventImageHolder(itemView: android.view.View?) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView!!) {
        fun bind(path: Uri){
            Picasso.get().load(path).into(itemView.image)
        }
    }

}