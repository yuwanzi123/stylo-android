package com.stylo.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stylo.app.ui.theme.StyloTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StyloTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var statusText by remember { mutableStateOf("Select a video to get started") }
    var isUploading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            statusText = "Uploading video..."
            isUploading = true

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 把 Uri 复制成临时文件
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "upload_video.mp4")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    // 上传到后端
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(120, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .build()

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "video.mp4",
                            tempFile.asRequestBody("video/mp4".toMediaType())
                        )
                        .build()

                    val request = Request.Builder()
                        .url("http://192.168.1.128:8000/upload-video")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            statusText = "Done! Response: $responseBody"
                        } else {
                            statusText = "Upload failed: ${response.code}"
                        }
                        isUploading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        statusText = "Error: ${e.message}"
                        isUploading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Stylo",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { if (!isUploading) videoPicker.launch("video/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading
        ) {
            Text(if (isUploading) "Processing..." else "Add Cloth from Video")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}