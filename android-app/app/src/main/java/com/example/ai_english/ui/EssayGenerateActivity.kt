package com.example.ai_english.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.databinding.ActivityEssayGenerateBinding
import com.example.ai_english.model.GenerateReq
import com.example.ai_english.net.ApiClient
import kotlinx.coroutines.launch

/** 作文生成页：便签式设置 + 便签式结果。 */
class EssayGenerateActivity : AppCompatActivity() {

    private lateinit var b: ActivityEssayGenerateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEssayGenerateBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }
        b.groupWords.check(b.btnW120.id)
        b.groupLevel.check(b.btnLSenior.id)
        b.btnGenerate.setOnClickListener { generate() }
    }

    private fun generate() {
        val topic = b.etTopic.text?.toString()?.trim().orEmpty()
        if (topic.isBlank()) {
            ApiClient.toast(this, "先写个主题")
            return
        }
        val words = when (b.groupWords.checkedButtonId) {
            b.btnW80.id -> 80
            b.btnW150.id -> 150
            else -> 120
        }
        val level = when (b.groupLevel.checkedButtonId) {
            b.btnLJunior.id -> "junior"
            b.btnLCet4.id -> "cet4"
            else -> "senior"
        }

        b.progress.visibility = View.VISIBLE
        b.btnGenerate.isEnabled = false
        b.noteResult.visibility = View.GONE

        lifecycleScope.launch {
            val resp = ApiClient.call(this@EssayGenerateActivity) {
                ApiClient.api.generateEssay(GenerateReq(topic, "narrative", words, level))
            }
            b.progress.visibility = View.GONE
            b.btnGenerate.isEnabled = true
            if (resp == null) return@launch

            b.tvTitle.text = resp.title.ifBlank { topic }
            b.tvMeta.text = "${resp.wordCount} 词 · ${levelText(level)}"
            b.tvEssay.text = resp.essay
            b.noteResult.visibility = View.VISIBLE
        }
    }

    private fun levelText(level: String) = when (level) {
        "junior" -> "初中"
        "cet4" -> "四级"
        else -> "高中"
    }
}
