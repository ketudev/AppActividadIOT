package com.ketudev.appactividadiot.features.luces

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ketudev.appactividadiot.R
import com.ketudev.appactividadiot.databinding.ItemLuzBinding
import com.ketudev.appactividadiot.models.LuzDormitorio

class LucesAdapter(
    private val onItemClick: (LuzDormitorio) -> Unit,
    private val onDeleteClick: (LuzDormitorio) -> Unit
) : ListAdapter<LuzDormitorio, LucesAdapter.LuzViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LuzViewHolder {
        val binding = ItemLuzBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LuzViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LuzViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LuzViewHolder(private val binding: ItemLuzBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(luz: LuzDormitorio) {
            val context = binding.root.context
            binding.tvHabitacion.text = luz.habitacion
            binding.tvInfo.text = context.getString(
                R.string.luz_info_format,
                luz.consumoWatts,
                luz.estado
            )
            binding.root.setOnClickListener { onItemClick(luz) }
            binding.btnDelete.setOnClickListener { onDeleteClick(luz) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LuzDormitorio>() {
            override fun areItemsTheSame(oldItem: LuzDormitorio, newItem: LuzDormitorio): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LuzDormitorio, newItem: LuzDormitorio): Boolean {
                return oldItem == newItem
            }
        }
    }
}
