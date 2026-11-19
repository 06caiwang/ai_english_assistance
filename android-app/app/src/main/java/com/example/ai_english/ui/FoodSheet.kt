package com.example.ai_english.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ai_english.R
import com.example.ai_english.databinding.ItemFoodBinding
import com.example.ai_english.databinding.SheetFoodBinding
import com.example.ai_english.model.BagFood
import com.example.ai_english.model.FeedReq
import com.example.ai_english.net.ApiClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 喂食面板：只列背包里还有库存的食物，点一下二次确认再喂。 */
class FoodSheet : BottomSheetDialogFragment() {

    companion object {
        const val KEY_FED = "food_fed"
    }

    private lateinit var b: SheetFoodBinding
    private val adapter = FoodAdapter { food -> confirmFeed(food) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        b = SheetFoodBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvFoods.layoutManager = LinearLayoutManager(requireContext())
        b.rvFoods.adapter = adapter
        b.btnGoShop.setOnClickListener {
            startActivity(Intent(requireContext(), ShopActivity::class.java))
            dismiss()
        }
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val bag = ApiClient.call(requireActivity()) { ApiClient.api.bag() } ?: return@launch
            val foods = bag.foods.orEmpty()
            b.tvEmpty.visibility = if (foods.isEmpty()) View.VISIBLE else View.GONE
            b.btnGoShop.visibility = if (foods.isEmpty()) View.VISIBLE else View.GONE
            adapter.submit(foods)
        }
    }

    private fun confirmFeed(food: BagFood) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("喂它吃「${food.name}」")
            .setMessage("消耗 1 个，恢复 ${food.restore} 点饥饿值（剩余 ${food.count} 个）。")
            .setNegativeButton("再想想", null)
            .setPositiveButton("喂食") { _, _ -> feed(food) }
            .show()
    }

    private fun feed(food: BagFood) {
        lifecycleScope.launch {
            val resp = ApiClient.call(requireActivity()) {
                ApiClient.api.feed(FeedReq(food.foodId))
            }
            if (resp != null) {
                ApiClient.toast(requireActivity(), "恢复 ${resp.restored} 点，还剩 ${resp.foodCount} 个")
                parentFragmentManager.setFragmentResult(KEY_FED, Bundle())
                dismiss()
            }
        }
    }
}

class FoodAdapter(
    private val onClick: (BagFood) -> Unit
) : RecyclerView.Adapter<FoodAdapter.VH>() {

    private val data = mutableListOf<BagFood>()

    fun submit(list: List<BagFood>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    inner class VH(private val b: ItemFoodBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: BagFood) {
            val ctx = b.root.context
            val resId = if (item.image.isBlank()) 0 else ctx.resources.getIdentifier(
                item.image, "drawable", ctx.packageName
            )
            b.ivFood.setImageResource(if (resId != 0) resId else R.drawable.food_carrot)
            b.tvName.text = item.name
            b.tvRestore.text = "恢复 ${item.restore} 点饥饿值"
            b.tvCount.text = "×${item.count}"
            b.root.setOnClickListener { onClick(item) }
        }
    }
}
