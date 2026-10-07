package com.ketudev.appactividadiot.features.luces

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.ketudev.appactividadiot.data.services.LucesService
import com.ketudev.appactividadiot.models.LuzDormitorio
import kotlinx.coroutines.launch

class LucesViewModel : ViewModel() {

    private val lucesService = LucesService()

    private val _luces = MutableLiveData<List<LuzDormitorio>>()
    val luces: LiveData<List<LuzDormitorio>> = _luces

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private var listenerRegistration: ListenerRegistration? = null

    fun startListening() {
        _loading.value = true
        viewModelScope.launch {
            try {
                lucesService.seedInitialData()
            } catch (e: Exception) {
                // Ignore seed error
            }
        }
        listenerRegistration?.remove()
        listenerRegistration = lucesService.listenLuces(
            onUpdate = { list ->
                _loading.value = false
                _luces.value = list
            },
            onError = { e ->
                _loading.value = false
                _error.value = e.localizedMessage ?: "Error al obtener datos"
            }
        )
    }

    fun addLuz(habitacion: String, wattsStr: String, estado: String) {
        if (habitacion.isBlank() || wattsStr.isBlank() || estado.isBlank()) {
            _error.value = "Todos los campos son obligatorios"
            return
        }
        val watts = wattsStr.toIntOrNull()
        if (watts == null || watts < 0) {
            _error.value = "Ingresa un consumo válido en Watts"
            return
        }
        viewModelScope.launch {
            try {
                lucesService.addLuz(LuzDormitorio(habitacion = habitacion, consumoWatts = watts, estado = estado))
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Error al guardar"
            }
        }
    }

    fun updateLuz(id: String, habitacion: String, wattsStr: String, estado: String) {
        if (habitacion.isBlank() || wattsStr.isBlank() || estado.isBlank()) {
            _error.value = "Todos los campos son obligatorios"
            return
        }
        val watts = wattsStr.toIntOrNull()
        if (watts == null || watts < 0) {
            _error.value = "Ingresa un consumo válido en Watts"
            return
        }
        viewModelScope.launch {
            try {
                lucesService.updateLuz(LuzDormitorio(id = id, habitacion = habitacion, consumoWatts = watts, estado = estado))
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Error al actualizar"
            }
        }
    }

    fun deleteLuz(id: String) {
        viewModelScope.launch {
            try {
                lucesService.deleteLuz(id)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Error al eliminar"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
