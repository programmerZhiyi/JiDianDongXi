package com.example.memoapp.data

import androidx.room.*

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY createdAt DESC")
    suspend fun getAllMemos(): List<Memo>

    @Insert
    suspend fun insert(memo: Memo)

    @Delete
    suspend fun delete(memo: Memo)

    @Update
    suspend fun update(memo: Memo)
} 