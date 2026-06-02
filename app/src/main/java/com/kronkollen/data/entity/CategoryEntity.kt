package com.kronkollen.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB colour used in charts and chips. */
    val colorArgb: Int,
    val sortOrder: Int = 0,
)
