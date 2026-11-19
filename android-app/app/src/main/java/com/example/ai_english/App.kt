package com.example.ai_english

import android.app.Application
import com.example.ai_english.util.TokenStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
    }
}
