package com.example.ai_english.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.databinding.ActivityEssayGradeBinding
import com.example.ai_english.model.EssayReq
import com.example.ai_english.net.ApiClient
import kotlinx.coroutines.launch

/** 作文打分页：便签式输入 + 便签式结果，不再用弹窗。 */
class EssayGradeActivity : AppCompatActivity() {

    private lateinit var b: ActivityEssayGradeBinding

    private val dimLabels = linkedMapOf(
        "content" to "内容", "grammar" to "语法",
        "vocabulary" to "词汇", "structure" to "结构"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEssayGradeBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }
        b.btnGrade.setOnClickListener { grade() }

        b.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun afterTextChanged(s: Editable?) {
                b.tvWordCount.text = "${countWords(s?.toString().orEmpty())} 词"
            }
        })
    }

    /** 和文末统计口径保持一致：按空白切分 */
    private fun countWords(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    private fun grade() {
        val content = b.etContent.text?.toString()?.trim().orEmpty()
        if (content.isBlank()) {
            ApiClient.toast(this, "先把作文贴进来")
            return
        }
        b.progress.visibility = View.VISIBLE
        b.btnGrade.isEnabled = false
        b.noteResult.visibility = View.GONE

        lifecycleScope.launch {
            val resp = ApiClient.call(this@EssayGradeActivity) {
                ApiClient.api.essay(EssayReq(content))
            }
            b.progress.visibility = View.GONE
            b.btnGrade.isEnabled = true
            if (resp == null) return@launch

            b.tvScore.text = resp.score.toString()
            b.tvLevel.text = buildString {
                append(resp.level.orEmpty())
                if (resp.wordCount > 0) append(" · ${resp.wordCount} 词")
            }

            // 四维按固定顺序展示，缺哪个就跳过
            val dims = resp.dimensions.orEmpty()
            b.tvDims.text = dimLabels.entries
                .filter { dims.containsKey(it.key) }
                .joinToString("　") { "${it.value} ${dims[it.key]}" }

            val deductions = resp.deductions.orEmpty()
            if (deductions.isEmpty()) {
                b.tvDeductions.visibility = View.GONE
            } else {
                b.tvDeductions.visibility = View.VISIBLE
                b.tvDeductions.text = "扣分\n" + deductions.joinToString("\n") {
                    "· [${it.points}] ${it.sentence} → ${it.fix}"
                }
            }

            val suggestions = resp.suggestions.orEmpty()
            b.tvSuggestions.text = if (suggestions.isEmpty()) "" else
                "建议\n" + suggestions.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")

            b.noteResult.visibility = View.VISIBLE
        }
    }
}
