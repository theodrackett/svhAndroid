package com.streetvendorhelpernew.model

data class Event(var id: String? = null,
                 var eventCategory: String = "",
                 var eventCity: String = "",
                 var eventContact: String = "",
                 var eventCountry: String = "",
                 var eventCreator: String = "",
                 var eventFromDate: String = "",
                 var eventEmail: String = "",
                 var eventName: String = "",
                 var eventPhone: String = "",
                 var eventToDate: String = "",
                 var eventState: String = "",
                 var eventStreet: String = "",
                 var eventWebSite: String = "",
                 var frequency: String = "",
                 var images: HashMap<String, Image> = hashMapOf(),
                 var Reviews: HashMap<String, Review> = hashMapOf()
                 )