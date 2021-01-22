package com.streetvendorhelpernew

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.crashlytics.android.Crashlytics
import com.davidecirillo.multichoicerecyclerview.MultiChoiceAdapter
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryDataEventListener
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.streetvendorhelpernew.Constants.GPSTracker
import com.streetvendorhelpernew.activity.EventDetailsActivity
import com.streetvendorhelpernew.activity.EventEditorActivity
import com.streetvendorhelpernew.activity.LocationActivity
import com.streetvendorhelpernew.model.Event
import com.streetvendorhelpernew.model.GPSLocation
import com.streetvendorhelpernew.util.VendorUtils
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.event_item.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var allEvents: ArrayList<Event> = arrayListOf()
    private var filteredEvents: ArrayList<Event> = arrayListOf()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val REQUEST_CODE_PERMISSION = 2
    private val REQUEST_CODE_LOCATION = 3
    private val REQUEST_CODE_SIGN_IN = 4
    private val TAG = "LocationProvider"
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private var currentGeoLocation: GeoLocation? = null
    protected var lastLocation: Location? = null
    private var userGeoLocation: GeoLocation? = null
    var adapter: EventAdapter? = null
    var location_ar: Location? = null

    companion object {
        var deleteMenu: MenuItem? = null
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    private var locationManager : LocationManager? = null
    var gpsTracker: GPSTracker? = null
    private var isAccepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        Fabric.with(this, Crashlytics())
        setSupportActionBar(findViewById(R.id.my_toolbar))
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val layoutManeger = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManeger

        button_location.setOnClickListener {
            startActivityForResult(intentFor<LocationActivity>(), REQUEST_CODE_LOCATION)
        }

        img_ad.setOnClickListener{
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cjoyboutique.com"))
            startActivity(browserIntent)
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            button_auth.text = getString(R.string.sign_out)

        } else {

            button_auth.text = getString(R.string.sign_in)

        }
        button_auth.setOnClickListener {
            checkAuthStatus()
        }

        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                filteredEvents = arrayListOf()
                if (p0.isNullOrEmpty()){
                    filteredEvents = allEvents
                } else {
                    for (event in allEvents){
                        if (event.eventCategory.toLowerCase().contains(p0.toLowerCase()) || event.eventName.toLowerCase().contains(p0.toLowerCase()) || event.eventCity.toLowerCase().contains(p0.toLowerCase())){
                            filteredEvents.add(event)
                        }
                    }

                }
                adapter?.setData(filteredEvents)
                return true
            }

        })

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        EventBus.getDefault().register(this)
        // Create persistent LocationManager reference
        gpsTracker = GPSTracker(this@MainActivity)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?




//        if (!checkPermissions()) {
//            requestPermissions()
//        } else if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            getLastLocation()
//        } else {
//            nesePermission()
//        }

        requestPermission()
    }

