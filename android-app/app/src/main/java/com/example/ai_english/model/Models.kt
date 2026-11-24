package com.example.ai_english.model

/** 与后端 DTO 一一对应，字段名必须完全一致，否则 Gson 解析成 null。 */

data class ApiResult<T>(val code: Int, val msg: String?, val data: T?)

data class UserInfo(val id: Long = 0, val username: String = "", val nickname: String? = null, val coin: Int = 0)

data class AuthResp(val token: String = "", val user: UserInfo? = null)

data class WordItem(
    val wordId: Long = 0,
    val spelling: String = "",
    val phonetic: String? = null,
    val meaning: String = "",
    val example: String? = null,
    val stage: Int = 0,
    val isNew: Boolean = false,
    /** 缺空提示，如 "ab_n__n"；首尾字母保留 */
    val fillHint: String = "",
    /** 这个词今天要走的关卡：know / fill / spell */
    val modes: List<String>? = null
)

data class AnswerResp(
    val correct: Boolean = false,
    val correctAnswer: String = "",
    val coinGained: Int = 0,
    val expGained: Int = 0,
    val coin: Int = 0,
    val exp: Int = 0,
    val level: Int = 1,
    val hunger: Int = 100,
    val stage: Int = 0,
    val nextReviewAt: String? = null,
    val message: String = ""
)

data class PetResp(
    val name: String = "Luna",
    val hunger: Int = 100,
    val level: Int = 1,
    val exp: Int = 0,
    val characterId: Long = 1,
    val characterImage: String = "",
    val characterName: String = "",
    val lastFedAt: String? = null
)

/** 喂食结果：不再返回金币（金币在购买食物时已经扣了） */
data class FeedResp(
    val hunger: Int = 0,
    val restored: Int = 0,
    val foodCount: Int = 0,
    val petName: String = ""
)

data class FoodItem(
    val id: Long = 0,
    val name: String = "",
    val image: String = "",
    val restore: Int = 0,
    val price: Int = 0,
    val description: String? = null,
    val count: Int = 0
)

data class BagFood(
    val foodId: Long = 0,
    val name: String = "",
    val image: String = "",
    val restore: Int = 0,
    val count: Int = 0
)

data class BagCharacter(val id: Long = 0, val name: String = "", val image: String = "", val current: Boolean = false)

data class BagResp(val foods: List<BagFood>? = null, val characters: List<BagCharacter>? = null)

data class BuyFoodResp(val coin: Int = 0, val foodId: Long = 0, val foodCount: Int = 0, val spent: Int = 0)

data class CharacterItem(
    val id: Long = 0,
    val name: String = "",
    val image: String = "",
    val price: Int = 0,
    val owned: Boolean = false,
    val current: Boolean = false
)

data class UnlockResp(val coin: Int = 0, val characterId: Long = 0, val name: String = "")

data class SwitchResp(val characterId: Long = 0, val characterImage: String = "")

data class TranslateResp(
    val meaning: String = "",
    val phonetic: String? = null,
    val example: String? = null,
    val pos: String? = null,
    val source: String? = null
)

data class Deduction(
    val type: String = "",
    val sentence: String = "",
    val fix: String = "",
    val points: Int = 0
)

data class EssayResp(
    val score: Int = 0,
    val dimensions: Map<String, Int>? = null,
    val deductions: List<Deduction>? = null,
    val wordCount: Int = 0,
    val lengthNote: String? = null,
    val level: String? = null,
    val suggestions: List<String>? = null,
    val trace: List<String>? = null,
    val source: String? = null
)

data class GenerateReq(
    val topic: String,
    val genre: String = "narrative",
    val targetWords: Int = 120,
    val level: String = "senior"
)

data class KeyWord(val word: String = "", val meaning: String = "")

data class GenerateResp(
    val title: String = "",
    val essay: String = "",
    val outline: List<String>? = null,
    val wordCount: Int = 0,
    val keyWords: List<KeyWord>? = null,
    val goodSentences: List<String>? = null,
    val trace: List<String>? = null,
    val source: String? = null
)

data class StatsResp(
    val learnedCount: Int = 0,
    val accuracy: Double = 0.0,
    val streakDays: Int = 0,
    val essayCount: Int = 0
)

// ---------- 历史记录 ----------

data class HistorySummary(val translate: Int = 0, val essay: Int = 0, val generate: Int = 0)

data class HistoryPage<T>(
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20,
    val items: List<T>? = null
)

data class TranslateRecordItem(
    val id: Long = 0,
    val text: String = "",
    val meaning: String = "",
    val phonetic: String? = null,
    val pos: String? = null,
    val example: String? = null,
    val source: String? = null,
    val createdAt: String = ""
)

data class EssayRecordItem(
    val id: Long = 0,
    val content: String = "",
    val score: Int = 0,
    val comment: String? = null,
    val dimsJson: String? = null,
    val deductionsJson: String? = null,
    val createdAt: String = ""
)

data class GenerateRecordItem(
    val id: Long = 0,
    val topic: String = "",
    val essay: String = "",
    val wordCount: Int = 0,
    val genre: String? = null,
    val level: String? = null,
    val levelText: String? = null,
    val keywordsJson: String? = null,
    val createdAt: String = ""
)

// ---------- 请求体 ----------

data class RegisterReq(val username: String, val password: String, val nickname: String?)
data class LoginReq(val username: String, val password: String)
data class AnswerReq(
    val wordId: Long,
    val mode: String,
    val answer: String? = null,
    val correct: Boolean? = null
)
data class CharacterReq(val characterId: Long)
data class TranslateReq(val text: String)
data class EssayReq(val content: String)
data class FeedReq(val foodId: Long)
data class BuyFoodReq(val foodId: Long, val count: Int)
data class NicknameReq(val nickname: String)
