package com.example.ai_english.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ai_english.databinding.ActivityHistoryBinding
import com.example.ai_english.databinding.DialogHistoryDetailBinding
import com.example.ai_english.databinding.ItemHistoryEssayBinding
import com.example.ai_english.databinding.ItemHistoryGenerateBinding
import com.example.ai_english.databinding.ItemHistoryTranslateBinding
import com.example.ai_english.model.EssayRecordItem
import com.example.ai_english.model.GenerateRecordItem
import com.example.ai_english.model.TranslateRecordItem
import com.example.ai_english.net.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.launch

/** 历史记录：单词查询 / 作文打分 / 作文生成 三个 Tab。 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var b: ActivityHistoryBinding
    private var tab = 0

    private val translateAdapter = TranslateHistoryAdapter { showTranslate(it) }
    private val essayAdapter = EssayHistoryAdapter { showEssay(it) }
    private val generateAdapter = GenerateHistoryAdapter { showGenerate(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvHistory.layoutManager = LinearLayoutManager(this)

        b.tabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: com.google.android.material.tabs.TabLayout.Tab) {
                tab = t.position
                load()
            }

            override fun onTabUnselected(t: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(t: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        loadSummary()
        load()
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            val s = ApiClient.call(this@HistoryActivity) { ApiClient.api.historySummary() } ?: return@launch
            b.tvSubtitle.text = "单词 ${s.translate} 条 · 打分 ${s.essay} 条 · 生成 ${s.generate} 条"
        }
    }

    private fun load() {
        b.progress.visibility = View.VISIBLE
        b.tvEmpty.visibility = View.GONE

        // 记下本次请求对应的 Tab：切得快时旧请求会后返回，
        // 不判断就会出现"点了作文打分，却显示单词的空态"
        val requested = tab

        lifecycleScope.launch {
            when (requested) {
                0 -> {
                    val page = ApiClient.call(this@HistoryActivity) { ApiClient.api.historyTranslate() }
                    if (requested != tab) return@launch
                    b.rvHistory.adapter = translateAdapter
                    val items = page?.items.orEmpty()
                    translateAdapter.submit(items)
                    showEmpty(items.isEmpty(), "还没有查过单词，去学习页试试翻译")
                }
                1 -> {
                    val page = ApiClient.call(this@HistoryActivity) { ApiClient.api.historyEssay() }
                    if (requested != tab) return@launch
                    b.rvHistory.adapter = essayAdapter
                    val items = page?.items.orEmpty()
                    essayAdapter.submit(items)
                    showEmpty(items.isEmpty(), "还没有作文打分记录，去学习页提交一篇")
                }
                else -> {
                    val page = ApiClient.call(this@HistoryActivity) { ApiClient.api.historyGenerate() }
                    if (requested != tab) return@launch
                    b.rvHistory.adapter = generateAdapter
                    val items = page?.items.orEmpty()
                    generateAdapter.submit(items)
                    showEmpty(items.isEmpty(), "还没有生成过作文，去学习页试试")
                }
            }
            b.progress.visibility = View.GONE
        }
    }

    private fun showEmpty(empty: Boolean, hint: String) {
        b.tvEmpty.text = hint
        b.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        b.rvHistory.visibility = if (empty) View.GONE else View.VISIBLE
    }

    // ---------------- 详情 ----------------

    private fun detail(title: String, body: String) {
        val vb = DialogHistoryDetailBinding.inflate(layoutInflater)
        vb.tvBody.text = body
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(vb.root)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showTranslate(item: TranslateRecordItem) {
        detail(item.text, buildString {
            append(item.meaning).append("\n\n")
            if (!item.phonetic.isNullOrBlank()) append("音标：${item.phonetic}\n")
            if (!item.pos.isNullOrBlank()) append("词性：${item.pos}\n")
            if (!item.example.isNullOrBlank()) append("例句：${item.example}\n")
            append("\n查询时间：${item.createdAt}")
            if (!item.source.isNullOrBlank()) append("　来源：${item.source}")
        })
    }

    private fun showEssay(item: EssayRecordItem) {
        val dims = parseDims(item.dimsJson)
        detail("作文打分 ${item.score} 分", buildString {
            if (dims.isNotEmpty()) {
                append("四维：")
                append(dims.entries.joinToString("  ") { "${it.key} ${it.value}" })
                append("（满分 30/25/25/20）\n\n")
            }
            if (!item.comment.isNullOrBlank()) append("建议：\n").append(item.comment).append("\n\n")
            append("原文：\n").append(item.content).append("\n\n")
            append("批改时间：${item.createdAt}")
        })
    }

    private fun showGenerate(item: GenerateRecordItem) {
        detail(item.topic, buildString {
            append(item.essay).append("\n\n—— ${item.wordCount} 词 · ${item.levelText.orEmpty()} ——\n\n")
            append("生成时间：${item.createdAt}")
        })
    }

    /**
     * dims_json 是后端序列化的 Map。
     * Gson 会把数字解析成 Double，直接强转 Map<String,Int> 会得到 "28.0"，所以逐项转一次。
     */
    private fun parseDims(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val raw = Gson().fromJson(json, Map::class.java) as? Map<*, *> ?: return emptyMap()
            raw.entries.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                key to ((v as? Number)?.toInt() ?: 0)
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// ---------------- 三个列表适配器 ----------------

class TranslateHistoryAdapter(
    private val onClick: (TranslateRecordItem) -> Unit
) : RecyclerView.Adapter<TranslateHistoryAdapter.VH>() {

    private val data = mutableListOf<TranslateRecordItem>()

    fun submit(list: List<TranslateRecordItem>) {
        data.clear(); data.addAll(list); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemHistoryTranslateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = data.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    inner class VH(private val b: ItemHistoryTranslateBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TranslateRecordItem) {
            b.tvWord.text = item.text
            b.tvPhonetic.text = item.phonetic.orEmpty()
            b.tvMeaning.text = item.meaning
            b.tvTime.text = item.createdAt
            b.root.setOnClickListener { onClick(item) }
        }
    }
}

class EssayHistoryAdapter(
    private val onClick: (EssayRecordItem) -> Unit
) : RecyclerView.Adapter<EssayHistoryAdapter.VH>() {

    private val data = mutableListOf<EssayRecordItem>()

    fun submit(list: List<EssayRecordItem>) {
        data.clear(); data.addAll(list); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemHistoryEssayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = data.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    inner class VH(private val b: ItemHistoryEssayBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: EssayRecordItem) {
            b.tvScore.text = item.score.toString()
            b.tvPreview.text = item.content.replace("\n", " ")
            b.tvTime.text = item.createdAt
            b.root.setOnClickListener { onClick(item) }
        }
    }
}

class GenerateHistoryAdapter(
    private val onClick: (GenerateRecordItem) -> Unit
) : RecyclerView.Adapter<GenerateHistoryAdapter.VH>() {

    private val data = mutableListOf<GenerateRecordItem>()

    fun submit(list: List<GenerateRecordItem>) {
        data.clear(); data.addAll(list); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemHistoryGenerateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = data.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    inner class VH(private val b: ItemHistoryGenerateBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: GenerateRecordItem) {
            b.tvTopic.text = item.topic
            b.tvWords.text = "${item.wordCount} 词"
            b.tvPreview.text = item.essay.replace("\n", " ")
            b.tvTime.text = item.createdAt
            b.root.setOnClickListener { onClick(item) }
        }
    }
}
