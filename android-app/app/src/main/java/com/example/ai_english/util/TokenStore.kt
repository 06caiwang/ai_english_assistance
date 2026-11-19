package com.example.ai_english.util

import android.content.Context
import android.content.SharedPreferences

/** token 与用户名的本地存储，重启 App 后仍是登录态。 */
object TokenStore {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("english_app", Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) {
            prefs.edit().putString("token", value).apply()
        }

    var nickname: String?
        get() = prefs.getString("nickname", null)
        set(value) {
            prefs.edit().putString("nickname", value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** 401 退出时用同步写，避免"刚清完又被下一帧读到旧值" */
    fun clearSync() {
        prefs.edit().clear().commit()
    }

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()
}
