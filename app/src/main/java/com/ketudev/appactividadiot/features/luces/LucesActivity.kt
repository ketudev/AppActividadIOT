package com.ketudev.appactividadiot.features.luces

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ketudev.appactividadiot.R
import com.ketudev.appactividadiot.databinding.ActivityLucesBinding
import com.ketudev.appactividadiot.databinding.DialogLuzBinding
import com.ketudev.appactividadiot.models.LuzDormitorio

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
        val estados = arrayOf(getString(R.string.estado_encendida), getString(R.string.estado_apagada))
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        dialogBinding.actvEstado.setAdapter(dropdownAdapter)
        dialogBinding.actvEstado.setText(estados[0], false)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_luz_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val habitacion = dialogBinding.etHabitacion.text?.toString()?.trim().orEmpty()
                val watts = dialogBinding.etConsumoWatts.text?.toString()?.trim().orEmpty()
                val estado = dialogBinding.actvEstado.text?.toString()?.trim().orEmpty()
                viewModel.addLuz(habitacion, watts, estado)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showEditDialog(luz: LuzDormitorio) {
        val dialogBinding = DialogLuzBinding.inflate(layoutInflater)
        val estados = arrayOf(getString(R.string.estado_encendida), getString(R.string.estado_apagada))
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        dialogBinding.actvEstado.setAdapter(dropdownAdapter)

        dialogBinding.etHabitacion.setText(luz.habitacion)
        dialogBinding.etConsumoWatts.setText(luz.consumoWatts.toString())
        val currentEstado = if (luz.estado.equals(getString(R.string.estado_apagada), ignoreCase = true)) {
            getString(R.string.estado_apagada)
        } else {
            getString(R.string.estado_encendida)
        }
        dialogBinding.actvEstado.setText(currentEstado, false)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_luz_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val habitacion = dialogBinding.etHabitacion.text?.toString()?.trim().orEmpty()
                val watts = dialogBinding.etConsumoWatts.text?.toString()?.trim().orEmpty()
                val estado = dialogBinding.actvEstado.text?.toString()?.trim().orEmpty()
                viewModel.updateLuz(luz.id, habitacion, watts, estado)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(luz: LuzDormitorio) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, luz.habitacion))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteLuz(luz.id)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
