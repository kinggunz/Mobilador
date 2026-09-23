package com.mobibawah.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobibawah.app.R
import com.mobibawah.app.model.AppInfo

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onSelected: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    private var selectedPosition = -1

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iconApp)
        val label: TextView = view.findViewById(R.id.labelApp)
        val radio: RadioButton = view.findViewById(R.id.radioApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.radio.isChecked = position == selectedPosition
        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onSelected(app)
        }
    }

    override fun getItemCount() = apps.size
}
