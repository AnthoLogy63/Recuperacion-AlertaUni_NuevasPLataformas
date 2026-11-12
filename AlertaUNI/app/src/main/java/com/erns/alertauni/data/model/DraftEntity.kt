package com.erns.alertauni.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Borrador de anuncio guardado localmente (Room). Pertenece a una cuenta (ownerKey). */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerKey: String,
    val courseId: String,
    val audience: String,            // "COURSE" | "STUDENT"
    val studentId: String? = null,
    val studentFirstname: String? = null,
    val studentSurname: String? = null,
    val priority: String = "medium",
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
