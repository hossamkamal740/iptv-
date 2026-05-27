package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "m3u" or "xtream"
    val url: String? = null,
    val host: String? = null,
    val username: String? = null,
    val password: String? = null,
    val isActive: Boolean = false
)

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["groupTitle"])
    ]
)
data class ChannelEntity(
    @PrimaryKey val id: String, // Generate as "playlistId_index"
    val playlistId: Int,
    val name: String,
    val url: String,
    val logoUrl: String?,
    val groupTitle: String,
    val tvgId: String?
)

@Entity(tableName = "favorites", primaryKeys = ["playlistId", "channelId"])
data class FavoriteEntity(
    val playlistId: Int,
    val channelId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history", primaryKeys = ["playlistId", "channelId"])
data class HistoryEntity(
    val playlistId: Int,
    val channelId: String,
    val timestamp: Long = System.currentTimeMillis()
)
