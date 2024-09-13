package com.example.proje.data.search

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class SearchGoogle(private val context: Context) {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(GoogleSearchApi::class.java)

    fun search(query: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.search(query, "b5a72a640ce944376","AIzaSyD9922mnQ8lRkF3EQee8UW9yi4nIF4NRrw").execute()
                if (response.isSuccessful) {
                    val firstLink = response.body()?.items?.firstOrNull()?.link
                    withContext(Dispatchers.Main) {
                        callback(firstLink)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
}