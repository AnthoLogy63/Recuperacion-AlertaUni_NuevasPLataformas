package com.erns.alertauni.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erns.alertauni.data.model.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity): Long

    @Query("SELECT * FROM drafts WHERE ownerKey = :owner ORDER BY updatedAt DESC")
    fun observe(owner: String): Flow<List<DraftEntity>>

    @Query("DELETE FROM drafts WHERE id = :id AND ownerKey = :owner")
    suspend fun delete(id: Long, owner: String)
}
