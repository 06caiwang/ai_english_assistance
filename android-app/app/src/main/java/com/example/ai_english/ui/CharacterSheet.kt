package com.example.ai_english.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ai_english.R
import com.example.ai_english.databinding.ItemCharacterBinding
import com.example.ai_english.databinding.SheetCharacterBinding
import com.example.ai_english.model.CharacterItem
import com.example.ai_english.model.CharacterReq
import com.example.ai_english.net.ApiClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 形象选择面板：已解锁的直接切，未解锁的二次确认后花金币解锁。 */
class CharacterSheet : BottomSheetDialogFragment() {

    companion object {
        const val KEY_RESULT = "character_changed"
    }

    private lateinit var b: SheetCharacterBinding
    private val adapter = CharacterAdapter { item -> onPick(item) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        b = SheetCharacterBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvCharacters.layoutManager = GridLayoutManager(requireContext(), 3)
        b.rvCharacters.adapter = adapter
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val list = ApiClient.call(requireActivity()) { ApiClient.api.characters() }
            if (list != null) adapter.submit(list)
        }
    }

    private fun onPick(item: CharacterItem) {
        if (item.current) return
        if (item.owned) {
            switchTo(item)
            return
        }
        val price = if (item.price == 0) "免费" else "${item.price} 金币"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("解锁「${item.name}」")
            .setMessage("需要花费 $price，解锁后可以直接使用。")
            .setNegativeButton("再想想", null)
            .setPositiveButton("解锁") { _, _ -> unlock(item) }
            .show()
    }

    private fun unlock(item: CharacterItem) {
        lifecycleScope.launch {
            val resp = ApiClient.call(requireActivity()) {
                ApiClient.api.unlock(CharacterReq(item.id))
            }
            if (resp != null) {
                ApiClient.toast(requireActivity(), "解锁成功，余额 ${resp.coin}")
                switchTo(item)
            }
        }
    }

    private fun switchTo(item: CharacterItem) {
        lifecycleScope.launch {
            val resp = ApiClient.call(requireActivity()) {
                ApiClient.api.switchCharacter(CharacterReq(item.id))
            }
            if (resp != null) {
                parentFragmentManager.setFragmentResult(KEY_RESULT, Bundle())
                dismiss()
            }
        }
    }
}

class CharacterAdapter(
    private val onClick: (CharacterItem) -> Unit
) : RecyclerView.Adapter<CharacterAdapter.VH>() {

    private val data = mutableListOf<CharacterItem>()

    fun submit(list: List<CharacterItem>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCharacterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    inner class VH(private val b: ItemCharacterBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: CharacterItem) {
            val ctx = b.root.context
            val resId = if (item.image.isBlank()) 0 else ctx.resources.getIdentifier(
                item.image, "drawable", ctx.packageName
            )
            b.ivChar.setImageResource(if (resId != 0) resId else R.drawable.char_placeholder)
            b.tvName.text = item.name

            b.tvStatus.text = when {
                item.current -> "使用中"
                item.owned -> "已解锁"
                item.price == 0 -> "免费"
                else -> "${item.price} 金币"
            }
            b.root.strokeWidth = if (item.current) 6 else 2
            b.root.setOnClickListener { onClick(item) }
        }
    }
}
