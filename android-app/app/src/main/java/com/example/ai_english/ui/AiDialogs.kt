package com.example.ai_english.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.databinding.DialogAiBinding
import com.example.ai_english.model.EssayReq
import com.example.ai_english.model.TranslateReq
import com.example.ai_english.net.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 翻译与作文打分共用一个弹层，学习中心两处调用。 */
object AiDialogs {

    fun translate(activity: AppCompatActivity) {
        open(activity, essay = false)
    }

    fun essay(activity: AppCompatActivity) {
        open(activity, essay = true)
    }

    private fun open(activity: AppCompatActivity, essay: Boolean) {
        val b = DialogAiBinding.inflate(activity.layoutInflater)
        b.etInput.hint = if (essay) "粘贴一段英语作文（80 词以上）" else "输入一个英文单词，如 resilient"
        b.btnGo.text = if (essay) "开始批改" else "翻译"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(if (essay) "作文打分（LangGraph 四步图）" else "单词翻译（LangChain Chain）")
            .setView(b.root)
            .setNegativeButton("关闭", null)
            .create()

        b.btnGo.setOnClickListener {
            val text = b.etInput.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                ApiClient.toast(activity, "请先输入内容")
                return@setOnClickListener
            }
            b.progress.visibility = View.VISIBLE
            b.btnGo.isEnabled = false
            b.tvResult.visibility = View.GONE

            activity.lifecycleScope.launch {
                if (essay) {
                    val resp = ApiClient.call(activity) { ApiClient.api.essay(EssayReq(text)) }
                    b.progress.visibility = View.GONE
                    b.btnGo.isEnabled = true
                    if (resp != null) {
                        val dims = resp.dimensions?.entries
                            ?.joinToString("  ") { "${it.key} ${it.value}" }.orEmpty()
                        val ded = resp.deductions.orEmpty().map {
                            "· [${it.points}] ${it.sentence} → ${it.fix}"
                        }.joinToString("\n").ifBlank { "· 无明显扣分项" }
                        val sugg = resp.suggestions?.mapIndexed { i, s -> "${i + 1}. $s" }
                            ?.joinToString("\n").orEmpty()
                        b.tvResult.text = buildString {
                            append("总分：${resp.score}（${resp.level ?: ""}）\n")
                            append("词数：${resp.wordCount}  ${resp.lengthNote ?: ""}\n")
                            append("维度：$dims（满分 30/25/25/20）\n\n")
                            append("扣分明细：\n$ded\n\n")
                            append("建议：\n$sugg\n\n")
                            append("节点轨迹：${resp.trace?.joinToString(" → ").orEmpty()}\n")
                            append("模型：${resp.source}")
                        }
                        b.tvResult.visibility = View.VISIBLE
                    }
                } else {
                    val resp = ApiClient.call(activity) { ApiClient.api.translate(TranslateReq(text)) }
                    b.progress.visibility = View.GONE
                    b.btnGo.isEnabled = true
                    if (resp != null) {
                        b.tvResult.text = buildString {
                            append("${resp.meaning}\n\n")
                            if (!resp.phonetic.isNullOrBlank()) append("音标：${resp.phonetic}\n")
                            if (!resp.pos.isNullOrBlank()) append("词性：${resp.pos}\n")
                            if (!resp.example.isNullOrBlank()) append("例句：${resp.example}\n")
                            append("\n模型：${resp.source}")
                        }
                        b.tvResult.visibility = View.VISIBLE
                    }
                }
            }
        }
        dialog.show()
    }

    /** 作文生成：主题 + 词数 + 难度，走第二张 LangGraph 图 */
    fun generate(activity: AppCompatActivity) {
        val b = com.example.ai_english.databinding.DialogGenerateBinding.inflate(activity.layoutInflater)
        b.groupWords.check(b.btnW120.id)
        b.groupLevel.check(b.btnLSenior.id)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("作文生成（LangGraph 生成图）")
            .setView(b.root)
            .setNegativeButton("关闭", null)
            .create()

        b.btnGenerate.setOnClickListener {
            val topic = b.etTopic.text?.toString()?.trim().orEmpty()
            if (topic.isBlank()) {
                ApiClient.toast(activity, "先写个主题")
                return@setOnClickListener
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
            b.tvResult.visibility = View.GONE

            activity.lifecycleScope.launch {
                val resp = ApiClient.call(activity) {
                    ApiClient.api.generateEssay(
                        com.example.ai_english.model.GenerateReq(topic, "narrative", words, level)
                    )
                }
                b.progress.visibility = View.GONE
                b.btnGenerate.isEnabled = true
                if (resp != null) {
                    val outline = resp.outline.orEmpty().mapIndexed { i, s -> "${i + 1}. $s" }
                        .joinToString("\n")
                    val kws = resp.keyWords.orEmpty().joinToString("  ") { "${it.word} ${it.meaning}" }
                    val good = resp.goodSentences.orEmpty().joinToString("\n")
                    b.tvResult.text = buildString {
                        append("${resp.essay}\n\n")
                        append("—— 共 ${resp.wordCount} 词 ——\n\n")
                        append("大纲：\n$outline\n\n")
                        if (kws.isNotBlank()) append("生词：$kws\n\n")
                        if (good.isNotBlank()) append("亮点句：\n$good\n\n")
                        append("节点轨迹：${resp.trace?.joinToString(" → ").orEmpty()}")
                    }
                    b.tvResult.visibility = View.VISIBLE
                }
            }
        }
        dialog.show()
    }
}
