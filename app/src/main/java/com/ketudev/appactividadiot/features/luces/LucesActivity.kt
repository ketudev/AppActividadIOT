package com.ketudev.appactividadiot.features.luces

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ketudev.appactividadiot.R
import com.ketudev.appactividadiot.databinding.ActivityLucesBinding
import com.ketudev.appactividadiot.databinding.DialogLuzBinding
import com.ketudev.appactividadiot.models.LuzDormitorio
import com.ketudev.appactividadiot.utils.ValidationUtils

class LucesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLucesBinding
    private val viewModel: LucesViewModel by viewModels()
    private lateinit var adapter: LucesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLucesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupObservers()

        binding.fabAddLuz.setOnClickListener {
            showAddDialog()
        }

        viewModel.startListening()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = LucesAdapter(
            onItemClick = { luz -> showEditDialog(luz) },
            onDeleteClick = { luz -> showDeleteConfirmation(luz) },
            onToggleClick = { luz -> viewModel.toggleLuzEstado(luz) }
        )
        binding.rvLuces.layoutManager = LinearLayoutManager(this)
        binding.rvLuces.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.luces.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmptyState.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogLuzBinding.inflate(layoutInflater)
        setupEstadoDropdown(dialogBinding)
        // Sin estado preseleccionado: así el formulario puede quedar completamente vacío.
        dialogBinding.actvEstado.setText("", false)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_luz_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val habitacion = dialogBinding.etHabitacion.text?.toString().orEmpty()
            val watts = dialogBinding.etConsumoWatts.text?.toString().orEmpty()
            val estado = dialogBinding.actvEstado.text?.toString().orEmpty()

            // Formulario en blanco => registro vacío permitido (sin errores).
            // Formulario con contenido => validaciones lógicas campo a campo.
            val errors = ValidationUtils.validateLuzForm(habitacion, watts, estado)

            if (errors.esValido) {
                viewModel.addLuz(habitacion, watts, estado)
                dialog.dismiss()
            } else {
                dialogBinding.tilHabitacion.error = errors.habitacion
                dialogBinding.tilConsumoWatts.error = errors.watts
                dialogBinding.tilEstado.error = errors.estado
            }
        }
    }

    private fun showEditDialog(luz: LuzDormitorio) {
        val dialogBinding = DialogLuzBinding.inflate(layoutInflater)
        setupEstadoDropdown(dialogBinding)

        val esRegistroVacio = luz.habitacion.isBlank() && luz.consumoWatts <= 0
        dialogBinding.etHabitacion.setText(luz.habitacion)
        dialogBinding.etConsumoWatts.setText(if (luz.consumoWatts > 0) luz.consumoWatts.toString() else "")
        dialogBinding.actvEstado.setText(
            when {
                esRegistroVacio -> ""
                luz.estado.equals(getString(R.string.estado_apagada), ignoreCase = true) ->
                    getString(R.string.estado_apagada)
                else -> getString(R.string.estado_encendida)
            },
            false
        )

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_luz_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val habitacion = dialogBinding.etHabitacion.text?.toString().orEmpty()
            val watts = dialogBinding.etConsumoWatts.text?.toString().orEmpty()
            val estado = dialogBinding.actvEstado.text?.toString().orEmpty()

            // Editar sigue las mismas reglas: vacío permitido, contenido validado.
            val errors = ValidationUtils.validateLuzForm(habitacion, watts, estado)

            if (errors.esValido) {
                viewModel.updateLuz(luz.id, habitacion, watts, estado)
                dialog.dismiss()
            } else {
                dialogBinding.tilHabitacion.error = errors.habitacion
                dialogBinding.tilConsumoWatts.error = errors.watts
                dialogBinding.tilEstado.error = errors.estado
            }
        }
    }

    private fun setupEstadoDropdown(dialogBinding: DialogLuzBinding) {
        val estados = arrayOf(getString(R.string.estado_encendida), getString(R.string.estado_apagada))
        dialogBinding.actvEstado.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        )
    }

    private fun showDeleteConfirmation(luz: LuzDormitorio) {
        val mensaje = if (luz.habitacion.isBlank()) {
            getString(R.string.delete_confirm_message_empty)
        } else {
            getString(R.string.delete_confirm_message, luz.habitacion)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(mensaje)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteLuz(luz.id)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}