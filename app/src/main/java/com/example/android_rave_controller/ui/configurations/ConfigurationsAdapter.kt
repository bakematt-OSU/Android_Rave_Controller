package com.example.android_rave_controller.ui.configurations

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.android_rave_controller.R
import com.example.android_rave_controller.databinding.ListItemConfigurationBinding

class ConfigurationsAdapter(
    private var files: MutableList<String>,
    private val listener: OnConfigInteractionListener
) : RecyclerView.Adapter<ConfigurationsAdapter.ConfigViewHolder>() {

    interface OnConfigInteractionListener {
        fun onLoad(filename: String)
        fun onRename(oldFilename: String)
        fun onDelete(filename: String)
    }

    class ConfigViewHolder(private val binding: ListItemConfigurationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(filename: String, listener: OnConfigInteractionListener) {
            binding.configNameTextView.text = filename

            // Set individual listeners for each icon button
            binding.buttonLoadConfig.setOnClickListener { listener.onLoad(filename) }
            binding.buttonRenameConfig.setOnClickListener { listener.onRename(filename) }
            binding.buttonDeleteConfig.setOnClickListener { listener.onDelete(filename) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
        val binding = ListItemConfigurationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConfigViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        holder.bind(files[position], listener)
    }

    override fun getItemCount(): Int = files.size

    fun updateData(newFiles: List<String>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }
}