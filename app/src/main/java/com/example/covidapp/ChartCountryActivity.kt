package com.example.covidapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.covidapp.model.InfoNegara
import com.example.covidapp.model.Negara
import com.example.covidapp.network.InfoService
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_chart_country.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ChartCountryActivity : AppCompatActivity() {

    companion object{
        const val EXTRA_COUNTRY = "EXTRA_COUNTRY"
        lateinit var simpanDataNegara:String
        lateinit var simpanDataFlag:String
    }

    //untuk menyimpan data kecil dan lokal di dalam HP
    private val sharedPrefFile = "kotlinsharedpreference"
    private lateinit var sharedPreferences: SharedPreferences
    private var dayCases = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart_country)

        sharedPreferences = this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)

        //format angka
        val formatter: NumberFormat = DecimalFormat("#,###")
        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        //mendapat data parcelized
        val data = intent.getParcelableExtra<Negara>(EXTRA_COUNTRY)

        //jika data null
        data?.let {
//            mengindikasikan data yang akan di tampilkan di antarmuka
            country_name.text = data.Country
            latest_update.text = data.Country
            hasil_total_death_currently.text = formatter.format(data.TotalDeaths?.toDouble())
            hasi_new_death_currently.text = formatter.format(data.NewDeaths?.toDouble())
            hasil_total_confirmed_currently.text = formatter.format(data.TotalConfirmed?.toDouble())
            hasi_new_confirmed_currently.text = formatter.format(data.NewConfirmed?.toDouble())
            hasil_total_recovered_currently.text = formatter.format(data.TotalRecovered?.toDouble())
            hasi_new_recovered_currently.text = formatter.format(data.NewRecovered?.toDouble())

            editor.putString(data.Country, data.Country)
            editor.apply()
            editor.commit()

            val simpaNegara = sharedPreferences.getString(data.Country, data.Country)
            val simpanFlag = sharedPreferences.getString(data.CountryCode, data.CountryCode)
            simpanDataNegara = simpaNegara.toString()
            simpanDataFlag = simpanFlag.toString() + "/flat/64.png"

            if(simpanFlag != null){
                Glide.with(this).load("https://www.countryflags.io/$simpanDataFlag")
                    .into(img_flag_country)
            }else{
                Toast.makeText(this, "ImageNot Found", Toast.LENGTH_SHORT).show()
            }

            getCountry()

        }
    }

    private fun getCountry(){
     val okHttp = OkHttpClient().newBuilder()
         .connectTimeout(15, TimeUnit.SECONDS)
         .readTimeout(15, TimeUnit.SECONDS)
         .writeTimeout(15,TimeUnit.SECONDS)
         .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.covid19api.com/dayone/country/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(InfoService::class.java)
        api.getInformation(simpanDataNegara).enqueue(object : Callback<List<InfoNegara>>{
            @SuppressLint("SimpleDataFormat")
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<List<InfoNegara>>,
                response: Response<List<InfoNegara>>
            ) {
                val getListDataCorona: List<InfoNegara> = response.body()!!
                if (response.isSuccessful){
                    val barEntries: ArrayList<BarEntry> = ArrayList()
                    val barEntries2: ArrayList<BarEntry> = ArrayList()
                    val barEntries3: ArrayList<BarEntry> = ArrayList()
                    val barEntries4: ArrayList<BarEntry> = ArrayList()
                    var i = 0
                    while (i < getListDataCorona.size){
                        for (s in getListDataCorona){
                            val barEntry = BarEntry(i.toFloat(), s.Confirmed?.toFloat()?:0f)
                            val barEntry2 = BarEntry(i.toFloat(), s.Deaths?.toFloat()?:0f)
                            val barEntry3 = BarEntry(i.toFloat(), s.Recovered?.toFloat()?:0f)
                            val barEntry4 = BarEntry(i.toFloat(), s.Active?.toFloat()?:0f)

                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS'Z'")
                            val outputFormat = SimpleDateFormat("dd-MM-yyyy")
                            val date: Date? = inputFormat.parse(s.Date!!)
                            val formattedDate: String = outputFormat.format(date!!)
                            dayCases.add(formattedDate)

                            barEntries.add(barEntry)
                            barEntries2.add(barEntry2)
                            barEntries3.add(barEntry3)
                            barEntries4.add(barEntry4)

                            i++
                        }
                        val xAxis:XAxis = barChardView.xAxis
                        xAxis.valueFormatter = IndexAxisValueFormatter(dayCases)
                        barChardView.axisLeft.axisMinimum =0f
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.setCenterAxisLabels(true)
                        xAxis.isGranularityEnabled = true

                        val barDataSet = BarDataSet(barEntries,"Confiorimed")
                        val barDataSet2 = BarDataSet(barEntries2,"Deaths")
                        val barDataSet3 = BarDataSet(barEntries3,"Recovered")
                        val barDataSet4 = BarDataSet(barEntries4,"Active")
                        barDataSet.setColors(Color.parseColor("#F44336"))
                        barDataSet2.setColors(Color.parseColor("#FFEB3B"))
                        barDataSet3.setColors(Color.parseColor("#03DAC5"))
                        barDataSet4.setColors(Color.parseColor("#2196F3"))

                        val data = BarData(barDataSet,barDataSet2,barDataSet3,barDataSet4)
                        barChardView.data = data

                        val barSpace = 0.02f
                        val groupSpace= 0.03f
                        val groupCount = 4f

                        data.barWidth = 0.15f
                        barChardView.invalidate()
                        barChardView.setNoDataTextColor(R.color.black)
                        barChardView.setTouchEnabled(true)
                        barChardView.description.isEnabled = false
                        barChardView.xAxis.axisMinimum = 0f
                        barChardView.setVisibleXRangeMaximum(
                            0f + barChardView.barData.getGroupWidth(
                                groupSpace,
                                barSpace
                            ) * groupCount
                        )
                        barChardView.groupBars(0f, groupSpace, barSpace)
                    }
                }
            }

            override fun onFailure(call: Call<List<InfoNegara>>, t: Throwable) {
            Toast.makeText(this@ChartCountryActivity, "error re-enter o this country",
            Toast.LENGTH_SHORT).show()

            }
        })
    }
}