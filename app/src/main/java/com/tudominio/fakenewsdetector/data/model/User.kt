package com.tudominio.fakenewsdetector.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "user"
) {
    fun isAdmin() = role == "admin"
}
