package com.booklog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kid_profiles")
data class KidProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "📚",
    val gender: String = KidGender.PREFER_NOT_TO_SAY.name,
    val dateOfBirth: Long? = null,
    val favoriteGenre: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val firstName: String get() = name.trim().split(" ").firstOrNull().orEmpty().ifBlank { name }

    val genderLabel: String
        get() = KidGender.entries.find { it.name == gender }?.label ?: gender
}