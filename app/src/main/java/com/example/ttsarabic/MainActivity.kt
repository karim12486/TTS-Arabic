package com.example.ttsarabic

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*

class MainActivity : AppCompatActivity() {

    val client = OkHttpClient()
    private lateinit var delete_button: ImageButton
    private lateinit var import: ImageButton
    private lateinit var edit_text: EditText
    private lateinit var button: Button
    private val REQUEST_CODE_OPEN_FILE = 1


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val requestCode = 1 // Unique code to identify the permission request

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            requestPermissions(permissions, requestCode)

        }

        delete_button = findViewById(R.id.deletebutton)
        import = findViewById(R.id.importer)
        edit_text = findViewById(R.id.editText)
        button = findViewById(R.id.button)
        delete_button.setOnClickListener {
            edit_text.setText("")
        }

        import.setOnClickListener {
            openFilePicker()
        }

        button.setOnClickListener {
            val apiUrl = "http://192.168.1.69:8080/api/tts"
            var text = edit_text.text.toString()
            text = "{\"text\":\"$text\"}"
            println(text)
            System.currentTimeMillis()
            System.currentTimeMillis()
//            println(TimeUnit.NANOSECONDS.toMillis(end - start))
            print("ggggggggggggggggggggg")

            // Create the request body
//            val requestBody = RequestBody.create("application/json; charset=utf-16".toMediaTypeOrNull(), text)
            val requestBody =
                text.toRequestBody("application/json; charset=utf-16".toMediaTypeOrNull())
            // Create the POST request
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            // Execute the request
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request.")
                    // Handle request failure
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    println("successful.")
                    // Check if the request was successful
                    if (response.isSuccessful) {
                        // Save the response as a binary file
                        val inputStream = response.body?.byteStream()
                        val outputFile = File(this@MainActivity.filesDir, "output.bin")
                        val outputStream = FileOutputStream(outputFile.absolutePath)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()

                        // Convert binary file to .wav format
                        val wavFile = File(this@MainActivity.filesDir, "output.wav")
                        convertToWav(outputFile, wavFile)

                        // Play the audio file
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setDataSource(wavFile.path)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    } else {
                        println("Unsuccessful response.")
                        // Handle unsuccessful response
                        // ...
                    }
                }
            })
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE)
    }

    private fun readFileContent(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                val content = reader?.readText()
                edit_text.setText(content)
                reader?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the exception
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                readFileContent(uri)
            }
        }
    }

    private fun convertToWav(inputFile: File, outputFile: File) {
        val rawBytes = inputFile.readBytes()
        val outputStream = FileOutputStream(outputFile)
        outputStream.write(rawBytes)
        outputStream.close()
    }

    private fun generateWavHeader(dataSize: Long): ByteArray {
        val headerSize = 44
        val totalSize = dataSize + headerSize - 8
        val sampleRate = 44100
        val bitsPerSample = 16
        val numChannels = 1
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8

        val header = ByteArray(headerSize)

        // ChunkID (4 bytes)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        // ChunkSize (4 bytes)
        header[4] = (totalSize and 0xFF).toByte()
        header[5] = (totalSize shr 8 and 0xFF).toByte()
        header[6] = (totalSize shr 16 and 0xFF).toByte()
        header[7] = (totalSize shr 24 and 0xFF).toByte()

        // Format (4 bytes)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // Subchunk1ID (4 bytes)
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        // Subchunk1Size (4 bytes)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // AudioFormat (2 bytes)
        header[20] = 1
        header[21] = 0

        // NumChannels (2 bytes)
        header[22] = numChannels.toByte()
        header[23] = 0

        // SampleRate (4 bytes)
        header[24] = (sampleRate and 0xFF).toByte()
        header[25] = (sampleRate shr 8 and 0xFF).toByte()
        header[26] = (sampleRate shr 16 and 0xFF).toByte()
        header[27] = (sampleRate shr 24 and 0xFF).toByte()

        // ByteRate (4 bytes)
        header[28] = (byteRate and 0xFF).toByte()
        header[29] = (byteRate shr 8 and 0xFF).toByte()
        header[30] = (byteRate shr 16 and 0xFF).toByte()
        header[31] = (byteRate shr 24 and 0xFF).toByte()

        // BlockAlign (2 bytes)
        header[32] = (blockAlign and 0xFF).toByte()
        header[33] = (blockAlign shr 8 and 0xFF).toByte()

        // BitsPerSample (2 bytes)
        header[34] = (bitsPerSample and 0xFF).toByte()
        header[35] = (bitsPerSample shr 8 and 0xFF).toByte()

        // Subchunk2ID (4 bytes)
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        // Subchunk2Size (4 bytes)
        header[40] = (dataSize and 0xFF).toByte()
        header[41] = (dataSize shr 8 and 0xFF).toByte()
        header[42] = (dataSize shr 16 and 0xFF).toByte()
        header[43] = (dataSize shr 24 and 0xFF).toByte()

        return header
    }
}