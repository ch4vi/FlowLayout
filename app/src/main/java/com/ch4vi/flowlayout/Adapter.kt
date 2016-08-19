package com.ch4vi.flowlayout

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class Adapter(private val data: Array<String>) : RecyclerView.Adapter<Adapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.primaryTag.text = data[position]
//        holder.secondaryTag.text = "## ${data[position]}"
//        holder.membersCount.text = "$position"

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val primaryTag: TextView = view.findViewById(R.id.primary_tag) as TextView
//        val secondaryTag: TextView = view.findViewById(R.id.secondary_tag) as TextView
//        val membersCount: TextView = view.findViewById(R.id.members_count) as TextView
    }
}
