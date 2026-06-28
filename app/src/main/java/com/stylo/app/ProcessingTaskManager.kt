package com.stylo.app

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "ProcessingTaskManager"

object ProcessingTaskManager {

    data class TaskProgress(
        val taskId: String,
        val currentStep: String? = null,  // "processing_media" | "detecting_items" | "generating_images"
        val isActive: Boolean = true,
    )

    var currentTask by mutableStateOf<TaskProgress?>(null)
        private set

    // 设置后由 MainActivity 根级 LaunchedEffect 消费并触发导航，clearTask() 清除
    var completedTaskId by mutableStateOf<String?>(null)
        private set

    private var pollingJob: Job? = null

    fun startTracking(taskId: String) {
        currentTask = TaskProgress(taskId = taskId)
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
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
                    val step = json.optString("current_step").takeIf { it.isNotEmpty() }

                    withContext(Dispatchers.Main) {
                        currentTask = TaskProgress(
                            taskId = taskId,
                            currentStep = step,
                            isActive = status == "processing",
                        )
                        Log.d(TAG, "[$taskId] status=$status  step=$step")
                    }

                    when (status) {
                        "completed" -> {
                            val imagesArray = json.optJSONArray("images")
                            val items = buildList {
                                for (i in 0 until (imagesArray?.length() ?: 0)) {
                                    val obj = imagesArray!!.getJSONObject(i)
                                    add(ReviewItem(
                                        filename = obj.optString("filename"),
                                        url = obj.optString("url"),
                                        description = obj.optString("description")
                                            .takeIf { it.isNotEmpty() },
                                    ))
                                }
                            }
                            withContext(Dispatchers.Main) {
                                PendingReviewResults.taskId = taskId
                                PendingReviewResults.items = items
                                completedTaskId = taskId  // 触发 MainActivity 根级导航
                                Log.d(TAG, "[$taskId] COMPLETED — ${items.size} item(s) ready")
                            }
                            return@launch
                        }
                        "failed" -> {
                            val error = json.optString("error")
                            withContext(Dispatchers.Main) {
                                Log.e(TAG, "[$taskId] FAILED — $error")
                                clearTask()
                            }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "[$taskId] poll error: ${e.message}")
                    }
                    return@launch
                }
            }
        }
    }

    fun clearTask() {
        pollingJob?.cancel()
        pollingJob = null
        currentTask = null
        completedTaskId = null
    }
}
