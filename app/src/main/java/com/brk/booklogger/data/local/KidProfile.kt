package com.brk.booklogger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A household reader (child or adult). Table name kept as kid_profiles for migrations.
 */
@Entity(tableName = "kid_profiles")
data class KidProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "📚",
    val gender: String = KidGender.PREFER_NOT_TO_SAY.name,
    val dateOfBirth: Long? = null,
    val favoriteGenre: String = "",
    val notes: String = "",
    val profileType: String = ReaderProfileType.CHILD.name,
    /** Stable id for multi-device / partner household sync. */
    val cloudId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val firstName: String get() = name.trim().split(" ").firstOrNull().orEmpty().ifBlank { name }

    val genderLabel: String
        get() = KidGender.entries.find { it.name == gender }?.label ?: gender

    val readerType: ReaderProfileType
        get() = ReaderProfileType.fromStorage(profileType)

    val isAdult: Boolean get() = readerType == ReaderProfileType.ADULT

    val typeLabel: String get() = readerType.label
}
