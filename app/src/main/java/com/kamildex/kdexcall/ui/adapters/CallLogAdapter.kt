package com.kamildex.kdexcall.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kamildex.kdexcall.R
import com.kamildex.kdexcall.databinding.ItemCallLogBinding
import com.kamildex.kdexcall.utils.CallDirection
import com.kamildex.kdexcall.utils.CallEntry
import com.kamildex.kdexcall.utils.CallLog

class CallLogAdapter(
    private val items: List<CallEntry>,
    private val onCall: (String) -> Unit
) : RecyclerView.Adapter<CallLogAdapter.VH>() {

    inner class VH(val b: ItemCallLogBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemCallLogBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        items[i].also { e ->
            h.b.tvName.text = e.name ?: e.number
            h.b.tvNumber.text = if (e.name != null) e.number else ""
            h.b.tvTime.text = "${e.time} · ${e.date}"
            h.b.tvDuration.text = if (e.duration > 0) CallLog.formatDuration(e.duration) else ""
            h.b.ivDirection.setImageResource(when (e.direction) {
                CallDirection.INCOMING -> R.drawable.ic_call_incoming
                CallDirection.OUTGOING -> R.drawable.ic_call_outgoing
                CallDirection.MISSED -> R.drawable.ic_call_missed
            })
            h.b.ivDirectionColor.setColorFilter(h.itemView.context.getColor(when (e.direction) {
                CallDirection.INCOMING -> R.color.green
                CallDirection.OUTGOING -> R.color.primary
                CallDirection.MISSED -> R.color.red
            }))
            h.b.btnCall.setOnClickListener { onCall(e.number) }
            if (e.recordingPath != null) {
                h.b.ivRecording.visibility = android.view.View.VISIBLE
            } else {
                h.b.ivRecording.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount() = items.size
}