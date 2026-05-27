package com.example.data

import android.util.Log
import java.io.BufferedReader
import java.io.StringReader

object M3UParser {
    private const val TAG = "M3UParser"

    fun parse(m3uContent: String, playlistId: Int): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val reader = BufferedReader(StringReader(m3uContent))
        
        var currentLine: String?
        var currentChannelName = ""
        var logoUrl: String? = null
        var groupTitle = "Other"
        var tvgId: String? = null
        var index = 0

        // Attribute extraction regex: key="value"
        val attrRegex = """([a-zA-Z0-9_-]+)="([^"]*)"""".toRegex()

        try {
            while (reader.readLine().also { currentLine = it } != null) {
                val line = currentLine!!.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF:")) {
                    // Reset variables for new channel entry
                    currentChannelName = ""
                    logoUrl = null
                    groupTitle = "Other"
                    tvgId = null

                    // 1. Parse name: It is everything after the last comma
                    val commaIndex = line.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex < line.length - 1) {
                        currentChannelName = line.substring(commaIndex + 1).trim()
                    }

                    // 2. Parse attributes
                    val matches = attrRegex.findAll(line)
                    for (match in matches) {
                        val key = match.groupValues[1].lowercase()
                        val value = match.groupValues[2].trim()
                        
                        when (key) {
                            "group-title" -> if (value.isNotEmpty()) groupTitle = value
                            "tvg-logo" -> if (value.isNotEmpty()) logoUrl = value
                            "tvg-id" -> if (value.isNotEmpty()) tvgId = value
                        }
                    }
                    
                    if (currentChannelName.isEmpty()) {
                        // If no comma found or channel name empty, try to get some fallback name from attributes
                        currentChannelName = tvgId ?: "Channel ${index + 1}"
                    }
                } else if (!line.startsWith("#")) {
                    // This is the URL line which follows #EXTINF
                    val url = line
                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("rtmp://") || url.startsWith("rtsp://")) {
                        val uniqueId = "playlist_${playlistId}_channel_${index++}"
                        channels.add(
                            ChannelEntity(
                                id = uniqueId,
                                playlistId = playlistId,
                                name = currentChannelName.ifEmpty { "Channel $index" },
                                url = url,
                                logoUrl = logoUrl,
                                groupTitle = groupTitle.ifEmpty { "Other" },
                                tvgId = tvgId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U content", e)
        } finally {
            reader.close()
        }

        return channels
    }
}
