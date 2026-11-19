package com.example.ai_english.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.R
import com.example.ai_english.databinding.ActivityBagBinding
import com.example.ai_english.databinding.ItemBagCharBinding
import com.example.ai_english.databinding.ItemFoodBinding
import com.example.ai_english.model.BagCharacter
import com.example.ai_english.model.BagFood
import com.example.ai_english.model.CharacterReq
import com.example.ai_english.model.FeedReq
import com.example.ai_english.net.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 背包：食物可直接喂，形象可直接切 */
class BagActivity : AppCompatActivity() {

    private lateinit var b: ActivityBagBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBagBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnShop.setOnClickListener { startActivity(Intent(this, ShopActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            ApiClient.call(this@BagActivity) { ApiClient.api.me() }?.let {
                b.tvCoin.text = "🪙 ${it.coin}"
            }
            val bag = ApiClient.call(this@BagActivity) { ApiClient.api.bag() } ?: return@launch

            val foods = bag.foods.orEmpty()
            b.tvFoodEmpty.visibility = if (foods.isEmpty()) View.VISIBLE else View.GONE
            b.llFoods.removeAllViews()
            foods.forEach { b.llFoods.addView(foodRow(it)) }

            b.llCharacters.removeAllViews()
            bag.characters.orEmpty().forEach { b.llCharacters.addView(charRow(it)) }
        }
    }

    private fun foodRow(food: BagFood): View {
        val row = ItemFoodBinding.inflate(LayoutInflater.from(this), b.llFoods, false)
        val resId = if (food.image.isBlank()) 0 else resources.getIdentifier(
            food.image, "drawable", packageName
        )
        row.ivFood.setImageResource(if (resId != 0) resId else R.drawable.food_carrot)
        row.tvName.text = food.name
        row.tvRestore.text = "恢复 ${food.restore} 点饥饿值"
        row.tvCount.text = "×${food.count}"
        row.root.setOnClickListener { confirmFeed(food) }
        return row.root
    }

    private fun charRow(c: BagCharacter): View {
        val row = ItemBagCharBinding.inflate(LayoutInflater.from(this), b.llCharacters, false)
        val resId = if (c.image.isBlank()) 0 else resources.getIdentifier(
            c.image, "drawable", packageName
        )
        row.ivChar.setImageResource(if (resId != 0) resId else R.drawable.char_placeholder)
        row.tvName.text = c.name
        row.tvState.text = if (c.current) "使用中" else "点击使用"
        row.tvState.setTextColor(
            if (c.current) getColor(R.color.cozy_green) else getColor(R.color.cozy_text_sub)
        )
        row.root.setOnClickListener {
            if (c.current) return@setOnClickListener
            lifecycleScope.launch {
                val resp = ApiClient.call(this@BagActivity) {
                    ApiClient.api.switchCharacter(CharacterReq(c.id))
                }
                if (resp != null) {
                    ApiClient.toast(this@BagActivity, "已切换到${c.name}")
                    load()
                }
            }
        }
        return row.root
    }

    private fun confirmFeed(food: BagFood) {
        MaterialAlertDialogBuilder(this)
            .setTitle("喂它吃「${food.name}」")
            .setMessage("消耗 1 个，恢复 ${food.restore} 点饥饿值。")
            .setNegativeButton("取消", null)
            .setPositiveButton("喂食") { _, _ ->
                lifecycleScope.launch {
                    val resp = ApiClient.call(this@BagActivity) { ApiClient.api.feed(FeedReq(food.foodId)) }
                    if (resp != null) {
                        ApiClient.toast(this@BagActivity, "恢复 ${resp.restored} 点，还剩 ${resp.foodCount} 个")
                        load()
                    }
                }
            }
            .show()
    }
}
