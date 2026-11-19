package com.example.ai_english.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.databinding.ActivitySettingsBinding
import com.example.ai_english.model.NicknameReq
import com.example.ai_english.net.ApiClient
import com.example.ai_english.util.TokenStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/** 设置：账号资料 / 音效开关 / 学习数据 / 退出登录 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvVersion.text = packageManager.getPackageInfo(packageName, 0).versionName

        b.swSound.isChecked = prefs.getBoolean("sound", true)
        b.swSound.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("sound", checked).apply()
        }

        b.btnEditNickname.setOnClickListener { editNickname() }
        b.btnLogout.setOnClickListener { logout() }

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            ApiClient.call(this@SettingsActivity) { ApiClient.api.me() }?.let {
                b.tvNickname.text = it.nickname ?: it.username
                b.tvUsername.text = it.username
            }
            ApiClient.call(this@SettingsActivity) { ApiClient.api.stats() }?.let {
                b.tvStats.text = "已学 ${it.learnedCount} 词 · 正确率 ${(it.accuracy * 100).toInt()}% · " +
                        "连续 ${it.streakDays} 天 · 批改 ${it.essayCount} 次"
            }
        }
    }

    private fun editNickname() {
        val input = TextInputEditText(this).apply {
            setText(b.tvNickname.text)
            setSelection(text?.length ?: 0)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("修改昵称")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) {
                    ApiClient.toast(this, "昵称不能为空")
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val resp = ApiClient.call(this@SettingsActivity) {
                        ApiClient.api.updateNickname(NicknameReq(name))
                    }
                    if (resp != null) {
                        b.tvNickname.text = resp.nickname
                        TokenStore.nickname = resp.nickname
                        ApiClient.toast(this@SettingsActivity, "昵称已更新")
                    }
                }
            }
            .show()
    }

    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("退出登录")
            .setMessage("退出后需要重新输入账号密码。")
            .setNegativeButton("取消", null)
            .setPositiveButton("退出") { _, _ ->
                TokenStore.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }
}
