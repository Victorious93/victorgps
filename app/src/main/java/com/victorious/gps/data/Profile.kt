package com.victorious.gps.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A quick-switch location profile. [targetPackage] is only a label/shortcut: Android mock
 * location is system-wide, so the location applies to every app, not just the tagged one.
 */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 5f,
    val altitudeMeters: Double = 0.0,
    val targetPackage: String? = null,
    val updatedAt: Long = 0,
)
