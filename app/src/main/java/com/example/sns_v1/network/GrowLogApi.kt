package com.example.sns_v1.network

import com.example.sns_v1.model.Comment
import com.example.sns_v1.model.Goal
import com.example.sns_v1.model.Notification
import com.example.sns_v1.model.ChatThread
import com.example.sns_v1.model.Conversation
import com.example.sns_v1.model.DiscoverData
import com.example.sns_v1.model.FocusMode
import com.example.sns_v1.model.FocusPresence
import com.example.sns_v1.model.FocusRoom
import com.example.sns_v1.model.FocusSession
import com.example.sns_v1.model.FocusVisibility
import com.example.sns_v1.model.Garden
import com.example.sns_v1.model.GardenPlant
import com.example.sns_v1.model.GardenTheme
import com.example.sns_v1.model.Group
import com.example.sns_v1.model.GroupMember
import com.example.sns_v1.model.GroupVisibility
import com.example.sns_v1.model.Message
import com.example.sns_v1.model.Post
import com.example.sns_v1.model.SearchResult
import com.example.sns_v1.model.TrendingTag
import com.example.sns_v1.model.UserProfile
import com.example.sns_v1.model.UserSummary
import com.example.sns_v1.network.model.LoginResponse
import com.example.sns_v1.utils.formatRelativeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PostsResult(val posts: List<Post>, val nextCursor: String?)
data class ReactionResult(val reacted: Boolean, val reactionCount: Int)
data class UploadResult(val imageKey: String, val imageUrl: String)

class GrowLogApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    suspend fun login(idToken: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/auth/login")
                .addHeader("Authorization", "Bearer $idToken")
                .post(ByteArray(0).toRequestBody(json))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}: $body"))

            val j = JSONObject(body)
            Result.success(LoginResponse(
                userId = j.getString("userId"),
                userName = j.getString("userName"),
                displayName = j.getString("displayName"),
                isNewUser = j.getBoolean("isNewUser")
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMe(idToken: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/users/me")
                .addHeader("Authorization", "Bearer $idToken")
                .get().build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))
            Result.success(JSONObject(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        idToken: String,
        displayName: String? = null,
        biography: String? = null,
        profileImageKey: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                if (displayName != null) put("displayName", displayName)
                if (biography != null) put("biography", biography)
                if (profileImageKey != null) put("profileImageKey", profileImageKey)
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/users/me")
                .addHeader("Authorization", "Bearer $idToken")
                .patch(payload.toString().toRequestBody(json))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}: $body"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPosts(
        idToken: String,
        cursor: String? = null,
        feed: String? = null
    ): Result<PostsResult> =
        withContext(Dispatchers.IO) {
            try {
                val query = buildList {
                    if (cursor != null) add("cursor=${URLEncoder.encode(cursor, "UTF-8")}")
                    if (feed != null) add("feed=$feed")
                }
                val url = buildString {
                    append("${ApiConfig.BASE_URL}/api/v1/posts")
                    if (query.isNotEmpty()) append("?").append(query.joinToString("&"))
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))

                val j = JSONObject(body)
                val postsArr = j.getJSONArray("posts")
                val posts = (0 until postsArr.length()).map { parsePost(postsArr.getJSONObject(it)) }
                val nextCursor = if (j.isNull("nextCursor")) null else j.getString("nextCursor")
                Result.success(PostsResult(posts, nextCursor))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createPost(
        idToken: String,
        content: String,
        postType: String = "progress",
        goalId: String? = null,
        progress: Int? = null,
        tags: List<String> = emptyList(),
        imageKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("content", content)
                put("postType", postType)
                if (goalId != null) put("goalId", goalId)
                if (progress != null) put("progress", progress)
                if (imageKey != null) put("imageKey", imageKey)
                put("tags", JSONArray(tags))
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/posts")
                .addHeader("Authorization", "Bearer $idToken")
                .post(payload.toString().toRequestBody(json))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}: $body"))
            Result.success(JSONObject(body).getString("postId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(
        idToken: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<UploadResult> = upload("image", idToken, bytes, mimeType)

    suspend fun uploadAvatar(
        idToken: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<UploadResult> = upload("avatar", idToken, bytes, mimeType)

    private suspend fun upload(
        endpoint: String,
        idToken: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<UploadResult> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/uploads/$endpoint")
                .addHeader("Authorization", "Bearer $idToken")
                .post(bytes.toRequestBody(mimeType.toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}: $body"))

            val j = JSONObject(body)
            Result.success(UploadResult(
                imageKey = j.getString("imageKey"),
                imageUrl = j.getString("imageUrl")
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(idToken: String, postId: String): Result<ReactionResult> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/posts/$postId/reactions")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(ByteArray(0).toRequestBody(json))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))

                val j = JSONObject(body)
                Result.success(ReactionResult(
                    reacted = j.getBoolean("reacted"),
                    reactionCount = j.getInt("reactionCount")
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getPost(idToken: String, postId: String): Result<Post> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/posts/$postId")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                Result.success(parsePost(JSONObject(body)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun toggleSave(idToken: String, postId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/posts/$postId/save")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(ByteArray(0).toRequestBody(json))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                Result.success(JSONObject(body).getBoolean("saved"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSavedPosts(idToken: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/users/me/saves")
                .addHeader("Authorization", "Bearer $idToken")
                .get().build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))

            val arr = JSONObject(body).getJSONArray("posts")
            Result.success((0 until arr.length()).map { parsePost(arr.getJSONObject(it)) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiscover(idToken: String): Result<DiscoverData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/discover")
                .addHeader("Authorization", "Bearer $idToken")
                .get().build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))

            val j = JSONObject(body)
            val tagsArr = j.getJSONArray("trendingTags")
            val usersArr = j.getJSONArray("suggestedUsers")
            val postsArr = j.optJSONArray("suggestedPosts")
            Result.success(DiscoverData(
                trendingTags = (0 until tagsArr.length()).map {
                    val t = tagsArr.getJSONObject(it)
                    TrendingTag(t.getString("tag"), t.optInt("count", 0))
                },
                suggestedPosts = if (postsArr == null) emptyList()
                                 else (0 until postsArr.length()).map { parsePost(postsArr.getJSONObject(it)) },
                suggestedUsers = (0 until usersArr.length()).map {
                    parseUserSummary(usersArr.getJSONObject(it))
                }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(idToken: String, query: String): Result<SearchResult> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/discover/search?q=$encoded")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))

                val j = JSONObject(body)
                val postsArr = j.getJSONArray("posts")
                val usersArr = j.getJSONArray("users")
                Result.success(SearchResult(
                    posts = (0 until postsArr.length()).map { parsePost(postsArr.getJSONObject(it)) },
                    users = (0 until usersArr.length()).map { parseUserSummary(usersArr.getJSONObject(it)) }
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- 集中ステータス ----

    private fun parseFocusSession(j: JSONObject) = FocusSession(
        focusSessionId = j.getString("focusSessionId"),
        userId = j.getString("userId"),
        displayName = j.getString("displayName"),
        userName = j.getString("userName"),
        profileImageUrl = j.optUrl("profileImageUrl"),
        activityTitle = j.getString("activityTitle"),
        goalTitle = j.optUrl("goalTitle"),
        groupId = j.optUrl("groupId"),
        status = j.optString("status", "active"),
        visibility = FocusVisibility.from(j.optString("visibilityType")),
        focusMode = FocusMode.from(j.optString("focusMode")),
        plannedDurationSeconds = if (j.isNull("plannedDurationSeconds")) null
                                 else j.optInt("plannedDurationSeconds"),
        actualDurationSeconds = if (j.isNull("actualDurationSeconds")) null
                                else j.optInt("actualDurationSeconds"),
        elapsedSeconds = j.optInt("elapsedSeconds", 0),
        gardenTheme = GardenTheme.from(j.optString("gardenTheme")),
        gardenStage = j.optInt("gardenStage", 0),
        secondsToNextStage = if (j.isNull("secondsToNextStage")) null
                             else j.optInt("secondsToNextStage"),
        breakCount = j.optInt("breakCount", 0),
        cheerCount = j.optInt("cheerCount", 0),
        myReactions = j.optJSONArray("myReactions")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
    )

    suspend fun startFocusSession(
        idToken: String,
        activityTitle: String,
        goalId: String? = null,
        groupId: String? = null,
        plannedDurationSeconds: Int? = null,
        visibility: FocusVisibility = FocusVisibility.FOLLOWERS,
        mode: FocusMode = FocusMode.LIGHT,
        gardenTheme: GardenTheme = GardenTheme.PLANT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("activityTitle", activityTitle)
                if (goalId != null) put("goalId", goalId)
                if (groupId != null) put("groupId", groupId)
                if (plannedDurationSeconds != null) put("plannedDurationSeconds", plannedDurationSeconds)
                put("visibilityType", visibility.value)
                put("focusMode", mode.value)
                put("gardenTheme", gardenTheme.value)
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/focus-sessions")
                .addHeader("Authorization", "Bearer $idToken")
                .post(payload.toString().toRequestBody(json)).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).getString("error") }
                    .getOrDefault("API error ${response.code}")
                return@withContext Result.failure(Exception(message))
            }
            Result.success(JSONObject(body).getString("focusSessionId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveFocusSessions(idToken: String): Result<List<FocusSession>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/focus-sessions/active")
                    .addHeader("Authorization", "Bearer $idToken").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                val arr = JSONObject(body).getJSONArray("sessions")
                Result.success((0 until arr.length()).map { parseFocusSession(arr.getJSONObject(it)) })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** pause / resume のような、本文を持たないセッション操作 */
    private suspend fun focusAction(idToken: String, path: String, payload: JSONObject? = null):
        Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val bodyContent = (payload ?: JSONObject()).toString().toRequestBody(json)
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/focus-sessions/$path")
                .addHeader("Authorization", "Bearer $idToken")
                .post(bodyContent).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).getString("error") }
                    .getOrDefault("API error ${response.code}")
                return@withContext Result.failure(Exception(message))
            }
            Result.success(JSONObject(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pauseFocusSession(idToken: String, sessionId: String) =
        focusAction(idToken, "$sessionId/pause")

    suspend fun resumeFocusSession(idToken: String, sessionId: String) =
        focusAction(idToken, "$sessionId/resume")

    suspend fun completeFocusSession(
        idToken: String,
        sessionId: String,
        reflection: String? = null,
        concentrationRating: Int? = null
    ) = focusAction(idToken, "$sessionId/complete", JSONObject().apply {
        if (reflection != null) put("reflection", reflection)
        if (concentrationRating != null) put("concentrationRating", concentrationRating)
    })

    suspend fun cancelFocusSession(idToken: String, sessionId: String, endReason: String? = null) =
        focusAction(idToken, "$sessionId/cancel", JSONObject().apply {
            if (endReason != null) put("endReason", endReason)
        })

    suspend fun sendFocusReaction(idToken: String, sessionId: String, reactionType: String = "cheer") =
        focusAction(idToken, "$sessionId/reactions", JSONObject().put("reactionType", reactionType))

    suspend fun getFocusRoom(idToken: String, groupId: String): Result<FocusRoom> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/groups/$groupId/focus-room")
                    .addHeader("Authorization", "Bearer $idToken").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                val j = JSONObject(body)
                val arr = j.getJSONArray("activeSessions")
                Result.success(FocusRoom(
                    activeSessions = (0 until arr.length()).map { parseFocusSession(arr.getJSONObject(it)) },
                    todayTotalSeconds = j.optInt("todayTotalSeconds", 0),
                    monthGrownCount = j.optInt("monthGrownCount", 0),
                    monthTotalSeconds = j.optInt("monthTotalSeconds", 0),
                    monthMemberCount = j.optInt("monthMemberCount", 0)
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** 今週の庭。[userName] を省略すると自分の庭 */
    suspend fun getGarden(idToken: String, userName: String? = null): Result<Garden> =
        withContext(Dispatchers.IO) {
            try {
                val suffix = if (userName != null) "/$userName" else ""
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/focus-sessions/garden$suffix")
                    .addHeader("Authorization", "Bearer $idToken").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))

                val j = JSONObject(body)
                val arr = j.getJSONArray("plants")
                Result.success(Garden(
                    sessionCount = j.optInt("sessionCount", 0),
                    totalSeconds = j.optInt("totalSeconds", 0),
                    plants = (0 until arr.length()).map {
                        val p = arr.getJSONObject(it)
                        GardenPlant(
                            focusSessionId = p.getString("focusSessionId"),
                            activityTitle = p.optString("activityTitle", ""),
                            gardenTheme = GardenTheme.from(p.optString("gardenTheme")),
                            gardenStage = p.optInt("gardenStage", 0),
                            durationSeconds = p.optInt("durationSeconds", 0)
                        )
                    }
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- グループ ----

    private fun parseGroup(j: JSONObject) = Group(
        groupId = j.getString("groupId"),
        groupSlug = j.getString("groupSlug"),
        groupName = j.getString("groupName"),
        description = j.optString("description", ""),
        visibility = GroupVisibility.from(j.optString("visibilityType")),
        joinType = j.optString("joinType", "open"),
        memberCount = j.optInt("memberCount", 1),
        maximumMembers = j.optInt("maximumMembers", 500),
        iconImageUrl = j.optUrl("iconImageUrl"),
        coverImageUrl = j.optUrl("coverImageUrl"),
        myRole = j.optUrl("myRole"),
        isMember = j.optBoolean("isMember", false),
        isOwner = j.optBoolean("isOwner", false),
        canViewContent = j.optBoolean("canViewContent", true)
    )

    suspend fun getGroups(idToken: String, query: String = "", mineOnly: Boolean = false):
        Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val params = buildList {
                if (query.isNotBlank()) add("q=${URLEncoder.encode(query, "UTF-8")}")
                if (mineOnly) add("mine=true")
            }
            val url = "${ApiConfig.BASE_URL}/api/v1/groups" +
                if (params.isEmpty()) "" else "?" + params.joinToString("&")
            val request = Request.Builder()
                .url(url).addHeader("Authorization", "Bearer $idToken").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))
            val arr = JSONObject(body).getJSONArray("groups")
            Result.success((0 until arr.length()).map { parseGroup(arr.getJSONObject(it)) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        idToken: String,
        groupName: String,
        groupSlug: String,
        description: String,
        visibility: GroupVisibility
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("groupName", groupName)
                put("groupSlug", groupSlug)
                put("description", description)
                put("visibilityType", visibility.value)
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/groups")
                .addHeader("Authorization", "Bearer $idToken")
                .post(payload.toString().toRequestBody(json)).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).getString("error") }
                    .getOrDefault("API error ${response.code}")
                return@withContext Result.failure(Exception(message))
            }
            Result.success(JSONObject(body).getString("groupId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGroup(idToken: String, groupId: String): Result<Group> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/groups/$groupId")
                    .addHeader("Authorization", "Bearer $idToken").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                Result.success(parseGroup(JSONObject(body)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** join / leave のような、結果を持たないグループ操作 */
    private suspend fun groupAction(idToken: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/groups/$path")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(ByteArray(0).toRequestBody(json)).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(body ?: "{}").getString("error") }
                        .getOrDefault("API error ${response.code}")
                    return@withContext Result.failure(Exception(message))
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun joinGroup(idToken: String, groupId: String) =
        groupAction(idToken, "$groupId/join")

    suspend fun leaveGroup(idToken: String, groupId: String) =
        groupAction(idToken, "$groupId/leave")

    private suspend fun groupJsonAction(idToken: String, path: String, payload: JSONObject): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/groups/$path")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(payload.toString().toRequestBody(json)).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(body ?: "{}").getString("error") }.getOrDefault("API error ${response.code}")
                    return@withContext Result.failure(Exception(message))
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun inviteToGroup(idToken: String, groupId: String, userName: String) =
        groupJsonAction(idToken, "$groupId/invitations", JSONObject().put("userName", userName))

    suspend fun decideGroupJoinRequest(idToken: String, groupId: String, requestId: String, approve: Boolean) =
        groupJsonAction(idToken, "$groupId/join-requests/$requestId/decision", JSONObject().put("approve", approve))

    suspend fun decideGroupInvitation(idToken: String, groupId: String, invitationId: String, accept: Boolean) =
        groupJsonAction(idToken, "$groupId/invitations/$invitationId/decision", JSONObject().put("accept", accept))

    suspend fun getGroupMembers(idToken: String, groupId: String): Result<List<GroupMember>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/groups/$groupId/members")
                    .addHeader("Authorization", "Bearer $idToken").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                val arr = JSONObject(body).getJSONArray("members")
                Result.success((0 until arr.length()).map {
                    val m = arr.getJSONObject(it)
                    GroupMember(
                        userId = m.getString("userId"),
                        displayName = m.getString("displayName"),
                        userName = m.getString("userName"),
                        profileImageUrl = m.optUrl("profileImageUrl"),
                        role = m.optString("role", "member"),
                        joinedAt = formatRelativeTime(m.optString("joinedAt", ""))
                    )
                })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getGroupPosts(idToken: String, groupId: String): Result<List<Post>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/groups/$groupId/posts")
                    .addHeader("Authorization", "Bearer $idToken").get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))
                val arr = JSONObject(body).getJSONArray("posts")
                Result.success((0 until arr.length()).map { parsePost(arr.getJSONObject(it)) })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createGroupPost(
        idToken: String,
        groupId: String,
        content: String,
        shareOutside: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("content", content)
                put("groupVisibility", if (shareOutside) "public" else "members")
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/groups/$groupId/posts")
                .addHeader("Authorization", "Bearer $idToken")
                .post(payload.toString().toRequestBody(json)).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).getString("error") }
                    .getOrDefault("API error ${response.code}")
                return@withContext Result.failure(Exception(message))
            }
            Result.success(JSONObject(body).getString("postId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- DM ----

    suspend fun getConversations(idToken: String): Result<List<Conversation>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/conversations")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))

                val arr = JSONObject(body).getJSONArray("conversations")
                Result.success((0 until arr.length()).map {
                    val j = arr.getJSONObject(it)
                    Conversation(
                        conversationId = j.getString("conversationId"),
                        partnerUserId = j.getString("partnerUserId"),
                        partnerDisplayName = j.getString("partnerDisplayName"),
                        partnerUserName = j.getString("partnerUserName"),
                        partnerImageUrl = j.optUrl("partnerImageUrl"),
                        lastMessage = if (j.isNull("lastMessage")) null else j.getString("lastMessage"),
                        lastMessageAt = if (j.isNull("lastMessageAt")) null
                                        else formatRelativeTime(j.getString("lastMessageAt")),
                        unreadCount = j.optInt("unreadCount", 0),
                        partnerFocus = j.optPresence("partnerFocus")
                    )
                })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getUnreadMessageCount(idToken: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/conversations/unread-count")
                .addHeader("Authorization", "Bearer $idToken")
                .get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))
            Result.success(JSONObject(body).optInt("unreadCount", 0))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 相手のユーザー名から会話を開く（無ければ作られる） */
    suspend fun openConversation(idToken: String, userName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().put("userName", userName)
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/conversations")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(payload.toString().toRequestBody(json)).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}: $body"))
                Result.success(JSONObject(body).getString("conversationId"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMessages(idToken: String, conversationId: String): Result<ChatThread> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/conversations/$conversationId/messages")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}"))

                val j = JSONObject(body)
                val partnerJson = j.optJSONObject("partner")
                val arr = j.getJSONArray("messages")
                Result.success(ChatThread(
                    conversationId = j.getString("conversationId"),
                    partner = partnerJson?.let {
                        UserSummary(
                            userId = it.getString("userId"),
                            displayName = it.getString("displayName"),
                            userName = it.getString("userName"),
                            profileImageUrl = it.optUrl("profileImageUrl")
                        )
                    },
                    messages = (0 until arr.length()).map { i ->
                        val m = arr.getJSONObject(i)
                        Message(
                            messageId = m.getString("messageId"),
                            senderId = m.getString("senderId"),
                            content = m.getString("content"),
                            isMine = m.optBoolean("isMine", false),
                            isRead = m.optBoolean("isRead", false),
                            createdAt = formatRelativeTime(m.optString("createdAt", ""))
                        )
                    }
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendMessage(idToken: String, conversationId: String, content: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().put("content", content)
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/conversations/$conversationId/messages")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(payload.toString().toRequestBody(json)).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful)
                    return@withContext Result.failure(Exception("API error ${response.code}: $body"))
                Result.success(JSONObject(body).getString("messageId"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMyGoals(idToken: String): Result<List<Goal>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/goals/my")
                .addHeader("Authorization", "Bearer $idToken")
                .get().build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))

            val arr = JSONObject(body).getJSONArray("goals")
            val goals = (0 until arr.length()).map { parseGoal(arr.getJSONObject(it)) }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGoal(
        idToken: String,
        title: String,
        description: String = "",
        startDate: String,
        endDate: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("title", title)
                put("description", description)
                put("startDate", startDate)
                if (endDate != null) put("endDate", endDate)
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/goals")
                .addHeader("Authorization", "Bearer $idToken")
                .post(payload.toString().toRequestBody(json))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}: $body"))
            Result.success(JSONObject(body).getString("goalId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(
        idToken: String,
        goalId: String,
        title: String? = null,
        description: String? = null,
        progress: Int? = null,
        isAchieved: Boolean? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                if (title != null) put("title", title)
                if (description != null) put("description", description)
                if (progress != null) put("progress", progress)
                if (isAchieved != null) put("isAchieved", isAchieved)
            }
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/goals/$goalId")
                .addHeader("Authorization", "Bearer $idToken")
                .patch(payload.toString().toRequestBody(json))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}: $body"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyPosts(idToken: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/users/me/posts")
                .addHeader("Authorization", "Bearer $idToken")
                .get().build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            if (!response.isSuccessful)
                return@withContext Result.failure(Exception("API error ${response.code}"))

            val arr = JSONObject(body).getJSONArray("posts")
            val posts = (0 until arr.length()).map { parsePost(arr.getJSONObject(it)) }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(idToken: String, postId: String): Result<List<Comment>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/posts/$postId/comments")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}"))
                val arr = JSONObject(body).getJSONArray("comments")
                Result.success((0 until arr.length()).map { parseComment(arr.getJSONObject(it)) })
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun addComment(idToken: String, postId: String, content: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().put("content", content)
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/posts/$postId/comments")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(payload.toString().toRequestBody(json)).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}: $body"))
                Result.success(JSONObject(body).getString("commentId"))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun deleteComment(idToken: String, postId: String, commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/posts/$postId/comments/$commentId")
                    .addHeader("Authorization", "Bearer $idToken")
                    .delete().build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}"))
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun toggleFollow(idToken: String, userName: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/users/$userName/follow")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(ByteArray(0).toRequestBody(json)).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}"))
                Result.success(JSONObject(body).getBoolean("following"))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getUserProfile(idToken: String, userName: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/users/$userName/profile")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}"))
                val j = JSONObject(body)
                Result.success(UserProfile(
                    userId = j.getString("userId"),
                    displayName = j.getString("displayName"),
                    userName = j.getString("userName"),
                    biography = j.optString("biography", ""),
                    profileImageUrl = j.optUrl("profileImageUrl"),
                    followerCount = j.optInt("followerCount", 0),
                    followingCount = j.optInt("followingCount", 0),
                    isFollowing = j.optBoolean("isFollowing", false),
                    postCount = j.optInt("postCount", 0),
                    goalCount = j.optInt("goalCount", 0),
                    focus = j.optPresence("focus")
                ))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getUserPosts(idToken: String, userName: String): Result<List<Post>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/users/$userName/posts")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}"))
                val arr = JSONObject(body).getJSONArray("posts")
                Result.success((0 until arr.length()).map { parsePost(arr.getJSONObject(it)) })
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getNotifications(idToken: String): Result<List<Notification>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/notifications")
                    .addHeader("Authorization", "Bearer $idToken")
                    .get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("API error ${response.code}"))
                val arr = JSONObject(body).getJSONArray("notifications")
                Result.success((0 until arr.length()).map { parseNotification(arr.getJSONObject(it)) })
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun markNotificationsRead(idToken: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}/api/v1/notifications/read-all")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(ByteArray(0).toRequestBody(json)).build()
                client.newCall(request).execute()
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    /** null / 空文字 / キー欠落をまとめて null に寄せる */
    private fun JSONObject.optUrl(name: String): String? =
        if (isNull(name)) null else optString(name).ifBlank { null }

    /** 集中していなければサーバーは null を返すので、そのまま null にする */
    private fun JSONObject.optPresence(name: String): FocusPresence? {
        if (isNull(name)) return null
        val p = optJSONObject(name) ?: return null
        return FocusPresence(
            focusSessionId = p.optString("focusSessionId", ""),
            status = p.optString("status", "active"),
            activityTitle = p.optString("activityTitle", ""),
            elapsedSeconds = p.optInt("elapsedSeconds", 0),
            gardenTheme = GardenTheme.from(p.optString("gardenTheme")),
            gardenStage = p.optInt("gardenStage", 0)
        )
    }

    private fun parseUserSummary(j: JSONObject) = UserSummary(
        userId = j.getString("userId"),
        displayName = j.getString("displayName"),
        userName = j.getString("userName"),
        biography = j.optString("biography", ""),
        profileImageUrl = j.optUrl("profileImageUrl"),
        followerCount = j.optInt("followerCount", 0)
    )

    private fun parseComment(j: JSONObject) = Comment(
        commentId = j.getString("commentId"),
        userId = j.getString("userId"),
        displayName = j.getString("displayName"),
        userName = j.getString("userName"),
        userImageUrl = j.optUrl("userImageUrl"),
        content = j.getString("content"),
        createdAt = formatRelativeTime(j.optString("createdAt", ""))
    )

    private fun parseNotification(j: JSONObject) = Notification(
        notificationId = j.getString("notificationId"),
        type = j.getString("type"),
        actorDisplayName = j.getString("actorDisplayName"),
        actorUserName = j.getString("actorUserName"),
        actorImageUrl = j.optUrl("actorImageUrl"),
        postId = if (j.isNull("postId")) null else j.getString("postId"),
        postContent = if (j.isNull("postContent")) null else j.getString("postContent"),
        commentContent = if (j.isNull("commentContent")) null else j.getString("commentContent"),
        groupId = j.optUrl("groupId"),
        groupName = if (j.isNull("groupName")) null else j.optString("groupName").ifBlank { null },
        groupJoinRequestId = if (j.isNull("groupJoinRequestId")) null else j.optString("groupJoinRequestId").ifBlank { null },
        groupInvitationId = if (j.isNull("groupInvitationId")) null else j.optString("groupInvitationId").ifBlank { null },
        isRead = j.optBoolean("isRead", false),
        createdAt = formatRelativeTime(j.optString("createdAt", ""))
    )

    private fun parsePost(j: JSONObject): Post {
        val tagsArr = j.optJSONArray("tags")
        val tags = if (tagsArr != null) (0 until tagsArr.length()).map { tagsArr.getString(it) }
                   else emptyList()
        return Post(
            postId = j.getString("postId"),
            userId = j.getString("userId"),
            displayName = j.getString("displayName"),
            userName = j.getString("userName"),
            authorImageUrl = j.optUrl("authorImageUrl"),
            content = j.getString("content"),
            postType = j.optString("postType", "progress"),
            tags = tags,
            imageUrl = j.optUrl("imageUrl"),
            groupId = j.optUrl("groupId"),
            isPinned = j.optBoolean("isPinned", false),
            progress = if (j.isNull("progress")) null else j.getInt("progress"),
            goalTitle = if (j.isNull("goalTitle")) null else j.getString("goalTitle"),
            reactionCount = j.optInt("reactionCount", 0),
            commentCount = j.optInt("commentCount", 0),
            myReaction = j.optBoolean("myReaction", false),
            isSaved = j.optBoolean("isSaved", false),
            authorFocus = j.optPresence("authorFocus"),
            createdAt = formatRelativeTime(j.optString("createdAt", ""))
        )
    }

    private fun parseGoal(j: JSONObject): Goal = Goal(
        goalId = j.getString("goalId"),
        title = j.getString("title"),
        description = j.optString("description", ""),
        progress = j.optInt("progress", 0),
        startDate = j.optString("startDate", ""),
        endDate = if (j.isNull("endDate")) null else j.getString("endDate"),
        isAchieved = j.optBoolean("isAchieved", false),
        activityCount = j.optInt("activityCount", 0)
    )
}

object ApiClient {
    val instance: GrowLogApi by lazy { GrowLogApi() }
}
