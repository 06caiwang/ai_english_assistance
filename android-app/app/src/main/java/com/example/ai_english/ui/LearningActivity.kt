package com.example.ai_english.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ai_english.databinding.ActivityLearningBinding

/** 学习中心：四个能力入口 */
class LearningActivity : AppCompatActivity() {

    private lateinit var b: ActivityLearningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLearningBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.cardWord.setOnClickListener { startActivity(Intent(this, WordActivity::class.java)) }
        b.cardTranslate.setOnClickListener { startActivity(Intent(this, TranslateActivity::class.java)) }
        b.cardGrade.setOnClickListener { startActivity(Intent(this, EssayGradeActivity::class.java)) }
        b.cardGenerate.setOnClickListener { startActivity(Intent(this, EssayGenerateActivity::class.java)) }
        b.cardHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
    }
}
