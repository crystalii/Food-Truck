package com.example.food_trock.models

import com.example.food_trock.DataManager
import com.google.firebase.firestore.DocumentId

data class MenuItem(@DocumentId var documentID: String? = null,
                    var foodName: String = "",
                    var foodPrice: Int = 0,
                    var foodImage: String? = null) {


}