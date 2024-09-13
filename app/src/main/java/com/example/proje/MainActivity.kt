// MainActivity.kt
package com.example.proje

import Message
import MessageAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.proje.data.search.SearchGoogle
import com.example.proje.databinding.ActivityMainBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var geminiApiKey: String
    private lateinit var searchGoogle: SearchGoogle
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private val REQUEST_CODE_OVERLAY_PERMISSION = 101

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

    private var karaSimsekMode = false

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val command = intent?.getStringExtra("command")
            if (command != null) {
                handleCommand(command)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceIntent = Intent(this, MyService::class.java)
        startService(serviceIntent)

        requestQueue = Volley.newRequestQueue(this)
        geminiApiKey = BuildConfig.GEMINI_API_KEY
        searchGoogle = SearchGoogle(this)

        adapter = MessageAdapter(messages)
        binding.recyclerViewChat.adapter = adapter
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)

        binding.buttonVoiceCommand.setOnClickListener {
            startVoiceCommand()
        }

        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            if (message.isNotBlank()) {
                val userMessage = Message(message, true)
                messages.add(userMessage)
                adapter.notifyItemInserted(messages.size - 1)
                binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                binding.editTextMessage.text.clear()
                sendGeminiRequest(message)
            } else {
                Toast.makeText(this, "Mesaj boş olamaz", Toast.LENGTH_SHORT).show()
            }
        }

        if (intent.getBooleanExtra("START_VOICE_COMMAND", false)) {
            startVoiceCommand()
        }

        checkAndRequestOverlayPermission()

        val filter = IntentFilter("com.example.proje.COMMAND")
        registerReceiver(commandReceiver, filter)
    }

    private fun sendGeminiRequest(message: String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = geminiApiKey,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 8192
                responseMimeType = "text/plain"
            },
            systemInstruction = content { text("Google'dan arama yapmak istenirse URL gönder.\n" +
                    "Haritalardan aç gibi bir ifade kullanılırsa istenen konumun google haritalarının URL döndür.\n") }
        )

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(message)
                }
                Log.d("Test", response.toString())
                val jsonResponse = response.candidates.first().content.parts.first().asTextOrNull() ?: ""

                val urlPattern = "http[s]?://.*".toRegex()
                val urlMatch = urlPattern.find(jsonResponse)

                if (urlMatch != null) {
                    openLink(urlMatch.value)
                } else {
                    val aiMessage = Message(jsonResponse, false)
                    messages.add(aiMessage)
                    runOnUiThread {
                        adapter.notifyItemInserted(messages.size - 1)
                        binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startVoiceCommand() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } else {
            Toast.makeText(this, "Cihazınız sesli komutları desteklemiyor", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Link açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            }
        }
    }

    private fun handleCommand(command: String) {
        Log.d("MainActivity", "Received command: $command")

        if (command.lowercase(Locale.getDefault()).contains("kara şimşek kapan")) {
            karaSimsekMode = false
            Toast.makeText(this, "Kara Şimşek modu kapatıldı", Toast.LENGTH_SHORT).show()
        } else if (command.lowercase(Locale.getDefault()).contains("kara şimşek")) {
            karaSimsekMode = true
            Toast.makeText(this, "Kara Şimşek modu aktif", Toast.LENGTH_SHORT).show()
        } else if (karaSimsekMode) {
            sendGeminiRequest(command)
        } else {
            Log.d("MainActivity", "Kara Şimşek modu aktif değil, komut işlenmedi.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {

            } else {
                Toast.makeText(this, "Overlay izinleri verilmedi", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            data?.let {
                val result = it.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                result?.let { results ->
                    if (results.isNotEmpty()) {
                        val spokenText = results[0]
                        handleCommand(spokenText)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(commandReceiver)
    }
}
