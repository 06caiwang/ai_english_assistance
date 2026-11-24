package com.example.ai_english.net

import com.example.ai_english.model.AnswerReq
import com.example.ai_english.model.AnswerResp
import com.example.ai_english.model.ApiResult
import com.example.ai_english.model.AuthResp
import com.example.ai_english.model.BagResp
import com.example.ai_english.model.BuyFoodReq
import com.example.ai_english.model.BuyFoodResp
import com.example.ai_english.model.CharacterItem
import com.example.ai_english.model.CharacterReq
import com.example.ai_english.model.EssayReq
import com.example.ai_english.model.EssayResp
import com.example.ai_english.model.FeedReq
import com.example.ai_english.model.FeedResp
import com.example.ai_english.model.EssayRecordItem
import com.example.ai_english.model.FoodItem
import com.example.ai_english.model.GenerateRecordItem
import com.example.ai_english.model.HistoryPage
import com.example.ai_english.model.HistorySummary
import com.example.ai_english.model.TranslateRecordItem
import com.example.ai_english.model.GenerateReq
import com.example.ai_english.model.GenerateResp
import com.example.ai_english.model.LoginReq
import com.example.ai_english.model.NicknameReq
import com.example.ai_english.model.PetResp
import com.example.ai_english.model.RegisterReq
import com.example.ai_english.model.StatsResp
import com.example.ai_english.model.SwitchResp
import com.example.ai_english.model.TranslateReq
import com.example.ai_english.model.TranslateResp
import com.example.ai_english.model.UnlockResp
import com.example.ai_english.model.UserInfo
import com.example.ai_english.model.WordItem
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {

    // ---------- 鉴权 ----------
    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterReq): ApiResult<AuthResp>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginReq): ApiResult<AuthResp>

    @GET("api/user/me")
    suspend fun me(): ApiResult<UserInfo>

    @PUT("api/user/nickname")
    suspend fun updateNickname(@Body req: NicknameReq): ApiResult<UserInfo>

    // ---------- 单词 ----------
    @GET("api/words/today")
    suspend fun todayWords(): ApiResult<List<WordItem>>

    @POST("api/words/answer")
    suspend fun answer(@Body req: AnswerReq): ApiResult<AnswerResp>

    // ---------- 宠物 ----------
    @GET("api/pet")
    suspend fun pet(): ApiResult<PetResp>

    /** 喂食必须带 foodId：按食物恢复值回血、扣背包库存 */
    @POST("api/pet/feed")
    suspend fun feed(@Body req: FeedReq): ApiResult<FeedResp>

    @POST("api/pet/character")
    suspend fun switchCharacter(@Body req: CharacterReq): ApiResult<SwitchResp>

    // ---------- 食物 / 背包 ----------
    @GET("api/foods")
    suspend fun foods(): ApiResult<List<FoodItem>>

    @POST("api/foods/buy")
    suspend fun buyFood(@Body req: BuyFoodReq): ApiResult<BuyFoodResp>

    @GET("api/bag")
    suspend fun bag(): ApiResult<BagResp>

    // ---------- 形象 ----------
    @GET("api/characters")
    suspend fun characters(): ApiResult<List<CharacterItem>>

    @POST("api/characters/unlock")
    suspend fun unlock(@Body req: CharacterReq): ApiResult<UnlockResp>

    // ---------- AI ----------
    @POST("api/ai/translate")
    suspend fun translate(@Body req: TranslateReq): ApiResult<TranslateResp>

    @POST("api/ai/essay")
    suspend fun essay(@Body req: EssayReq): ApiResult<EssayResp>

    @POST("api/ai/essay/generate")
    suspend fun generateEssay(@Body req: GenerateReq): ApiResult<GenerateResp>

    // ---------- 历史记录 ----------
    @GET("api/history/summary")
    suspend fun historySummary(): ApiResult<HistorySummary>

    @GET("api/history/translate")
    suspend fun historyTranslate(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("size") size: Int = 20
    ): ApiResult<HistoryPage<TranslateRecordItem>>

    @GET("api/history/essay")
    suspend fun historyEssay(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("size") size: Int = 20
    ): ApiResult<HistoryPage<EssayRecordItem>>

    @GET("api/history/generate")
    suspend fun historyGenerate(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("size") size: Int = 20
    ): ApiResult<HistoryPage<GenerateRecordItem>>

    // ---------- 统计 ----------
    @GET("api/stats")
    suspend fun stats(): ApiResult<StatsResp>
}
