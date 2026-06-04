package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_links")
data class RecentLink(
    @PrimaryKey val url: String,
    val title: String,
    val thumbnail: String?,
    val timestamp: Long = System.currentTimeMillis()
)
