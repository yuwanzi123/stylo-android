package com.stylo.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "StyloUpload"

data class ReviewItem(
    val filename: String,
    val url: String,        // relative, e.g. /images/{task_id}/item_01_flat.png
    val description: String? = null,
)

data class ClosetItem(
    val id: Int,
    val filename: String,
    val imageUrl: String,
    val description: String,
    val category: String? = null,
)

data class OutfitItem(
    val id: Int,
    val imageUrl: String,
    val category: String?,
    val description: String?,
)

data class ChatOutfitResult(
    val messageId: String,
    val type: String,
    val outfitId: String?,
    val caption: String?,
    val compositeImageUrl: String?,
    val items: List<OutfitItem>,
)

data class ChatHistoryMessage(
    val messageId: String,
    val role: String,
    val type: String,
    val content: String?,
    val outfitId: String?,
    val caption: String?,
    val compositeImageUrl: String?,
    val items: List<OutfitItem>,
)

private fun parseClosetItems(arr: JSONArray?): List<ClosetItem> {
    val items = mutableListOf<ClosetItem>()
    arr ?: return items
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        items.add(
            ClosetItem(
                id          = obj.optInt("id"),
                filename    = obj.optString("filename"),
                imageUrl    = obj.optString("image_url"),
                description = obj.optString("description"),
                category    = obj.optString("category").takeIf { it.isNotEmpty() },
            )
        )
    }
    return items
}

private fun parseOutfitItems(arr: JSONArray?): List<OutfitItem> {
    val items = mutableListOf<OutfitItem>()
    arr ?: return items
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        items.add(
            OutfitItem(
                id = obj.optInt("id"),
                imageUrl = obj.optString("image_url"),
                category = obj.optString("category").takeIf { it.isNotEmpty() },
                description = obj.optString("description").takeIf { it.isNotEmpty() },
            )
        )
    }
    return items
}

// 轻量级单例，在 handleMediaUpload 完成时填充，ReviewResultsScreen 读取
object PendingReviewResults {
    var taskId: String = ""
    var items: List<ReviewItem> = emptyList()
}

// 衣橱列表点击时填充，ItemDetailScreen 读取
object SelectedClosetItem {
    var current: ClosetItem? = null
}

object StyloApi {
    const val BASE_URL = "http://100.97.248.72:8000"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getOrCreateUserId(context: Context): String {
        val prefs = context.getSharedPreferences("stylo_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("user_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("user_id", id).apply()
        }
        return id
    }

    fun sendChatMessage(
        userId: String,
        text: String,
        onResult: (result: ChatOutfitResult?, error: String?) -> Unit,
    ) {
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("text", text)
        }
        val request = Request.Builder()
            .url("$BASE_URL/chat/message")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post { onResult(null, "HTTP ${response.code}: $body") }
                    return
                }
                try {
                    val json = JSONObject(body ?: "{}")
                    val result = ChatOutfitResult(
                        messageId = json.optString("message_id"),
                        type = json.optString("type"),
                        outfitId = json.optString("outfit_id").takeIf { it.isNotEmpty() },
                        caption = json.optString("caption").takeIf { it.isNotEmpty() },
                        compositeImageUrl = json.optString("composite_image_url").takeIf { it.isNotEmpty() },
                        items = parseOutfitItems(json.optJSONArray("items")),
                    )
                    Handler(Looper.getMainLooper()).post { onResult(result, null) }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
                }
            }
        })
    }

    fun fetchChatHistory(
        userId: String,
        onResult: (history: List<ChatHistoryMessage>?, error: String?) -> Unit,
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/chat/history?user_id=$userId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "{}")
                    val arr = json.optJSONArray("messages")
                    val history = mutableListOf<ChatHistoryMessage>()
                    arr?.let {
                        for (i in 0 until it.length()) {
                            val obj = it.getJSONObject(i)
                            history.add(
                                ChatHistoryMessage(
                                    messageId = obj.optString("message_id"),
                                    role = obj.optString("role"),
                                    type = obj.optString("type"),
                                    content = obj.optString("content").takeIf { c -> c.isNotEmpty() },
                                    outfitId = obj.optString("outfit_id").takeIf { c -> c.isNotEmpty() },
                                    caption = obj.optString("caption").takeIf { c -> c.isNotEmpty() },
                                    compositeImageUrl = obj.optString("composite_image_url").takeIf { c -> c.isNotEmpty() },
                                    items = parseOutfitItems(obj.optJSONArray("items")),
                                )
                            )
                        }
                    }
                    Handler(Looper.getMainLooper()).post { onResult(history, null) }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
                }
            }
        })
    }

    fun deleteClosetItem(itemId: Int, onResult: (success: Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/closet/items/$itemId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                Handler(Looper.getMainLooper()).post { onResult(response.isSuccessful) }
            }
        })
    }

    fun fetchClosetItems(onResult: (items: List<ClosetItem>?, error: String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/closet/items")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "{}")
                    val items = parseClosetItems(json.optJSONArray("items"))
                    Handler(Looper.getMainLooper()).post { onResult(items, null) }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
                }
            }
        })
    }

    // Semantic search over closet item descriptions (embedding cosine similarity, backend-side).
    fun fetchClosetSearch(query: String, onResult: (items: List<ClosetItem>?, error: String?) -> Unit) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("$BASE_URL/closet/search?q=$encodedQuery")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "{}")
                    val items = parseClosetItems(json.optJSONArray("items"))
                    Handler(Looper.getMainLooper()).post { onResult(items, null) }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { onResult(null, e.message) }
                }
            }
        })
    }

    fun uploadMedia(
        file: File,
        mimeType: String,
        onResult: (taskId: String?, error: String?) -> Unit,
    ) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null, e.message ?: "Upload failed")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val taskId = body
                    ?.let { JSONObject(it).optString("task_id") }
                    ?.takeIf { it.isNotEmpty() }
                onResult(taskId, if (taskId == null) "Invalid response: $body" else null)
            }
        })
    }
}

