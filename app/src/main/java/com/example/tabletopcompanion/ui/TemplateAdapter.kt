package com.example.tabletopcompanion.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tabletopcompanion.R
import com.example.tabletopcompanion.model.GameTemplateInfo

class TemplateAdapter(
    private var templates: List<GameTemplateInfo>,
    private val onDeleteClicked: (GameTemplateInfo) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = templates[position]
        holder.bind(template, onDeleteClicked)
    }

    override fun getItemCount(): Int = templates.size

    fun updateTemplates(newTemplates: List<GameTemplateInfo>) {
        templates = newTemplates
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }

    class TemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.templateNameTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.templateDescriptionTextView)
        private val versionTextView: TextView = itemView.findViewById(R.id.templateVersionTextView)
        private val pathTextView: TextView = itemView.findViewById(R.id.templatePathTextView)
        private val originalZipTextView: TextView = itemView.findViewById(R.id.templateOriginalZipTextView)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteTemplateButton)

        fun bind(template: GameTemplateInfo, onDeleteClicked: (GameTemplateInfo) -> Unit) {
            nameTextView.text = template.name
            descriptionTextView.text = template.description ?: "No description"
            versionTextView.text = "Version: ${template.version ?: "N/A"}"
            pathTextView.text = "Path: ${template.unzippedPath}"
            originalZipTextView.text = "Original: ${template.originalZipName ?: "N/A"}"
            deleteButton.setOnClickListener { onDeleteClicked(template) }
        }
    }
}
