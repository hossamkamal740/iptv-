package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IPTVDao {

    // --- PLAYLISTS ---
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlaylist(): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    fun getActivePlaylistFlow(): Flow<PlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun clearActivePlaylists()

    @Transaction
    suspend fun setActivePlaylist(playlistId: Int) {
        clearActivePlaylists()
        updatePlaylistActiveStatus(playlistId, true)
    }

    @Query("UPDATE playlists SET isActive = :isActive WHERE id = :playlistId")
    suspend fun updatePlaylistActiveStatus(playlistId: Int, isActive: Boolean)

    // --- CHANNELS ---
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY name ASC")
    fun getChannelsForPlaylist(playlistId: Int): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId ORDER BY groupTitle ASC")
    fun getGroupsForPlaylist(playlistId: Int): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND groupTitle = :groupTitle ORDER BY name ASC")
    fun getChannelsByGroup(playlistId: Int, groupTitle: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND name LIKE :query ORDER BY name ASC")
    fun searchChannels(playlistId: Int, query: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun clearChannelsForPlaylist(playlistId: Int)

    @Transaction
    suspend fun replaceChannelsForPlaylist(playlistId: Int, channels: List<ChannelEntity>) {
        clearChannelsForPlaylist(playlistId)
        insertChannels(channels)
    }

    // --- FAVORITES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE playlistId = :playlistId AND channelId = :channelId")
    suspend fun removeFavorite(playlistId: Int, channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE playlistId = :playlistId AND channelId = :channelId)")
    fun isFavorite(playlistId: Int, channelId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE playlistId = :playlistId AND channelId = :channelId)")
    suspend fun isFavoriteDirect(playlistId: Int, channelId: String): Boolean

    @Query("SELECT c.* FROM channels c INNER JOIN favorites f ON c.id = f.channelId WHERE f.playlistId = :playlistId ORDER BY f.timestamp DESC")
    fun getFavoriteChannels(playlistId: Int): Flow<List<ChannelEntity>>

    // --- HISTORY ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistoryEntry(history: HistoryEntity)

    @Query("SELECT c.* FROM channels c INNER JOIN history h ON c.id = h.channelId WHERE h.playlistId = :playlistId ORDER BY h.timestamp DESC LIMIT 50")
    fun getHistoryChannels(playlistId: Int): Flow<List<ChannelEntity>>

    @Query("DELETE FROM history WHERE playlistId = :playlistId AND channelId = :channelId")
    suspend fun deleteHistoryEntry(playlistId: Int, channelId: String)

    @Query("DELETE FROM history WHERE playlistId = :playlistId")
    suspend fun clearHistory(playlistId: Int)
}
