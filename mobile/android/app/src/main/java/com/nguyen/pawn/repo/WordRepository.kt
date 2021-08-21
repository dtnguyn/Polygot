package com.nguyen.pawn.repo

import android.annotation.SuppressLint
import android.util.Log
import com.nguyen.pawn.api.model.ApiResponse
import com.nguyen.pawn.api.model.LogoutRequestBody
import com.nguyen.pawn.api.model.ToggleSavedWordRequestBody
import com.nguyen.pawn.db.PawnDatabase
import com.nguyen.pawn.db.mapper.DailyWordMapper
import com.nguyen.pawn.db.mapper.SavedWordMapper
import com.nguyen.pawn.model.Definition
import com.nguyen.pawn.model.Word
import com.nguyen.pawn.model.WordDetail
import com.nguyen.pawn.repo.utils.mainGetNetworkBoundResource
import com.nguyen.pawn.repo.utils.mainPostNetworkBoundResource
import com.nguyen.pawn.util.Constants.apiURL
import com.nguyen.pawn.util.CustomAppException
import com.nguyen.pawn.util.UIState
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class WordRepository
@Inject constructor(
    private val apiClient: HttpClient,
    private val db: PawnDatabase
) {
    companion object {
        private const val TAG = "WordRepo"
    }

    @SuppressLint("SimpleDateFormat")
    fun getRandomDailyWord(wordCount: Int, language: String): Flow<UIState<List<Word>>> {
        return mainGetNetworkBoundResource(
            query = {
                db.dailyWordDao().getMany(SimpleDateFormat("yyyy.MM.dd").format(Date()), language)
                    .map { if (it.isEmpty()) null else DailyWordMapper.mapToListNetworkEntity(it) }
            },
            fetch = {
                Log.d(TAG, "get random words")
                val response: ApiResponse<ArrayList<Word>?> =
                    apiClient.get("${apiURL}/word/daily?language=${language}&dailyWordCount=${wordCount}")
                Log.d(TAG, "response: $response")
                if (response.status) response.data
                else throw CustomAppException(response.message)
            },
            saveFetchResult = { words ->
                words?.let {
                    db.dailyWordDao().clearAll(language = language)
                    db.dailyWordDao().insertMany(DailyWordMapper.mapToListCacheEntity(it))
                }
            },
            shouldFetch = {
                it.isNullOrEmpty()
            },
            tag = TAG
        )
    }


    fun toggleSavedWord(
        word: Word,
        language: String,
        currentSavedWordList: List<Word>,
        accessToken: String
    ): Flow<UIState<List<Word>>> {
        return mainPostNetworkBoundResource(
            submit = {
                val response: ApiResponse<Void> = apiClient.post("${apiURL}/word/save") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    contentType(ContentType.Application.Json)
                    body = ToggleSavedWordRequestBody(word.value, language)
                }

                if (response.status) {
                    val updatedList = currentSavedWordList.filter {
                        it.value != word.value
                    }
                    if (updatedList.size == currentSavedWordList.size) {
                        arrayListOf(word) + updatedList
                    } else updatedList
                } else throw CustomAppException(response.message)
            },
            shouldSave = {
                false
            },
            saveSubmitResult = {}
        )
    }


    fun getSavedWords(language: String, accessToken: String): Flow<UIState<List<Word>>> {
        Log.d(TAG, "getSavedWords")

        return mainGetNetworkBoundResource(
            query = {
                db.savedWordDao().getMany(language).map { SavedWordMapper.mapToListNetworkEntity(it) }
            },
            fetch = {
                Log.d(TAG, "saved words pre response")

                val response: ApiResponse<List<Word>> = apiClient.get("${apiURL}/word/save?language=${language}") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    contentType(ContentType.Application.Json)
                }
                Log.d(TAG, "saved words response: ${response}")
                if(response.status) response.data
                else throw CustomAppException(response.message)
            },
            saveFetchResult = { savedWords ->
                savedWords?.let {
                    db.savedWordDao().clearAll(language)
                    db.savedWordDao().insertMany(SavedWordMapper.mapToListCacheEntity(it))
                }
            }
        )
    }

    fun getWordDetail(wordValue: String, language: String): Flow<UIState<WordDetail>>{
        var wordDetail: WordDetail? = null
        return mainGetNetworkBoundResource(
            query = {
                flow { emit(wordDetail) }
            },
            fetch = {
                Log.d(TAG, "Fetching word detail")
                val response: ApiResponse<WordDetail> = apiClient.get("${apiURL}/word/detail?word=${wordValue}&language=${language}")
                Log.d(TAG, "word detail response $response")
                if(response.status) response.data
                else throw CustomAppException(response.message)
            },
            saveFetchResult = {
                wordDetail = it
            }
        )
    }

    fun getAutoCompleteWords(search: String, language: String): Flow<UIState<List<Word>>>{
        var autoCompleteWords: List<Word>? = null
        return mainGetNetworkBoundResource(
            query = {
                flow { emit(autoCompleteWords) }
            },
            fetch = {
                Log.d(TAG, "Fetching word detail")
                val response: ApiResponse<List<Word>> = apiClient.get("${apiURL}/word/autocomplete?text=${search}&language=${language}")
                Log.d(TAG, "auto complete words response $response")
                if(response.status) response.data
                else throw CustomAppException(response.message)
            },
            saveFetchResult = {
                autoCompleteWords = it
            }
        )
    }

}