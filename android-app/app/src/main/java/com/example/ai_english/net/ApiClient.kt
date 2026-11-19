package com.example.ai_english.net

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.example.ai_english.BuildConfig
import com.example.ai_english.model.ApiResult
import com.example.ai_english.ui.LoginActivity
import com.example.ai_english.util.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = TokenStore.token
                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                }
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            // 作文批改要跑四步图，10 秒起步，读超时给足 60 秒
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * 统一包装：异常不崩、业务码非 0 弹提示、401 清 token 跳登录。
     * 每个 Activity 都不用再写 try/catch。
     */
    suspend fun <T> call(activity: Activity, block: suspend () -> ApiResult<T>): T? {
        return try {
            val result = block()
            if (result.code == 0) {
                result.data
            } else {
                if (result.code == 401) {
                    // 先告诉用户为什么被踢回登录页，再清 token
                    toast(activity, result.msg ?: "登录已过期，请重新登录")
                    logout(activity)
                } else {
                    toast(activity, result.msg ?: "请求失败")
                }
                null
            }
        } catch (e: Exception) {
            toast(activity, "网络异常：${e.message ?: "请检查后端是否启动"}")
            null
        }
    }

    /**
     * 401 统一处理：清 token 并回登录页。
     * 必须切到主线程执行 —— 这个方法是从协程里调的，后台线程 startActivity 会静默失败，
     * 表现就是"明明 401 了却还停在原来页面"。
     */
    fun logout(activity: Activity) {
        activity.runOnUiThread {
            TokenStore.clearSync()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
    }

    fun toast(activity: Activity, msg: String) {
        activity.runOnUiThread { Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show() }
    }
}
