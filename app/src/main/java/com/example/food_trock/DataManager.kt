package com.example.food_trock

import com.example.food_trock.models.MenuItem
import com.example.food_trock.models.Store

object DataManager {

    val stores = mutableListOf<Store>()

    val menus = mutableListOf<MenuItem>()

    val tempStores = mutableListOf<Store>()
    val temp2Stores = mutableListOf<Store>()
    var currentLat: String = ""
    var currentLng: String = ""

}