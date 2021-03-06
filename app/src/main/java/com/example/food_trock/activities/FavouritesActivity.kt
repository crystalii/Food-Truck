package com.example.food_trock.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.food_trock.DataManager
import com.example.food_trock.R
import com.example.food_trock.adapters.FavoritesAdapter
import com.example.food_trock.firebase.FireStoreClass
import com.example.food_trock.fragments.StoreFragment
import com.example.food_trock.models.Store
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_store.*
import kotlinx.android.synthetic.main.store_item.*
import java.math.RoundingMode
import java.text.DecimalFormat

class FavouritesActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    val userId = FireStoreClass().getCurrentUserID()
    lateinit var recyclerView: RecyclerView
    val db: FirebaseFirestore = Firebase.firestore
    val usersRef = db.collection("Users").document(userId)
    val foodTruckRef = db.collection("FoodTrucks")

    private val navigation = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.favourites -> {
                return@OnNavigationItemSelectedListener false
            }
            R.id.trucks -> {

                bottomNavigationView.menu.getItem(0).isChecked = false
                val intent = Intent(this@FavouritesActivity, StoreActivity::class.java)
                startActivity(intent)
                DataManager.tempStores.clear()

                return@OnNavigationItemSelectedListener true
            }
            R.id.settings -> {
                bottomNavigationView.menu.getItem(0).isChecked = false

                val intentAdmin = Intent(this, AdminPortalActivity::class.java)
                val intentClient = Intent(this, UserProfileActivity::class.java)
                val intentTruckOwner = Intent(this, OwnerSettingsActivity::class.java)
                val intentLogin = Intent(this, LoginActivity::class.java)

                if(DataManager.currentUserRole.admin) {
                    startActivity(intentAdmin)
                } else if (DataManager.currentUserRole.foodTruckOwner) {
                    startActivity(intentTruckOwner)
                } else if (DataManager.currentUserRole.client){
                    startActivity(intentClient)
                } else {
                    startActivity(intentLogin)
                }

                return@OnNavigationItemSelectedListener true
            }
            R.id.maps -> {
                val intent = Intent(this@FavouritesActivity, MapsActivity::class.java)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true
            }
        }
        false

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_favourites)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(navigation)
        bottomNavigationView.menu.getItem(0).isChecked = true

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var favoritesAdapter = FavoritesAdapter(this, DataManager.favorites)
        recyclerView.adapter = favoritesAdapter


        favoritesAdapter.setOnItemClickListener(object : FavoritesAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {

                val storeFragment = StoreFragment()

                var selectedStore: Store = favoritesAdapter.favoriteList[position]
                val bundle = Bundle()
                storeFragment.arguments = bundle
                bundle.putString("storeName", selectedStore.storeName)
                bundle.putInt("storePriceClass", selectedStore.storePriceClass)
                bundle.putString("storeImage", selectedStore.storeImage)
                bundle.putString("storeID", selectedStore.UID)

                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.container, storeFragment, "store")
                transaction.commit()
            }
        })


        loadFavorites()




    }

    fun loadFavorites () {
        usersRef.get().addOnSuccessListener { task ->
            if(task != null) {
                DataManager.favorites.clear()
                val listOfFavorites = task.get("favorites") as List<*>
                for(favorite in listOfFavorites) {
                    getStore(favorite as String)
                }
            }
        }
    }


    fun getStore (storeID: String) {
            foodTruckRef.document(storeID).get().addOnSuccessListener { task ->
                val store = task.toObject(Store::class.java)
                if (store != null) {
                    store.distance = calculateDistance(
                        DataManager.currentLat,
                        DataManager.currentLng,store.storeLatitude,store.storeLongitude)
                    DataManager.favorites.add(store)
                    DataManager.favorites.sortedWith(compareBy(Store::storeStatus, Store::distance))
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }

    }


    fun calculateDistance (startLatitude: String,
                           startLongitude: String,
                           endLatitude: Double,
                           endLongitude: Double,
    ):Double {
        try {
            val startLat = startLatitude.toDouble()
            val startLong = startLongitude.toDouble()
            val endLat = endLatitude
            val endLong = endLongitude

            val theta = startLong - endLong
            var dist = Math.sin(deg2rad(startLat)) * Math.sin(deg2rad(endLat)) + Math.cos(deg2rad(startLat)) * Math.cos(deg2rad(endLat)) * Math.cos(deg2rad(theta))
            dist = Math.acos(dist)
            dist = rad2deg(dist)
            dist = dist * 60 * 1.1515
            dist = dist * 1.609344

            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING


            return dist
        }
        catch (e: Exception) {
            return 0.0
        }


    }
    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

}