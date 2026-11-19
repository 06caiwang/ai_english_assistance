package com.example.ai_english.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ai_english.R
import com.example.ai_english.databinding.ActivityHomeBinding
import com.example.ai_english.net.ApiClient
import com.example.ai_english.util.TokenStore
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var b: ActivityHomeBinding
    private val drums = mutableListOf<ImageView>()
    private var lastHunger = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        buildHungerIcons()

        b.btnFeed.setOnClickListener { FoodSheet().show(supportFragmentManager, "food") }
        b.btnStudy.setOnClickListener { openStudy() }
        b.btnShop.setOnClickListener { startActivity(Intent(this, ShopActivity::class.java)) }
        b.btnBag.setOnClickListener { startActivity(Intent(this, BagActivity::class.java)) }
        b.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        supportFragmentManager.setFragmentResultListener(FoodSheet.KEY_FED, this) { _, _ -> refresh() }
        supportFragmentManager.setFragmentResultListener(CharacterSheet.KEY_RESULT, this) { _, _ -> refresh() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** 10 个鸡腿：满格 10 点 / 半格 5 点 */
    private fun buildHungerIcons() {
        val size = dp(22)
        for (i in 0 until 10) {
            val iv = ImageView(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = dp(2)
            iv.layoutParams = lp
            iv.setImageResource(R.drawable.ic_hunger_empty)
            b.llHunger.addView(iv)
            drums.add(iv)
        }
    }

    private fun renderHunger(hunger: Int) {
        val full = hunger / 10
        val half = if (hunger % 10 >= 5) 1 else 0
        drums.forEachIndexed { index, iv ->
            val res = when {
                index < full -> R.drawable.ic_hunger_full
                index == full && half == 1 -> R.drawable.ic_hunger_half
                else -> R.drawable.ic_hunger_empty
            }
            iv.setImageResource(res)
        }
        b.tvHunger.text = "$hunger / 100"
        b.tvHunger.setTextColor(
            if (hunger <= 19) getColor(R.color.danger) else getColor(R.color.cozy_text_sub)
        )
    }

    private fun bubbleFor(hunger: Int) = when {
        hunger >= 70 -> "精神满满，来学两个词？"
        hunger >= 40 -> "有点饿了…"
        hunger >= 20 -> "肚子在叫了"
        hunger >= 1 -> "好饿好饿…"
        else -> "饿坏了，学不进去了"
    }

    private fun openStudy() {
        if (lastHunger <= 0) {
            ApiClient.toast(this, "它饿得学不进去了，先喂点东西")
            FoodSheet().show(supportFragmentManager, "food")
            return
        }
        startActivity(Intent(this, LearningActivity::class.java))
    }

    private fun refresh() {
        lifecycleScope.launch {
            val me = ApiClient.call(this@HomeActivity) { ApiClient.api.me() }
            if (me == null) {
                if (!TokenStore.isLoggedIn()) {
                    startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                    finish()
                }
                return@launch
            }
            b.tvHello.text = "你好，${me.nickname ?: me.username}"
            b.tvCoin.text = "🪙 ${me.coin}"

            val pet = ApiClient.call(this@HomeActivity) { ApiClient.api.pet() } ?: return@launch
            lastHunger = pet.hunger

            b.tvBubble.text = bubbleFor(pet.hunger)
            b.tvBubble.setTextColor(
                if (pet.hunger <= 19) getColor(R.color.danger) else getColor(R.color.cozy_text_sub)
            )
            b.tvCharName.text = pet.characterName.ifBlank { "宠物" }
            b.tvPetName.text = "名叫 ${pet.name}"
            b.tvLevel.text = "Lv.${pet.level}"
            renderHunger(pet.hunger)
            loadCharacter(pet.characterImage)

            b.progressExp.max = 50
            b.progressExp.progress = pet.exp % 50
            b.tvExp.text = "${pet.exp} / ${pet.level * 50} EXP"

            ApiClient.call(this@HomeActivity) { ApiClient.api.stats() }?.let {
                b.tvLearned.text = it.learnedCount.toString()
                b.tvAccuracy.text = "${(it.accuracy * 100).toInt()}%"
                b.tvStreak.text = it.streakDays.toString()
            }
        }
    }

    private fun loadCharacter(imageName: String) {
        val resId = if (imageName.isBlank()) 0 else resources.getIdentifier(
            imageName, "drawable", packageName
        )
        b.ivPet.setImageResource(if (resId != 0) resId else R.drawable.char_placeholder)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
