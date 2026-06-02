package com.kronkollen.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kronkollen.data.entity.KeywordRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rules ORDER BY keyword")
    fun observeAll(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules ORDER BY keyword")
    suspend fun getAll(): List<KeywordRuleEntity>

    @Query("SELECT * FROM keyword_rules WHERE categoryId = :categoryId ORDER BY keyword")
    fun observeForCategory(categoryId: Long): Flow<List<KeywordRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: KeywordRuleEntity): Long

    @Delete
    suspend fun delete(rule: KeywordRuleEntity)

    @Query("DELETE FROM keyword_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM keyword_rules")
    suspend fun deleteAll()
}
