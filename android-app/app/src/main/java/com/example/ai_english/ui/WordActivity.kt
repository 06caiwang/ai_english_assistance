package com.example.ai_english.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.R
import com.example.ai_english.databinding.ActivityWordBinding
import com.example.ai_english.model.AnswerReq
import com.example.ai_english.model.WordItem
import com.example.ai_english.net.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 背单词：每个词按复习阶段走 1–3 关。
 * 认识/不认识（自评） → 缺空默写（给提示，填整词） → 完整默写（只给释义）。
 */
class WordActivity : AppCompatActivity() {

    private lateinit var b: ActivityWordBinding
    private val words = mutableListOf<WordItem>()
    private var wi = 0          // 第几个词
    private var mi = 0          // 这个词的第几关
    private var coin = 0
    private var answered = false

    private val modes: List<String>
        get() = words.getOrNull(wi)?.modes?.takeIf { it.isNotEmpty() } ?: listOf("know")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityWordBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnSubmit.setOnClickListener { if (answered) next() else submit() }
        b.btnKnow.setOnClickListener { answerKnow(true) }
        b.btnDontKnow.setOnClickListener { answerKnow(false) }

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            ApiClient.call(this@WordActivity) { ApiClient.api.me() }?.let {
                coin = it.coin
                b.tvCoin.text = "🪙 $coin"
            }
            val list = ApiClient.call(this@WordActivity) { ApiClient.api.todayWords() }
            if (list.isNullOrEmpty()) {
                ApiClient.toast(this@WordActivity, "今天没有需要学习的单词")
                finish()
                return@launch
            }
            words.clear()
            words.addAll(list)
            wi = 0
            mi = 0
            render()
        }
    }

    private fun render() {
        if (wi >= words.size) {
            finishWithSummary()
            return
        }
        val w = words[wi]
        val mode = modes[mi]
        answered = false

        // 进度按"关卡数"算，不是按词数
        val totalLevels = words.sumOf { it.modes?.size ?: 1 }
        val doneLevels = words.take(wi).sumOf { it.modes?.size ?: 1 } + mi
        b.tvProgress.text = "第 ${doneLevels + 1} 关 / 共 $totalLevels 关"
        b.progressBar.max = totalLevels
        b.progressBar.progress = doneLevels + 1

        b.tvPhonetic.text = w.phonetic ?: ""
        b.tvMeaning.text = w.meaning
        b.tvCoin.text = "🪙 $coin"
        b.tvFeedback.visibility = View.GONE
        b.tvExample.visibility = View.GONE

        when (mode) {
            "know" -> {
                b.tvMode.text = if (w.isNew) "新词 · 认识吗" else "复习 · 认识吗"
                b.tvHint.visibility = View.GONE
                b.layoutAnswer.visibility = View.GONE
                b.btnSubmit.visibility = View.GONE
                b.llKnow.visibility = View.VISIBLE
            }
            "fill" -> {
                b.tvMode.text = "缺空默写"
                b.tvHint.text = spaced(w.fillHint)
                b.tvHint.visibility = View.VISIBLE
                b.layoutAnswer.visibility = View.VISIBLE
                b.btnSubmit.visibility = View.VISIBLE
                b.btnSubmit.text = "提交"
                b.llKnow.visibility = View.GONE
                b.etAnswer.setText("")
                b.etAnswer.isEnabled = true
                b.layoutAnswer.isEnabled = true
            }
            else -> {
                b.tvMode.text = "完整默写"
                b.tvHint.text = spaced("_".repeat(w.spelling.length))
                b.tvHint.visibility = View.VISIBLE
                b.layoutAnswer.visibility = View.VISIBLE
                b.btnSubmit.visibility = View.VISIBLE
                b.btnSubmit.text = "提交"
                b.llKnow.visibility = View.GONE
                b.etAnswer.setText("")
                b.etAnswer.isEnabled = true
                b.layoutAnswer.isEnabled = true
            }
        }
    }

    /** 字母之间加空格，缺空位置更好读 */
    private fun spaced(s: String) = s.toCharArray().joinToString(" ")

    private fun submit() {
        val mode = modes[mi]
        val typed = b.etAnswer.text?.toString()?.trim().orEmpty()
        if (typed.isBlank()) {
            ApiClient.toast(this, "先写点什么再提交")
            return
        }
        send(AnswerReq(wordId = words[wi].wordId, mode = mode, answer = typed))
    }

    private fun answerKnow(know: Boolean) {
        send(AnswerReq(wordId = words[wi].wordId, mode = "know", correct = know))
    }

    private fun send(req: AnswerReq) {
        b.btnSubmit.isEnabled = false
        b.btnKnow.isEnabled = false
        b.btnDontKnow.isEnabled = false

        lifecycleScope.launch {
            val resp = ApiClient.call(this@WordActivity) { ApiClient.api.answer(req) }
            b.btnSubmit.isEnabled = true
            b.btnKnow.isEnabled = true
            b.btnDontKnow.isEnabled = true
            if (resp == null) return@launch

            coin = resp.coin
            b.tvCoin.text = "🪙 $coin"
            answered = true

            // 结果反馈：对错 + 正确答案 + 收益 + 饥饿消耗
            b.tvFeedback.visibility = View.VISIBLE
            b.tvFeedback.setTextColor(
                getColor(if (resp.correct) R.color.cozy_green else R.color.danger)
            )
            val gain = buildString {
                if (resp.coinGained > 0) append("金币 +${resp.coinGained}  ")
                if (resp.expGained > 0) append("经验 +${resp.expGained}")
                if (resp.expGained == 0 && resp.correct) append("（饿得没力气，没有经验）")
            }
            b.tvFeedback.text = if (resp.correct) {
                "答对了  $gain\n饥饿值 -2，现在 ${resp.hunger}"
            } else {
                "答错了，正确答案：${resp.correctAnswer}\n饥饿值 -2，现在 ${resp.hunger}"
            }
            b.tvExample.text = words[wi].example ?: ""
            b.tvExample.visibility = if (words[wi].example.isNullOrBlank()) View.GONE else View.VISIBLE

            // 把输入区锁住，按钮变成"继续"
            b.etAnswer.isEnabled = false
            b.layoutAnswer.isEnabled = false
            b.llKnow.visibility = View.GONE
            b.btnSubmit.visibility = View.VISIBLE
            b.btnSubmit.text = "继续"
        }
    }

    private fun next() {
        answered = false
        mi++
        if (mi >= modes.size) {
            mi = 0
            wi++
        }
        render()
    }

    private fun finishWithSummary() {
        MaterialAlertDialogBuilder(this)
            .setTitle("今天学完了")
            .setMessage("好样的，宠物也吃饱了。\n当前金币 $coin。\n答对的词会按 1/3/7/15 天的节奏安排复习。")
            .setPositiveButton("回首页") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
