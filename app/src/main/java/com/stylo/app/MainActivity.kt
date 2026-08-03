package com.stylo.app

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StyloTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                var hasSeenCaptureGuide by remember { mutableStateOf(false) }
                var showCaptureSheet by remember { mutableStateOf(false) }
                var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
                var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
                var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
                var pendingVideoFile by remember { mutableStateOf<File?>(null) }
                var pendingRetakeUri by remember { mutableStateOf<Uri?>(null) }

                // 根级监听：任务完成后无论当前在哪个页面都能触发跳转
                val completedTaskId = ProcessingTaskManager.completedTaskId
                LaunchedEffect(completedTaskId) {
                    if (completedTaskId != null) {
                        navController.navigate("review_results") {
                            popUpTo("processing_screen") { inclusive = true }
                        }
                        ProcessingTaskManager.clearTask()
                    }
                }

                val photoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) {
                        Log.d("MainActivity", "Photo captured: $pendingPhotoUri")
                        val file = pendingPhotoFile
                        if (file != null && file.exists()) {
                            handleMediaUpload(context, file, "image/jpeg") {
                                navController.navigate("processing_screen")
                            }
                        }
                    }
                    pendingPhotoUri = null
                    pendingPhotoFile = null
                }

                val retakePhotoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) Log.d("MainActivity", "Retake photo captured: $pendingRetakeUri")
                    // TODO: 处理 retake 后的新照片（更新当前图片或加入版本历史）
                    pendingRetakeUri = null
                }

                val videoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CaptureVideo()
                ) { success ->
                    if (success) {
                        Log.d("MainActivity", "Video captured: $pendingVideoUri")
                        val file = pendingVideoFile
                        if (file != null && file.exists()) {
                            handleMediaUpload(context, file, "video/mp4") {
                                navController.navigate("processing_screen")
                            }
                        }
                    }
                    pendingVideoUri = null
                    pendingVideoFile = null
                }

                fun createCameraFile(isVideo: Boolean): Pair<File, Uri> {
                    val ext = if (isVideo) ".mp4" else ".jpg"
                    val file = File(context.cacheDir, "camera/stylo_${System.currentTimeMillis()}$ext")
                    file.parentFile?.mkdirs()
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    return Pair(file, uri)
                }

                val launchCamera = { showCaptureSheet = true }

                val launchRetakeCamera: () -> Unit = {
                    val (_, uri) = createCameraFile(false)
                    pendingRetakeUri = uri
                    retakePhotoLauncher.launch(uri)
                }

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            onSkip = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        HomeScreen(
                            isLoggedIn = false,
                            onNavigateToLogin = { navController.navigate("login") },
                            onNavigateToAccount = { navController.navigate("account") },
                            onNavigateToCloset = { navController.navigate("closet") },
                            onNavigateToAddClothes = { navController.navigate("add_clothes") },
                            onNavigateToOOTDFeed = { navController.navigate("ootd_feed") }
                        )
                    }
                    composable("account") {
                        AccountScreen(
                            isLoggedIn = false,
                            onBack = { navController.navigateUp() },
                            onSignIn = { navController.navigate("login") }
                        )
                    }
                    composable("ootd_feed") {
                        OOTDFeedScreen(onBack = { navController.navigateUp() })
                    }
                    composable("closet") {
                        ClosetListScreen(
                            onBack = { navController.navigateUp() },
                            onItemClick = { item ->
                                SelectedClosetItem.current = item
                                navController.navigate("item_detail")
                            }
                        )
                    }
                    composable("item_detail") {
                        ItemDetailScreen(
                            onBack = { navController.navigateUp() },
                            onRetakePhoto = launchRetakeCamera
                        )
                    }
                    composable("add_clothes") {
                        AddClothesScreen(
                            hasSeenCaptureGuide = hasSeenCaptureGuide,
                            onBack = { navController.navigateUp() },
                            onNavigateToShootingGuide = { navController.navigate("shooting_guide") },
                            onLaunchCamera = launchCamera,
                            onUploadCompleted = { navController.navigate("processing_screen") },
                        )
                    }
                    composable("processing_screen") {
                        ProcessingScreen()
                    }
                    composable("review_results") {
                        ReviewResultsScreen(
                            onBack = { navController.navigateUp() },
                            onConfirmed = {
                                navController.navigate("closet") {
                                    popUpTo("review_results") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("shooting_guide") {
                        ShootingGuideScreen(
                            onFinish = {
                                hasSeenCaptureGuide = true
                                navController.navigateUp()
                                launchCamera()
                            },
                            onBack = { navController.navigateUp() }
                        )
                    }
                }

                if (showCaptureSheet) {
                    CaptureOptionsSheet(
                        onDismiss = { showCaptureSheet = false },
                        onTakePhoto = {
                            showCaptureSheet = false
                            val (file, uri) = createCameraFile(false)
                            pendingPhotoUri = uri
                            pendingPhotoFile = file
                            photoLauncher.launch(uri)
                        },
                        onRecordVideo = {
                            showCaptureSheet = false
                            val (file, uri) = createCameraFile(true)
                            pendingVideoUri = uri
                            pendingVideoFile = file
                            videoLauncher.launch(uri)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var statusText by remember { mutableStateOf("Select a video to get started") }
    var isUploading by remember { mutableStateOf(false) }
    var imageBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            statusText = "Uploading video..."
            isUploading = true
            imageBitmaps = emptyList()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "upload_video.mp4")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(300, TimeUnit.SECONDS)
                        .readTimeout(300, TimeUnit.SECONDS)
                        .build()

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file", "video.mp4",
                            tempFile.asRequestBody("video/mp4".toMediaType())
                        )
                        .build()

                    val request = Request.Builder()
                        .url("${StyloApi.BASE_URL}/upload-video")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val json = JSONObject(responseBody)
                        val imagesArray = json.getJSONArray("images")
                        val bitmaps = mutableListOf<android.graphics.Bitmap>()

                        for (i in 0 until imagesArray.length()) {
                            val item = imagesArray.getJSONObject(i)
                            val b64 = item.getString("b64")
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) bitmaps.add(bitmap)
                        }

                        withContext(Dispatchers.Main) {
                            imageBitmaps = bitmaps
                            statusText = "Found ${bitmaps.size} items"
                            isUploading = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            statusText = "Upload failed: ${response.code}"
                            isUploading = false
                        }
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Stylo", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 16.dp))

        Button(
            onClick = { if (!isUploading) videoPicker.launch("video/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading
        ) {
            Text(if (isUploading) "Processing..." else "Add Clothes from Video")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = statusText, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (imageBitmaps.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageBitmaps) { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Clothing item",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureOptionsSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            TextButton(
                onClick = onTakePhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("📷  Take a photo", modifier = Modifier.fillMaxWidth())
            }
            TextButton(
                onClick = onRecordVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("🎥  Record a video", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