fun pollTaskStatus(
    taskId: String,
    onUpdate: (status: String, items: List<ReviewItem>?, error: String?) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        repeat(60) {
            delay(5000)
            val request = Request.Builder()
                .url("${StyloApi.BASE_URL}/task/$taskId")
                .build()
            try {
                val response = StyloApi.client.newCall(request).execute()
                val body = response.body?.string()
                val json = JSONObject(body ?: "{}")
                val status = json.optString("status")

                withContext(Dispatchers.Main) {
                    when (status) {
                        "completed" -> {
                            val imagesArray = json.optJSONArray("images")
                            val items = mutableListOf<ReviewItem>()
                            imagesArray?.let {
                                for (i in 0 until it.length()) {
                                    val obj = it.getJSONObject(i)
                                    items.add(
                                        ReviewItem(
                                            filename = obj.optString("filename"),
                                            url = obj.optString("url"),
                                            description = obj.optString("description")
                                                .takeIf { d -> d.isNotEmpty() },
                                        )
                                    )
                                }
                            }
                            onUpdate("completed", items, null)
                        }
                        "failed" -> onUpdate("failed", null, json.optString("error"))
                        else -> onUpdate("processing", null, null)
                    }
                }

                if (status == "completed" || status == "failed") return@launch
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onUpdate("failed", null, e.message)
                }
                return@launch
            }
        }
    }
}

// taskId 确认后立刻回调 onTaskStarted（主线程），供调用方立即导航到处理中页面
fun handleMediaUpload(
    context: Context,
    file: File,
    mimeType: String,
    onTaskStarted: (() -> Unit)? = null,
) {
    Log.d(TAG, "Uploading ${file.name} ($mimeType, ${file.length()} bytes)")
    StyloApi.uploadMedia(file, mimeType) { taskId, error ->
        if (taskId != null) {
            Log.d(TAG, "Upload accepted, task_id=$taskId — starting tracker")
            ProcessingTaskManager.startTracking(taskId)
            Handler(Looper.getMainLooper()).post {
                onTaskStarted?.invoke()
            }
        } else {
            Log.e(TAG, "Upload failed: $error")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun confirmSelectedItems(
    acceptedItems: List<ReviewItem>,
    taskId: String,
    onResult: (success: Boolean) -> Unit,
) {
    val jsonBody = JSONObject().apply {
        put("items", JSONArray(acceptedItems.map { item ->
            JSONObject().apply {
                put("filename", item.filename)
                put("image_url", item.url)
                put("description", item.description ?: "")
                put("source_task_id", taskId)
            }
        }))
    }

    val request = Request.Builder()
        .url("${StyloApi.BASE_URL}/closet/items")
        .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        .build()

    StyloApi.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post { onResult(false) }
        }
        override fun onResponse(call: Call, response: Response) {
            val success = response.isSuccessful
            Handler(Looper.getMainLooper()).post { onResult(success) }
        }
    })
}