//    override fun onResume() {
//        super.onResume()
//        if (isAccepted) {
//            if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                Handlers()
////                getLastLocation()
//            } else {
//                nesePermission()
//            }
//        }
//    }

    private fun Handlers() {
        Handler().postDelayed({
            if (gpsTracker!!.canGetLocation()) {
             gpsTracker!!.latitude
                gpsTracker!!.longitude

                getLastLocation()

            }

        }, 1500)
    }
    fun nesePermission() {
        val alertDialog =
            AlertDialog.Builder(this@MainActivity)
        alertDialog.setTitle("Allow Permission")
        alertDialog.setMessage("This permission is necessary to use Street Vendor Helper")
        alertDialog.setPositiveButton("Allow"
        ) { dialog, which ->
            val intent =
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            dialog.dismiss()
        }
        alertDialog.setNegativeButton("Close"
        ) { dialog, which -> dialog.dismiss() }
        alertDialog.show()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (location_ar != null) {
            //Getting longitude and latitude
            println("location_ar"+location_ar!!.getLongitude());

        } else {
            println("----------------geting Location from GPS----------------")
            val tracker = GPSTracker(this)
            location_ar = tracker.getLocation()
            println("location_ar tracker"+location_ar!!.getLongitude());
            if (location_ar == null) {

            } else {


            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
//        loadByLocation(36.648380, -107.632640)
        mFusedLocationClient!!.lastLocation
            .addOnCompleteListener(this) { task ->
                println("other message"+task.isSuccessful+task.result)
                if (task.isSuccessful && task.result != null) {
                    lastLocation = task.result

                    val currentGeoLocation = LatLng((lastLocation )!!.latitude,(lastLocation )!!.longitude)
//                userGeoLocation = currentGeoLocation
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(currentGeoLocation.latitude, currentGeoLocation.longitude, 1)
                    val address = addresses[0]
                    val buttonLocationText = address.locality + ", " + address.adminArea
                    button_location.text = buttonLocationText
                    println("currentGeoLocation.latitude="+currentGeoLocation.latitude +"currentGeoLocation.longitude="+currentGeoLocation.longitude)
                    loadByLocation(36.648380, -107.632640)
                    loadByLocation(currentGeoLocation.latitude, currentGeoLocation.longitude)

                } else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)

                    showMessage(getString(R.string.no_location_detected))
                }
            }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED


    }


    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)

    }
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this@MainActivity, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), REQUEST_CODE_LOCATION
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                View.OnClickListener {
                    // Request permission
                    startLocationPermissionRequest()
                })

        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val response = IdpResponse.fromResultIntent (data)
            if (resultCode == RESULT_OK) {
                button_auth.text = getString(R.string.sign_out)
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    println("phoneNumber"+user.phoneNumber)
                    println("email"+user.email)
                    println("displayName"+user.displayName)
                }
            } else {
                val err = response?.error
                err?.printStackTrace()
            }
        } else if (requestCode == REQUEST_CODE_LOCATION && resultCode == RESULT_OK) {
            val long = data?.getDoubleExtra("long",0.0)
            val lat = data?.getDoubleExtra("lat",0.0)
            val name = data?.getStringExtra("name")
            button_location.text = name
            println("long="+long+"lat="+lat+"name="+name)
            loadByLocation(lat!!, long!!)
                }

    }
    // Callback received when a permissions request has been completed.

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (a in grantResults) {
            isAccepted = a == 0
            if (!isAccepted) {
                break
            }
        }
        if (isAccepted) {
            if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Handlers()
            } else {
                nesePermission()
            }
        } else {
            requestPermissions()
        }
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation()
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                    View.OnClickListener {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null)
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
            }
        }
    }

