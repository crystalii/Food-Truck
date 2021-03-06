package com.example.food_trock.models

data class Store(var fullName: String = "",
                 var storeImage: String= "",
                 var storeName: String = "",
                 var openHrs: String = "",
                 var storePriceClass: Int = 0,
                 var storeRating: Float = 1f,
                 var phoneNumber: String = "",
                 var storeStatus: Boolean = false,
                 var UID: String = "",
                 var storeLatitude: Double = 0.0,
                 var storeLongitude: Double = 0.0,
                 var category1: String = "",
                 var category2: String = "",
                 var distance: Double = 0.0)
