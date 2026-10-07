package com.ketudev.appactividadiot.data.services

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ketudev.appactividadiot.models.LuzDormitorio
import com.ketudev.appactividadiot.models.User
import kotlinx.coroutines.tasks.await

class LucesService {

    private val db = FirebaseFirestore.getInstance()
    private val collectionLuces = db.collection("luces_dormitorio")
    private val collectionUsuarios = db.collection("usuarios")

    suspend fun saveUser(user: User) {
        if (user.id.isNotEmpty()) {
            collectionUsuarios.document(user.id).set(user).await()
        }
    }

    fun listenLuces(onUpdate: (List<LuzDormitorio>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return collectionLuces.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(LuzDormitorio::class.java)
                onUpdate(list)
            }
        }
    }

    suspend fun addLuz(luz: LuzDormitorio) {
        collectionLuces.add(luz).await()
    }

    suspend fun updateLuz(luz: LuzDormitorio) {
        collectionLuces.document(luz.id).set(luz).await()
    }

    suspend fun deleteLuz(id: String) {
        collectionLuces.document(id).delete().await()
    }

    suspend fun seedInitialData() {
        val countSnapshot = collectionLuces.get().await()
        if (countSnapshot.isEmpty) {
            val initialList = listOf(
                LuzDormitorio(habitacion = "Dormitorio Principal (Techo)", consumoWatts = 60, estado = "Encendida"),
                LuzDormitorio(habitacion = "Dormitorio Principal (Mesa Noche)", consumoWatts = 15, estado = "Apagada"),
                LuzDormitorio(habitacion = "Dormitorio Infantil", consumoWatts = 40, estado = "Encendida"),
                LuzDormitorio(habitacion = "Dormitorio Visitas", consumoWatts = 25, estado = "Apagada"),
                LuzDormitorio(habitacion = "Dormitorio Suite", consumoWatts = 75, estado = "Apagada")
            )
            for (item in initialList) {
                collectionLuces.add(item).await()
            }
        }
    }
}
