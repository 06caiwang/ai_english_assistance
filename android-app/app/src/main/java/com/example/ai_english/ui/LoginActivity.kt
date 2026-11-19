package com.example.ai_english.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.databinding.ActivityLoginBinding
import com.example.ai_english.model.LoginReq
import com.example.ai_english.net.ApiClient
import com.example.ai_english.util.TokenStore
import kotlinx.coroutines.launch

/** 单账号登录：不提供注册入口，账号由后台创建。 */
class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnSubmit.setOnClickListener { submit() }

        // 有本地 token 时先静默校验一次：有效才跳首页。
        // 直接"有 token 就跳"会在 token 失效时形成 401 → 登录页 → 自动跳回 → 401 的死循环。
        if (TokenStore.isLoggedIn()) {
            b.progress.visibility = View.VISIBLE
            lifecycleScope.launch {
                val me = ApiClient.call(this@LoginActivity) { ApiClient.api.me() }
                b.progress.visibility = View.GONE
                if (me != null) {
                    goHome()
                } else {
                    // 校验失败：token 已被 ApiClient 清掉，停在登录页让用户重新登录
                    b.etUsername.text?.clear()
                    b.etPassword.text?.clear()
                }
            }
        }
    }

    private fun submit() {
        val username = b.etUsername.text?.toString()?.trim().orEmpty()
        val password = b.etPassword.text?.toString().orEmpty()

        if (username.isBlank()) {
            ApiClient.toast(this, "请输入用户名")
            return
        }
        if (password.isBlank()) {
            ApiClient.toast(this, "请输入密码")
            return
        }

        b.progress.visibility = View.VISIBLE
        b.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            val resp = ApiClient.call(this@LoginActivity) {
                ApiClient.api.login(LoginReq(username, password))
            }
            b.progress.visibility = View.GONE
            b.btnSubmit.isEnabled = true

            if (resp != null) {
                TokenStore.token = resp.token
                TokenStore.nickname = resp.user?.nickname
                goHome()
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
