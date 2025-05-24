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
    private val listener: OnTemplateActionClickListener
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    interface OnTemplateActionClickListener {
        fun onDeleteTemplate(templateId: String, templateName: String)
        fun onSelectTemplate(templateId: String, templateName: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = templates[position]
        holder.bind(template, listener)
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
        private val selectButton: Button = itemView.findViewById(R.id.selectTemplateButton)

        fun bind(template: GameTemplateInfo, listener: OnTemplateActionClickListener) {
            nameTextView.text = template.name
            descriptionTextView.text = template.description ?: itemView.context.getString(R.string.template_item_no_description)
            versionTextView.text = itemView.context.getString(R.string.template_item_version_prefix, template.version ?: itemView.context.getString(R.string.template_item_version_na_suffix))
            pathTextView.text = itemView.context.getString(R.string.template_item_path_prefix, template.unzippedPath)
            originalZipTextView.text = itemView.context.getString(R.string.template_item_original_prefix, template.originalZipName ?: itemView.context.getString(R.string.template_item_original_na_suffix))

            deleteButton.setOnClickListener { listener.onDeleteTemplate(template.id, template.name) }
            selectButton.setOnClickListener { listener.onSelectTemplate(template.id, template.name) }
        }
    }
}
