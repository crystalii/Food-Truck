package com.example.food_trock.activities

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.food_trock.DataManager
import com.example.food_trock.R
import com.example.food_trock.databinding.ActivityMapsBinding
import com.example.food_trock.fragments.StoreFragment
import com.example.food_trock.models.Store
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var myLocationPin: Bitmap
    lateinit var cardStoreInfo: CardView
    lateinit var cardStoreImage: ImageView
    lateinit var cardStoreName: TextView
    lateinit var cardRating: RatingBar
    lateinit var cardRatingTxt: TextView
    private var firstTime: Boolean = true
    val auth : FirebaseAuth = Firebase.auth
    val markers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getButtonNavigationBar()
        cardStoreInfo = findViewById(R.id.cardStoreInfo)
        cardStoreImage = findViewById(R.id.cardStoreImage)
        cardStoreName = findViewById(R.id.cardStoreName)
        cardRating = findViewById(R.id.cardRatingBar)
        cardRatingTxt = findViewById(R.id.txtCardRating)

        myLocationPin = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.mylocation)

    }

    override fun onMapReady(googleMap: GoogleMap) {

        getStores()
        val storeLatFocus = intent.getDoubleExtra("lat",0.0)
        val storeLongFocus = intent.getDoubleExtra("long",0.0)
        val focusKey = intent.getStringExtra("key")
        mMap = googleMap

        if (DataManager.currentLng != "" && DataManager.currentLat != null) {
            addMarker(
                LatLng(DataManager.currentLat.toDouble(), DataManager.currentLng.toDouble()),
                "You are here",
                null
                , null)

            if (firstTime) {
                firstTime = false
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(DataManager.currentLat.toDouble(), DataManager.currentLng.toDouble()), 15f))
            }
        }


        mMap.setOnMarkerClickListener { marker ->
                for (store in DataManager.stores) {
                    if (store.storeName == marker.tag) {
                        cardStoreInfo.visibility = View.VISIBLE
                        Glide.with(this).load(store.storeImage).into(cardStoreImage)
                        cardStoreName.text = store.storeName
                        cardRating.rating = store.storeRating.toFloat()
                        cardRatingTxt.text = store.storeRating.toString()
                    }
                }
                if (marker.isInfoWindowShown) {

                } else {
                    marker.showInfoWindow()
                }
                true
            }


        mMap.setOnMapClickListener {
            cardStoreInfo.visibility = View.GONE
        }

        moreInfoBtn.setOnClickListener() {

            if(auth.currentUser != null) {
                val storeFragment = StoreFragment()

                for(store in DataManager.stores) {
                    if(store.storeName == cardStoreName.text) {
                        val bundle = Bundle()
                        storeFragment.arguments = bundle
                        bundle.putString("storeName", store.storeName)
                        bundle.putInt("storePriceClass", store.storePriceClass)
                        bundle.putString("storeImage", store.storeImage)
                        bundle.putString("storeID", store.UID)
                        bundle.putString("phoneNumber",store.phoneNumber)
                        bundle.putString("openHrs",store.openHrs)
                        bundle.putFloat("rating",store.storeRating)

                        val transaction = supportFragmentManager.beginTransaction()
                        transaction.add(R.id.container, storeFragment, "store")
                        transaction.commit()
                    }
                }
            } else {
                val intentLogin = Intent(this,LoginActivity::class.java)
                startActivity(intentLogin)
            }

        }

        if(focusKey == "KEY_STORE") {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(storeLatFocus,storeLongFocus),15f))
            for (store in DataManager.stores) {
                if (storeLatFocus == store.storeLatitude) {
                    cardStoreInfo.visibility = View.VISIBLE
                    Glide.with(this).load(store.storeImage).into(cardStoreImage)
                    cardStoreName.text = store.storeName
                    cardRating.rating = store.storeRating
                    cardRatingTxt.text = store.storeRating.toString()
                }
            }
        }
    }

    private fun removeAllMarkers() {
        for (mLocationMarker in markers) {
            mLocationMarker.remove()
        }
        markers.clear()
    }


    fun getStores() {

        val db: FirebaseFirestore = Firebase.firestore

        db.collection("FoodTrucks").addSnapshotListener(object: EventListener<QuerySnapshot> {
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                if(value != null) {
                    DataManager.stores.clear()
                    removeAllMarkers()

                    for(document in value.documents) {
                        val store = document.toObject(Store::class.java)
                        if (store != null && store.storeStatus) {
                            loadMarker(store)
                        }
                    }
                }
            }

        })
    }

    private fun loadMarker (store: Store) {
        DataManager.stores.add(store)
        Log.e("StoreSize", DataManager.stores.size.toString())
        val storeLatLng = LatLng(store.storeLatitude, store.storeLongitude)
        Glide.with(this)
            .asBitmap()
            .load(store.storeImage)
            .fitCenter()
            .into(object : CustomTarget<Bitmap>(200, 200){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    addMarker(storeLatLng, store.storeName, resource, store.storeName)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }
            })
    }

    private fun getCroppedBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), (
                    bitmap.width / 4).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output
    }

    fun addMarker(latLng: LatLng, title :String, image: Bitmap?, tag: String?){
        if (image != null) {
            val Icon: BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(getCroppedBitmap(image))
            val markerOptions = MarkerOptions().position(latLng).title(title).icon(Icon)
            val marker: Marker =  mMap.addMarker(markerOptions)
            marker.setTag(tag)

            markers.add(marker)
        } else {
            val markerOptions = MarkerOptions().position(latLng).title("You are here")
            mMap.addMarker(markerOptions).setTag(tag)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    fun getButtonNavigationBar(){
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.menu.getItem(2).isChecked = true

        val navigation = BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.favourites -> {
                    bottomNavigationView.menu.getItem(2).isChecked = false
                    val intentLogin = Intent(this, LoginActivity::class.java)
                    val intentFavorites = Intent(this, FavouritesActivity::class.java)
                    if(auth.currentUser != null) {
                        startActivity(intentFavorites)
                    } else {
                        startActivity(intentLogin)
                    }
                    return@OnNavigationItemSelectedListener true
                }
                R.id.trucks -> {
                    bottomNavigationView.menu.getItem(2).isChecked = false
                    val intent = Intent(this@MapsActivity, StoreActivity::class.java)
                    startActivity(intent)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.settings -> {
                    bottomNavigationView.menu.getItem(2).isChecked = false
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
                    return@OnNavigationItemSelectedListener false
                }
            }
            false

        }
        bottomNavigationView.setOnNavigationItemSelectedListener(navigation)

    }
}
