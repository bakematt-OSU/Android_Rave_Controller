package com.example.android_rave_controller.ui.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.android_rave_controller.databinding.ListItemEffectBinding
import com.example.android_rave_controller.models.Effect

class EffectsAdapter(
    private var effects: MutableList<Effect>
) : RecyclerView.Adapter<EffectsAdapter.EffectViewHolder>() {

    class EffectViewHolder(private val binding: ListItemEffectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(effect: Effect) {
            binding.effectNameTextView.text = effect.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val binding = ListItemEffectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EffectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        holder.bind(effects[position])
    }

    override fun getItemCount(): Int {
        return effects.size
    }

    fun updateEffects(newEffects: List<Effect>) {
        effects.clear()
        effects.addAll(newEffects)
        notifyDataSetChanged()
    }
}