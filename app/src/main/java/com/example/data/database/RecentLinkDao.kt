package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RecentLink
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentLinkDao {
    @Query("SELECT * FROM recent_links ORDER BY timestamp DESC LIMIT 15")
    fun getAllRecentLinks(): Flow<List<RecentLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentLink(link: RecentLink)

    @Query("DELETE FROM recent_links WHERE url = :url")
    suspend fun deleteRecentLink(url: String)

    @Query("DELETE FROM recent_links")
    suspend fun clearAllRecentLinks()
}