/*
    private fun getLocation() {
//        val tracker = MyTracker(this)
        checkLocationPermission()
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location : Location? ->
            if (location != null) {
                lastLocation = location
                val currentGeoLocation = LatLng(location.latitude, location.longitude)
//                userGeoLocation = currentGeoLocation
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(currentGeoLocation.latitude, currentGeoLocation.longitude, 1)
                val address = addresses[0]
                val buttonLocationText = address.locality + ", " + address.adminArea
                button_location.text = buttonLocationText
                loadByLocation(currentGeoLocation.latitude, currentGeoLocation.longitude)
            }
        }

    }
*/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        deleteMenu = menu.findItem(R.id.action_delete)
        deleteMenu?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_new){
            if (FirebaseAuth.getInstance().currentUser != null){
                startActivity(intentFor<EventEditorActivity>())
            } else {
                alert(getString(R.string.login_required_new_event), getString(R.string.login_required)).show()
            }
        } else if (item.itemId == R.id.action_delete){
            if (FirebaseAuth.getInstance().currentUser != null){
                alert("Do you want to delete selected events?", "Warning"){
                    yesButton {
                        for (selectedPos in adapter?.selectedItemList!!) {
                            val eRef = FirebaseDatabase.getInstance().reference.child("Event")
                            val selectedItem = adapter?.getItem(selectedPos)
                            if (selectedItem != null){
                                selectedItem.id?.let { it1 -> eRef.child(it1).removeValue() }
                            }
                        }
                        toast("Deleted")
                        loadByLocation((lastLocation)!!.latitude, (lastLocation)!!.longitude)
                    }
                    cancelButton {  }
                }.show()
            } else {
                alert(getString(R.string.login_required_delete_event), getString(R.string.login_required)).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadByLocation(lat: Double, long: Double){
        println("loadByLocation")
        val ref = FirebaseDatabase.getInstance().reference.child("eventLocs")
        val geoFire = GeoFire(ref)
        val currentGeoLocation = GeoLocation(lat, long)
        allEvents = arrayListOf()
        search.setQuery("", false)
        adapter = EventAdapter()
        geoFire.queryAtLocation(currentGeoLocation, 1000.0).addGeoQueryDataEventListener(object: GeoQueryDataEventListener {
            override fun onGeoQueryReady() {

            }

            override fun onDataExited(dataSnapshot: DataSnapshot?) {

            }

            override fun onDataChanged(dataSnapshot: DataSnapshot?, location: GeoLocation?) {

            }

            override fun onDataEntered(dataSnapshot: DataSnapshot?, location: GeoLocation?) {
                val key = dataSnapshot?.key
                println("key"+key)
                val eRef = FirebaseDatabase.getInstance().reference.child("Event/$key")
                val postListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val value = dataSnapshot.getValue(Event::class.java)
                        if (value != null){
                            value.id= dataSnapshot.key
                            allEvents.add(value)
                            filteredEvents = allEvents
                            adapter?.setData(filteredEvents)
                        } //else {
                        // alert user to add first event
                        //              alert("Be the first to add an event!", "No events found nearby"){
                        //                    yesButton {
                        //                      // present user with add event screen
                        //                            startActivity(intentFor<EventEditorActivity>())
                        //                          }
                        //                            cancelButton {  }
                        //                          }.show()
//
                        //}
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                        // ...
                    }
                }
                eRef.addValueEventListener(postListener)

            }

            override fun onDataMoved(dataSnapshot: DataSnapshot?, location: GeoLocation?) {

            }

            override fun onGeoQueryError(error: DatabaseError?) {

            }

        })
        recyclerView.adapter = adapter

    }

    class EventAdapter : MultiChoiceAdapter<EventHolder>() {
        private var mItems: ArrayList<Event> = arrayListOf()

        fun setData(items: ArrayList<Event>){
            mItems = items
            notifyDataSetChanged()
            deselectAll()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.recyclerview_item_row, parent, false)
                .inflate(R.layout.event_item, parent, false)

            return EventHolder(view)
        }

        override fun onBindViewHolder(holder: EventHolder, position: Int) {
            val item = mItems[position]
            holder.bind(item)
            super.onBindViewHolder(holder, position)
        }

        override fun defaultItemViewClickListener(holder: EventHolder?, position: Int): View.OnClickListener {
            return View.OnClickListener {
                val context = holder!!.mView.context
                val item = mItems[position]
                context.startActivity(context.intentFor<EventDetailsActivity>("id" to item.id, "title" to item.eventName))
            }

        }

        fun getItem(position: Int): Event {
            return mItems[position]
        }

        override fun getItemCount(): Int {
            return mItems.count()
        }

        override fun setActive(view: View, state: Boolean) {
            super.setActive(view, state)
            deleteMenu?.isVisible = selectedItemCount > 0
        }
    }


    class EventHolder(val mView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        fun bind(item: Event){
            mView.imageView.setImageResource(R.drawable.vd)
            mView.name.text = item.eventName
            val descText = item.eventCategory + " in " + item.eventCity
            val reviewsText = "${item.Reviews.count()}  reviews"
            mView.description.text = descText
            mView.reviews.text = reviewsText
            mView.rating.rating = VendorUtils.calculateRating(item)
            if (item.images.isNotEmpty()){
                val thumb = item.images.entries.iterator().next()
                val imageRef = FirebaseStorage.getInstance().reference.child("images/"+thumb.key)
                mView.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    Picasso.get().load(uri).resize(1024, 768).into(mView.imageView)
                }
            } else {
                mView.imageView.setImageResource(VendorUtils.getRandomImage(mView.context, item.eventCategory))
            }
        }
    }

    fun generateLocation() {
        val ref = FirebaseDatabase.getInstance().reference.child("Event")
        val lRef = FirebaseDatabase.getInstance().reference.child("Location")

        val postListener = object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                for (p in p0.children){
                    val event = p.getValue(Event::class.java)
                    if (event != null){
                        val key = event.eventCity.trim() + ", " + event.eventState.trim() + ", " + event.eventCountry.trim()
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        val addresses = geocoder.getFromLocationName(key, 1)
                        val address = addresses[0]
                        val location = GPSLocation(event.eventCity, event.eventState, address.longitude, address.latitude)
                        lRef.child(key).setValue(location)
                    }

                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                //               Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }

        }
        ref.addListenerForSingleValueEvent(postListener)
    }

    private fun performLogin(){
//        val providers = listOf(AuthUI.IdpConfig.FacebookBuilder().build())
// Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build())

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.mipmap.ic_launcher)
                .build(),
            REQUEST_CODE_SIGN_IN)
    }

    private fun performLogout(){
//        FirebaseAuth.getInstance().signOut()
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                // ...
            }
        button_auth.text = getString(R.string.sign_in)
    }

    private fun checkAuthStatus(){
        if (FirebaseAuth.getInstance().currentUser == null){
                performLogin()
        } else {
                performLogout()
        }
    }

    private fun showMessage(text: String) {
        val container = findViewById<View>(R.id.main_activity_container)
        if (container != null) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {

        Toast.makeText(this@MainActivity, getString(mainTextStringId), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: String){
        if (event == "reload"){
            loadByLocation((lastLocation)!!.latitude, (lastLocation)!!.longitude)
        }
    }
}
