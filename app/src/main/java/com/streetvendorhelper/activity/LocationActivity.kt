package com.streetvendorhelper.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.streetvendorhelper.model.GPSLocation
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.streetvendorhelper.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.location_item.view.*
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
                        if (location.city.toLowerCase().contains(p0.toLowerCase()) || location.state.toLowerCase().contains(p0.toLowerCase())){
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
        val ref = FirebaseDatabase.getInstance().reference.child("Location")
        allLocations = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                for (snapshot in p0.children) {
                    val eventLocale = snapshot.getValue(GPSLocation::class.java)
                    if (eventLocale != null) {
                        allLocations.add(eventLocale)
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

                val data = Intent()
                data.putExtra("lat", item.latitude)
                data.putExtra("long", item.longitube)
                data.putExtra("name", item.city)
                context.setResult(Activity.RESULT_OK, data)
                context.finish()
            }
        }


        override fun getItemCount(): Int {
            return mItems.count()
        }

    }


    class LocationHolder(val mView: View) : RecyclerView.ViewHolder(mView) {

        fun bind(item: GPSLocation){
            val cityState = item.city + ", " + item.state
            mView.name.text = cityState

        }
    }
}
