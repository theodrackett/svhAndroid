package com.streetvendorhelper.model

data class Review(var id: String? = null,
                  var Comment: String = "",
                  var Rating: Long = 0,
                  var Reviewer: String = ""
                 )