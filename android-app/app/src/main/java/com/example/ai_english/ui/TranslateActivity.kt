package com.example.ai_english.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.databinding.ActivityTranslateBinding
import com.example.ai_english.model.TranslateReq
import com.example.ai_english.net.ApiClient
import kotlinx.coroutines.launch

/** 单词翻译页：上方输入、下方结果卡，风格参考常见翻译软件。 */
class TranslateActivity : AppCompatActivity() {

    private lateinit var b: ActivityTranslateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTranslateBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }
        b.btnTranslate.setOnClickListener { translate() }

        // 输入框一有内容就允许翻译，空白时按钮置灰，减少一次"点了没反应"的疑惑
        b.btnTranslate.isEnabled = false
        b.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun afterTextChanged(s: Editable?) {
                b.btnTranslate.isEnabled = !s.isNullOrBlank()
            }
        })

        loadRecent()
    }

    private fun translate() {
        val text = b.etInput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            ApiClient.toast(this, "先输入要翻译的内容")
            return
        }
        b.progress.visibility = View.VISIBLE
        b.btnTranslate.isEnabled = false
        b.cardResult.visibility = View.GONE

        lifecycleScope.launch {
            val resp = ApiClient.call(this@TranslateActivity) {
                ApiClient.api.translate(TranslateReq(text))
            }
            b.progress.visibility = View.GONE
            b.btnTranslate.isEnabled = true
            if (resp == null) return@launch

            b.tvWord.text = text
            b.tvPhonetic.text = resp.phonetic.orEmpty()
            b.tvPhonetic.visibility = if (resp.phonetic.isNullOrBlank()) View.GONE else View.VISIBLE
            b.tvPos.text = resp.pos.orEmpty()
            b.tvPos.visibility = if (resp.pos.isNullOrBlank()) View.GONE else View.VISIBLE
            b.tvMeaning.text = resp.meaning

            val hasExample = !resp.example.isNullOrBlank()
            b.dividerExample.visibility = if (hasExample) View.VISIBLE else View.GONE
            b.tvExample.text = resp.example.orEmpty()
            b.tvExample.visibility = if (hasExample) View.VISIBLE else View.GONE

            b.cardResult.visibility = View.VISIBLE
            loadRecent()
        }
    }

    /** 最近查过的词做成可点的一行行，点一下直接回填并重查 */
    private fun loadRecent() {
        lifecycleScope.launch {
            val page = ApiClient.call(this@TranslateActivity) {
                ApiClient.api.historyTranslate(1, 5)
            } ?: return@launch
            val items = page.items.orEmpty().filter { it.text != b.etInput.text?.toString()?.trim() }
            b.llRecent.removeAllViews()
            b.tvRecentTitle.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE

            items.forEach { item ->
                val row = layoutInflater.inflate(com.example.ai_english.R.layout.item_recent_word, b.llRecent, false)
                val word = row.findViewById<android.widget.TextView>(com.example.ai_english.R.id.tvRecentWord)
                val meaning = row.findViewById<android.widget.TextView>(com.example.ai_english.R.id.tvRecentMeaning)
                word.text = item.text
                meaning.text = item.meaning
                row.setOnClickListener {
                    b.etInput.setText(item.text)
                    b.etInput.setSelection(item.text.length)
                    translate()
                }
                b.llRecent.addView(row)
            }
        }
    }
}
