package com.kamildex.kdexcall.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kamildex.kdexcall.databinding.ItemContactBinding
import com.kamildex.kdexcall.ui.fragments.Contact

class ContactsAdapter(
    private val items: List<Contact>,
    private val onCall: (String) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.VH>() {

    inner class VH(val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemContactBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        items[i].also { c ->
            h.b.tvName.text = c.name
            h.b.tvNumber.text = c.number
            h.b.tvInitial.text = c.name.firstOrNull()?.uppercase() ?: "?"
            h.b.btnCall.setOnClickListener { onCall(c.number) }
            h.itemView.setOnClickListener { onCall(c.number) }
        }
    }

    override fun getItemCount() = items.size
}