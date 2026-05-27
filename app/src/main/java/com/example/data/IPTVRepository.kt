package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class IPTVRepository(
    private val context: Context,
    private val dao: IPTVDao
) {
    private val tag = "IPTVRepository"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val activePlaylist: Flow<PlaylistEntity?> = dao.getActivePlaylistFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChannels: Flow<List<ChannelEntity>> = activePlaylist.flatMapLatest { playlist ->
        if (playlist != null) {
            dao.getChannelsForPlaylist(playlist.id)
        } else {
            emptyFlow()
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeGroups: Flow<List<String>> = activePlaylist.flatMapLatest { playlist ->
        if (playlist != null) {
            dao.getGroupsForPlaylist(playlist.id)
        } else {
            emptyFlow()
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoriteChannels: Flow<List<ChannelEntity>> = activePlaylist.flatMapLatest { playlist ->
        if (playlist != null) {
            dao.getFavoriteChannels(playlist.id)
        } else {
            emptyFlow()
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historyChannels: Flow<List<ChannelEntity>> = activePlaylist.flatMapLatest { playlist ->
        if (playlist != null) {
            dao.getHistoryChannels(playlist.id)
        } else {
            emptyFlow()
        }
    }.flowOn(Dispatchers.IO)

    fun getChannelsByGroup(groupTitle: String): Flow<List<ChannelEntity>> {
        return activePlaylist.flatMapLatest { playlist ->
            if (playlist != null) {
                if (groupTitle == "All" || groupTitle.lowercase() == "all channels") {
                    dao.getChannelsForPlaylist(playlist.id)
                } else {
                    dao.getChannelsByGroup(playlist.id, groupTitle)
                }
            } else {
                emptyFlow()
            }
        }.flowOn(Dispatchers.IO)
    }

    fun searchChannels(query: String): Flow<List<ChannelEntity>> {
        return activePlaylist.flatMapLatest { playlist ->
            if (playlist != null) {
                dao.searchChannels(playlist.id, "%$query%")
            } else {
                emptyFlow()
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addFavorite(channelId: String) {
        val active = dao.getActivePlaylist() ?: return
        dao.addFavorite(FavoriteEntity(playlistId = active.id, channelId = channelId))
    }

    suspend fun removeFavorite(channelId: String) {
        val active = dao.getActivePlaylist() ?: return
        dao.removeFavorite(playlistId = active.id, channelId = channelId)
    }

    fun isFavorite(channelId: String): Flow<Boolean> {
        return activePlaylist.flatMapLatest { playlist ->
            if (playlist != null) {
                dao.isFavorite(playlist.id, channelId)
            } else {
                emptyFlow()
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addHistory(channelId: String) {
        val active = dao.getActivePlaylist() ?: return
        dao.addHistoryEntry(HistoryEntity(playlistId = active.id, channelId = channelId))
    }

    suspend fun deleteHistory(channelId: String) {
        val active = dao.getActivePlaylist() ?: return
        dao.deleteHistoryEntry(playlistId = active.id, channelId = channelId)
    }

    suspend fun clearHistory() {
        val active = dao.getActivePlaylist() ?: return
        dao.clearHistory(active.id)
    }

    suspend fun addNewPlaylist(playlist: PlaylistEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // Insert playlist into DB to get generated ID
            val insertedId = dao.insertPlaylist(playlist).toInt()
            val finalPlaylist = playlist.copy(id = insertedId)
            
            // Try fetching channels to verify the connection
            val success = refreshPlaylistCache(finalPlaylist)
            if (success) {
                // Set as active if it loaded successfully
                dao.setActivePlaylist(insertedId)
                return@withContext true
            } else {
                // If validation failed, delete playlist
                dao.deletePlaylist(finalPlaylist)
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save playlist", e)
            false
        }
    }

    suspend fun addNewPlaylistDirect(playlist: PlaylistEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getAllPlaylists().firstOrNull() ?: emptyList()
            if (existing.any { it.url == playlist.url }) {
                return@withContext true
            }

            val insertedId = dao.insertPlaylist(playlist).toInt()
            val finalPlaylist = playlist.copy(id = insertedId)
            dao.setActivePlaylist(insertedId)

            // Attempt to fetch but keep on failure (perfect onboarding robustness)
            refreshPlaylistCache(finalPlaylist)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to save playlist directly", e)
            false
        }
    }

    suspend fun switchAndActivatePlaylist(playlistId: Int) = withContext(Dispatchers.IO) {
        dao.setActivePlaylist(playlistId)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlist)
        dao.clearChannelsForPlaylist(playlist.id)
    }

    suspend fun refreshActivePlaylist(): Boolean = withContext(Dispatchers.IO) {
        val active = dao.getActivePlaylist() ?: return@withContext false
        refreshPlaylistCache(active)
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        val active = dao.getActivePlaylist() ?: return@withContext
        dao.clearChannelsForPlaylist(active.id)
    }

    private suspend fun refreshPlaylistCache(playlist: PlaylistEntity): Boolean = withContext(Dispatchers.IO) {
        if (playlist.type == "m3u") {
            loadM3UPlaylist(playlist)
        } else {
            loadXtreamPlaylist(playlist)
        }
    }

    private suspend fun loadM3UPlaylist(playlist: PlaylistEntity): Boolean {
        val url = playlist.url ?: return false
        val request = Request.Builder().url(url).build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(tag, "M3U request failed: ${response.code}")
                    return false
                }
                val body = response.body?.string() ?: return false
                if (!body.contains("#EXTM3U")) {
                    Log.e(tag, "Invalid M3U data: Missing #EXTM3U tag")
                    return false
                }
                
                // Parse channels
                val channels = M3UParser.parse(body, playlist.id)
                if (channels.isEmpty()) {
                    Log.w(tag, "Parsed 0 channels from M3U playlist")
                } else {
                    Log.d(tag, "Parsed ${channels.size} channels successfully")
                }
                
                // Save in DB
                dao.replaceChannelsForPlaylist(playlist.id, channels)
                true
            }
        } catch (e: Exception) {
            Log.e(tag, "M3U playlist load exception", e)
            false
        }
    }

    private suspend fun loadXtreamPlaylist(playlist: PlaylistEntity): Boolean {
        val host = playlist.host ?: return false
        val username = playlist.username ?: return false
        val password = playlist.password ?: return false

        // 1. Authenticate user
        val authUrl = "$host/player_api.php?username=$username&password=$password"
        val authRequest = Request.Builder().url(authUrl).build()

        try {
            okHttpClient.newCall(authRequest).execute().use { response ->
                if (!response.isSuccessful) return false
                val authBody = response.body?.string() ?: return false
                val jsonObj = JSONObject(authBody)
                
                // If it contains "user_info", auth was successful
                if (!jsonObj.has("user_info")) {
                    Log.e(tag, "Xtream codes authentication failed")
                    return false
                }
                
                val userInfo = jsonObj.getJSONObject("user_info")
                val authStatus = userInfo.optInt("auth", 0)
                if (authStatus != 1 && userInfo.optString("status", "") != "Active") {
                    Log.e(tag, "Xtream auth status inactive")
                    return false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Xtream Auth Exception", e)
            return false
        }

        // 2. Fetch categories
        val categoriesMap = mutableMapOf<String, String>()
        val catUrl = "$host/player_api.php?username=$username&password=$password&action=get_live_categories"
        val catRequest = Request.Builder().url(catUrl).build()

        try {
            okHttpClient.newCall(catRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val catBody = response.body?.string()
                    if (catBody != null) {
                        val arr = JSONArray(catBody)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val catId = obj.optString("category_id")
                            val catName = obj.optString("category_name")
                            if (catId.isNotEmpty() && catName.isNotEmpty()) {
                                categoriesMap[catId] = catName
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Xtream categories fetching failed", e)
        }

        // 3. Fetch live streams
        val streamsUrl = "$host/player_api.php?username=$username&password=$password&action=get_live_streams"
        val streamsRequest = Request.Builder().url(streamsUrl).build()

        return try {
            okHttpClient.newCall(streamsRequest).execute().use { response ->
                if (!response.isSuccessful) return false
                val streamsBody = response.body?.string() ?: return false
                val streamArray = JSONArray(streamsBody)
                val channels = mutableListOf<ChannelEntity>()

                for (i in 0 until streamArray.length()) {
                    val obj = streamArray.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val logoUrl = obj.optString("stream_icon").ifEmpty { null }
                    val categoryId = obj.optString("category_id")
                    
                    val groupTitle = categoriesMap[categoryId] ?: "Other"
                    
                    // Xtream play URL: http://<domain_and_port>/live/<username>/<password>/<stream_id>.ts
                    // Ensure the / is correct between host and live
                    val formattedHost = if (host.endsWith("/")) host.substring(0, host.length - 1) else host
                    val playUrl = "$formattedHost/live/$username/$password/$streamId.ts"

                    val uniqueId = "playlist_${playlist.id}_stream_${streamId}"
                    channels.add(
                        ChannelEntity(
                            id = uniqueId,
                            playlistId = playlist.id,
                            name = name,
                            url = playUrl,
                            logoUrl = logoUrl,
                            groupTitle = groupTitle,
                            tvgId = streamId
                        )
                    )
                }

                // Cache Channels in DB
                dao.replaceChannelsForPlaylist(playlist.id, channels)
                true
            }
        } catch (e: Exception) {
            Log.e(tag, "Xtream streams load error", e)
            false
        }
    }
}
