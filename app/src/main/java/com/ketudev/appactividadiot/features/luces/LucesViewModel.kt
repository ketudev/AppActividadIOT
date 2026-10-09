package com.ketudev.appactividadiot.features.luces

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.ketudev.appactividadiot.data.services.LucesService
import com.ketudev.appactividadiot.models.LuzDormitorio
import com.ketudev.appactividadiot.utils.ErrorSanitizer
import com.ketudev.appactividadiot.utils.ValidationUtils
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
                _error.value = ErrorSanitizer.sanitize(e)
            }
        )
    }

    fun toggleLuzEstado(luz: LuzDormitorio) {
        val newEstado = if (luz.estado.equals("Encendida", ignoreCase = true)) "Apagada" else "Encendida"
        val esRegistroVacio = luz.habitacion.isBlank() && luz.consumoWatts <= 0
        // En un registro vacio solo se alterna el estado, sin inventar un consumo.
        val newWatts = when {
            newEstado == "Apagada" -> 0
            esRegistroVacio -> 0
            luz.consumoWatts in 1..150 -> luz.consumoWatts
            else -> 60
        }
        updateLuzDirect(luz.id, luz.habitacion, newWatts, newEstado)
    }

    private fun updateLuzDirect(id: String, habitacion: String, watts: Int, estado: String) {
        viewModelScope.launch {
            try {
                lucesService.updateLuz(LuzDormitorio(id = id, habitacion = habitacion, consumoWatts = watts, estado = estado))
            } catch (e: Exception) {
                _error.value = ErrorSanitizer.sanitize(e)
            }
        }
    }

    fun addLuz(habitacion: String, wattsStr: String, estado: String) {
        val errors = ValidationUtils.validateLuzForm(habitacion, wattsStr, estado)
        if (!errors.esValido) {
            _error.value = errors.primerError
            return
        }

        val esVacio = ValidationUtils.esFormularioVacio(habitacion, wattsStr, estado)
        val watts = if (esVacio) 0 else wattsStr.trim().toInt()
        viewModelScope.launch {
            try {
                lucesService.addLuz(
                    LuzDormitorio(
                        habitacion = habitacion.trim(),
                        consumoWatts = watts,
                        estado = estado.trim()
                    )
                )
            } catch (e: Exception) {
                _error.value = ErrorSanitizer.sanitize(e)
            }
        }
    }

    fun updateLuz(id: String, habitacion: String, wattsStr: String, estado: String) {
        val errors = ValidationUtils.validateLuzForm(habitacion, wattsStr, estado)
        if (!errors.esValido) {
            _error.value = errors.primerError
            return
        }

        val esVacio = ValidationUtils.esFormularioVacio(habitacion, wattsStr, estado)
        val watts = if (esVacio) 0 else wattsStr.trim().toInt()
        viewModelScope.launch {
            try {
                lucesService.updateLuz(LuzDormitorio(id = id, habitacion = habitacion.trim(), consumoWatts = watts, estado = estado.trim()))
            } catch (e: Exception) {
                _error.value = ErrorSanitizer.sanitize(e)
            }
        }
    }

    fun deleteLuz(id: String) {
        viewModelScope.launch {
            try {
                lucesService.deleteLuz(id)
            } catch (e: Exception) {
                _error.value = ErrorSanitizer.sanitize(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
