package com.streetvendorhelpernew.activity

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.streetvendorhelpernew.R
import com.streetvendorhelpernew.model.GPSLocation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.location_item.view.*
import org.jetbrains.anko.toast
import java.util.*


class LocationActivity : AppCompatActivity() {
    private var allLocations: ArrayList<GPSLocation> = arrayListOf()
    private var filteredLocations: ArrayList<GPSLocation> = arrayListOf()
    var adapter: LocationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.my_toolbar))

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val layoutManeger = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManeger
        recyclerView.adapter = adapter
        adapter = LocationAdapter()
        recyclerView.adapter = adapter

        bottom_layout.visibility = View.GONE

        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                filteredLocations = arrayListOf()
                if (p0.isNullOrEmpty()){
                    adapter?.setData(allLocations)
                } else {
                    for (location in allLocations){
                        if (location.eventCity.toLowerCase().contains(p0.toLowerCase()) || location.eventState.toLowerCase().contains(p0.toLowerCase())){
                            filteredLocations.add(location)
                        }
                    }
                    adapter?.setData(filteredLocations)
                }

                return true
            }

        })

        loadData()
    }


    private fun loadData(){
        val ref = FirebaseDatabase.getInstance().reference.child("Event")
        println("reference"+ref)
        allLocations = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                for (snapshot in p0.children) {
                    println("snapshot = "+snapshot)
                    try {
                        val eventLocale = snapshot.getValue(GPSLocation::class.java)
                        if (eventLocale != null) {
                            allLocations.add(eventLocale)
                        }
                    }catch (ex:Exception){

                        toast(ex.localizedMessage)


                    }

                    adapter?.setData(allLocations)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                //nothing
            }
        }
        ref.addValueEventListener(postListener)
    }


    class LocationAdapter : RecyclerView.Adapter<LocationHolder>() {
        private var mItems: ArrayList<GPSLocation> = arrayListOf()

        fun setData(items: ArrayList<GPSLocation>){
            mItems = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.location_item, parent, false)

            return LocationHolder(view)
        }

        override fun onBindViewHolder(holder: LocationHolder, position: Int) {
            val context = holder.mView.context as Activity
            val item = mItems[position]
            holder.bind(item)
            holder.mView.setOnClickListener {


                val geocoder = Geocoder(
                    context,
                    Locale.getDefault()
                )
                val key = item.eventCity.trim() + ", " + item.eventState.trim()

                try {
                    val addressList: List<*> =
                        geocoder.getFromLocationName(key, 1)
                    if (addressList != null && addressList.size > 0) {
                        val address: Address = addressList[0] as Address
                        val sb = StringBuilder()
                        sb.append(address.getLatitude()).append("\n")
                        sb.append(address.getLongitude()).append("\n")

                        val data = Intent()
                        data.putExtra("lat", address.latitude)
                        data.putExtra("long", address.longitude)
                        data.putExtra("name", item.eventCity)
                        context.setResult(Activity.RESULT_OK, data)
                        context.finish()
                    }else {

                        context.toast("Location is not on google")
                    }
                }catch (ex:Exception){

                    context.toast(ex.localizedMessage)
                }





            }


        }


        override fun getItemCount(): Int {
            return mItems.count()
        }

    }


    class LocationHolder(val mView: View) : RecyclerView.ViewHolder(mView) {

        fun bind(item: GPSLocation){
            val cityState = item.eventCity + ", " + item.eventState
            mView.name.text = cityState

        }
    }


}
