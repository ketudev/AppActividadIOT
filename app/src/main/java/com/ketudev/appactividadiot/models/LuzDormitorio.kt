package com.ketudev.appactividadiot.models

import com.google.firebase.firestore.DocumentId

data class LuzDormitorio(
    @DocumentId val id: String = "",
    val habitacion: String = "",
    val consumoWatts: Int = 0,
    val estado: String = "Encendida"
)
