package com.example.whereami
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.whereami.NeshanResponse
import com.example.whereami.NeshanApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var neshanApi: NeshanApi
    private var address:String  = ""
    private var extraInfo = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Retrofit setup
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.neshan.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        neshanApi = retrofit.create(NeshanApi::class.java)
        val myButton = findViewById<Button>(R.id.btnSearch)
        myButton.setOnClickListener {
            requestLocationPermission()
        }

        val btnShareLocation = findViewById<Button>(R.id.btnShare)
        btnShareLocation.setOnClickListener {
            shareLocation(address)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, access the location
            getLastLocation()
        } else {
            // Permission denied, show a message to the user
            showToast("Permission denied")
        }
    }


    private fun requestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showToast("Location permission is required to show your location.")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }


    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                getAddressFromCoordinates(latitude, longitude)
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun getAddressFromCoordinates(latitude: Double, longitude: Double) {
        val apiKey = BuildConfig.API_KEY
        neshanApi.getAddress(apiKey, latitude, longitude).enqueue(object : Callback<NeshanResponse> {
            override fun onResponse(call: Call<NeshanResponse>, response: Response<NeshanResponse>) {
                if (response.isSuccessful) {
                    val neshanResponse = response.body()
                    neshanResponse?.let {
                        val addressBuilder = StringBuilder()
                        val extraInformation = StringBuilder()

                        if (it.state.isNotBlank()) addressBuilder.append("${it.state} ")
                        if (it.city.isNotBlank()) addressBuilder.append(" شهر ${it.city} ")
                        if (it.neighbourhood.isNotBlank()) addressBuilder.append(" محله ${it.neighbourhood} ")
                        if (it.route_name.isNotBlank()) addressBuilder.append(" کوچه ${it.route_name}\n")

                        if (it.municipality_zone.isNotBlank()) extraInformation.append(" منطقه ${it.municipality_zone}\n")
                        val oddEvenStatus = if (it.in_odd_even_zone.equals("true", ignoreCase = true)) "بله" else "خیر"
                        val trafficZoneStatus = if (it.in_traffic_zone.equals("true", ignoreCase = true)) "بله" else "خیر"
                        extraInformation.append(" طرح زوج و فرد؟: $oddEvenStatus \n")
                        extraInformation.append(" طرح ترافیک؟: $trafficZoneStatus")


                        val formattedAddress = addressBuilder.toString().trim()
                        extraInfo = extraInformation.toString().trim()
                        address = formattedAddress

                        // Update your UI with the formatted address
                        runOnUiThread {
                            updateUI(formattedAddress + extraInfo)
                        }
                    }
                } else {
                    // Handle error
                    Log.e("NeshanAPI", "Error: ${response.code()}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to get address", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<NeshanResponse>, t: Throwable) {
                // Handle network error
                Log.e("NeshanAPI", "Network error: ${t.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    private fun updateUI(address: String) {
        val locTextView = findViewById<TextView>(R.id.tvLocation)
        locTextView.text = address
    }

    private fun shareLocation(address: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, address)
            }
            startActivity(Intent.createChooser(intent, "ارسال به"))
        } catch (e: Exception) {
            Log.e("ShareLocation", "Error sharing location: ${e.message}", e)
            Toast.makeText(this, "خطا در اشتراک‌گذاری موقعیت", Toast.LENGTH_SHORT).show()
        }
    }
}
