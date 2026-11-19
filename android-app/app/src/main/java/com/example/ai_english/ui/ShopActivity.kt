package com.example.ai_english.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ai_english.R
import com.example.ai_english.databinding.ActivityShopBinding
import com.example.ai_english.databinding.ItemFoodShopBinding
import com.example.ai_english.model.BuyFoodReq
import com.example.ai_english.model.CharacterItem
import com.example.ai_english.model.CharacterReq
import com.example.ai_english.model.FoodItem
import com.example.ai_english.net.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 商城：食物 / 形象 两个 Tab */
class ShopActivity : AppCompatActivity() {

    private lateinit var b: ActivityShopBinding
    private val foodAdapter = ShopFoodAdapter { food -> buy(food) }
    private var charAdapter: CharacterAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityShopBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvFoods.layoutManager = LinearLayoutManager(this)
        b.rvFoods.adapter = foodAdapter
        b.rvCharacters.layoutManager = GridLayoutManager(this, 3)
        charAdapter = CharacterAdapter { item -> onCharacter(item) }.also { b.rvCharacters.adapter = it }

        b.tabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                val foodTab = tab.position == 0
                b.rvFoods.visibility = if (foodTab) View.VISIBLE else View.GONE
                b.rvCharacters.visibility = if (foodTab) View.GONE else View.VISIBLE
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            ApiClient.call(this@ShopActivity) { ApiClient.api.me() }?.let {
                b.tvCoin.text = "🪙 ${it.coin}"
            }
            ApiClient.call(this@ShopActivity) { ApiClient.api.foods() }?.let { foodAdapter.submit(it) }
            ApiClient.call(this@ShopActivity) { ApiClient.api.characters() }?.let { charAdapter?.submit(it) }
        }
    }

    private fun buy(food: FoodItem) {
        lifecycleScope.launch {
            val resp = ApiClient.call(this@ShopActivity) {
                ApiClient.api.buyFood(BuyFoodReq(food.id, 1))
            }
            if (resp != null) {
                ApiClient.toast(this@ShopActivity, "买到 1 个${food.name}，余额 ${resp.coin}")
                load()
            }
        }
    }

    private fun onCharacter(item: CharacterItem) {
        when {
            item.current -> ApiClient.toast(this, "正在使用这个形象")
            item.owned -> lifecycleScope.launch {
                val resp = ApiClient.call(this@ShopActivity) {
                    ApiClient.api.switchCharacter(CharacterReq(item.id))
                }
                if (resp != null) {
                    ApiClient.toast(this@ShopActivity, "已切换到${item.name}")
                    load()
                }
            }

            else -> MaterialAlertDialogBuilder(this)
                .setTitle("解锁「${item.name}」")
                .setMessage("需要花费 ${item.price} 金币，解锁后可直接使用。")
                .setNegativeButton("再想想", null)
                .setPositiveButton("解锁") { _, _ ->
                    lifecycleScope.launch {
                        val resp = ApiClient.call(this@ShopActivity) {
                            ApiClient.api.unlock(CharacterReq(item.id))
                        }
                        if (resp != null) {
                            ApiClient.toast(this@ShopActivity, "解锁成功，余额 ${resp.coin}")
                            load()
                        }
                    }
                }
                .show()
        }
    }
}

class ShopFoodAdapter(
    private val onBuy: (FoodItem) -> Unit
) : RecyclerView.Adapter<ShopFoodAdapter.VH>() {

    private val data = mutableListOf<FoodItem>()

    fun submit(list: List<FoodItem>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFoodShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    inner class VH(private val b: ItemFoodShopBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FoodItem) {
            val ctx = b.root.context
            val resId = if (item.image.isBlank()) 0 else ctx.resources.getIdentifier(
                item.image, "drawable", ctx.packageName
            )
            b.ivFood.setImageResource(if (resId != 0) resId else R.drawable.food_carrot)
            b.tvName.text = item.name
            b.tvDesc.text = "恢复 ${item.restore} 点 · 已有 ${item.count}"
            b.btnBuy.text = "🪙 ${item.price}"
            b.btnBuy.setOnClickListener { onBuy(item) }
        }
    }
}
