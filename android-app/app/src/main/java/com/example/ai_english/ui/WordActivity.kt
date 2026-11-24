package com.example.ai_english.ui

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
 * 认识（自评） → 缺空默写（**直接在空格里填字母**） → 完整默写（全空格）。
 */
class WordActivity : AppCompatActivity() {

    private lateinit var b: ActivityWordBinding
    private val words = mutableListOf<WordItem>()
    private var wi = 0
    private var mi = 0
    private var coin = 0
    private var answered = false
    private var mask = ""
    private val blankInputs = mutableListOf<EditText>()
    private val letterViews = mutableListOf<TextView>()

    private val modes: List<String>
        get() = words.getOrNull(wi)?.modes?.takeIf { it.isNotEmpty() } ?: listOf("know")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityWordBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }
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

        val totalLevels = words.sumOf { it.modes?.size ?: 1 }
        val doneLevels = words.take(wi).sumOf { it.modes?.size ?: 1 } + mi
        b.tvProgress.text = "第 ${doneLevels + 1} 关 / 共 $totalLevels 关"
        b.progressBar.max = totalLevels
        b.progressBar.progress = doneLevels + 1

        b.tvPhonetic.text = w.phonetic.orEmpty()
        b.tvMeaning.text = w.meaning
        b.tvCoin.text = "🪙 $coin"
        b.tvFeedback.visibility = View.GONE
        b.tvExample.visibility = View.GONE

        when (mode) {
            "know" -> {
                b.tvMode.text = if (w.isNew) "新词 · 认识吗" else "复习 · 认识吗"
                b.svBlanks.visibility = View.GONE
                b.btnSubmit.visibility = View.GONE
                b.llKnow.visibility = View.VISIBLE
                blankInputs.clear()
                letterViews.clear()
            }
            else -> {
                b.tvMode.text = if (mode == "fill") "缺空默写" else "完整默写"
                b.svBlanks.visibility = View.VISIBLE
                b.btnSubmit.visibility = View.VISIBLE
                b.btnSubmit.text = "提交"
                b.llKnow.visibility = View.GONE
                buildBlanks(w.spelling, if (mode == "fill") w.fillHint else null)
            }
        }
    }

    /**
     * 把单词拆成"字母 + 空位"：已知字母是静态 TextView，要填的位置是单字符 EditText。
     * 用户直接往空格里敲，不用把整个单词再拼一遍。
     */
    private fun buildBlanks(spelling: String, hint: String?) {
        b.llBlanks.removeAllViews()
        blankInputs.clear()
        letterViews.clear()

        // 后端提示与单词长度对不上时，退化为全遮（完整默写）
        mask = if (!hint.isNullOrBlank() && hint.length == spelling.length) hint
        else "_".repeat(spelling.length)
        if (hint.isNullOrBlank()) mask = "_".repeat(spelling.length)

        val boxWidth = dp(if (spelling.length > 9) 24 else 28)
        val boxHeight = dp(40)

        mask.forEachIndexed { index, ch ->
            if (ch == '_') {
                val et = EditText(this).apply {
                    layoutParams = LinearLayout.LayoutParams(boxWidth, boxHeight).apply {
                        marginEnd = dp(2)
                    }
                    background = ContextCompat.getDrawable(this@WordActivity, R.drawable.bg_blank_box)
                    gravity = Gravity.CENTER
                    setTextColor(getColor(R.color.cozy_text))
                    textSize = 18f
                    isSingleLine = true
                    setPadding(0, 0, 0, dp(2))
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    imeOptions = EditorInfo.IME_ACTION_NEXT
                    filters = arrayOf(InputFilter.LengthFilter(1))
                }
                et.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (!s.isNullOrEmpty()) focusNext(et)
                    }
                })
                // 空位按退格回到上一个空位，符合纸面填写的直觉
                et.setOnKeyListener { v, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN
                        && (v as EditText).text.isNullOrEmpty()
                    ) {
                        focusPrev(et, clearPrev = true)
                        true
                    } else false
                }
                blankInputs.add(et)
                b.llBlanks.addView(et)
            } else {
                val tv = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, boxHeight
                    ).apply { marginEnd = dp(1) }
                    text = ch.toString()
                    gravity = Gravity.CENTER
                    setTextColor(getColor(R.color.cozy_text))
                    textSize = 20f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                letterViews.add(tv)
                b.llBlanks.addView(tv)
            }
        }

        blankInputs.firstOrNull()?.requestFocus()
    }

    private fun focusNext(from: EditText) {
        val i = blankInputs.indexOf(from)
        if (i >= 0 && i < blankInputs.size - 1) {
            blankInputs[i + 1].requestFocus()
        }
    }

    private fun focusPrev(from: EditText, clearPrev: Boolean) {
        val i = blankInputs.indexOf(from)
        if (i > 0) {
            val prev = blankInputs[i - 1]
            prev.requestFocus()
            if (clearPrev) prev.setText("")
        }
    }

    /** 把已填的字母拼回整词，交给服务端判题（大小写不敏感） */
    private fun assemble(): String? {
        if (blankInputs.isEmpty()) return null
        val sb = StringBuilder()
        var bi = 0
        for (ch in mask) {
            if (ch == '_') {
                val t = blankInputs[bi].text?.toString().orEmpty()
                if (t.isBlank()) return null
                sb.append(t)
                bi++
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun submit() {
        val assembled = assemble()
        if (assembled == null) {
            ApiClient.toast(this, "还有空没填")
            return
        }
        send(AnswerReq(wordId = words[wi].wordId, mode = modes[mi], answer = assembled))
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

            // 答错就把正确字母填进空格并标红，让用户直接看到差在哪
            if (!resp.correct && blankInputs.isNotEmpty()) fillCorrect(resp.correctAnswer)
            paintBlanks(resp.correct)

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
                "拼写不对，正确写法：${resp.correctAnswer}\n饥饿值 -2，现在 ${resp.hunger}"
            }
            b.tvExample.text = words[wi].example.orEmpty()
            b.tvExample.visibility =
                if (words[wi].example.isNullOrBlank()) View.GONE else View.VISIBLE

            blankInputs.forEach { it.isEnabled = false }
            b.llKnow.visibility = View.GONE
            b.btnSubmit.visibility = View.VISIBLE
            b.btnSubmit.text = "继续"
        }
    }

    /** 把正确答案逐字母写进空格，用于答错时的红字对照 */
    private fun fillCorrect(correct: String) {
        if (correct.length != mask.length) return
        var bi = 0
        for (ch in mask) {
            if (ch == '_') {
                blankInputs.getOrNull(bi)?.setText(correct[bi].toString())
                bi++
            }
        }
    }

    private fun paintBlanks(ok: Boolean) {
        val bg = ContextCompat.getDrawable(
            this, if (ok) R.drawable.bg_blank_box_ok else R.drawable.bg_blank_box_bad
        )
        val color = getColor(if (ok) R.color.cozy_green else R.color.danger)
        blankInputs.forEach {
            it.background = bg
            it.setTextColor(color)
        }
        letterViews.forEach { it.setTextColor(color) }
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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
