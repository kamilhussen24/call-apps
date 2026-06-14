package com.kamildex.kdexcall.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kamildex.kdexcall.R
import com.kamildex.kdexcall.databinding.ItemCallLogBinding
import com.kamildex.kdexcall.utils.CallDirection
import com.kamildex.kdexcall.utils.CallEntry
import com.kamildex.kdexcall.utils.CallLog
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

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
            val (iconRes, colorRes) = when (e.direction) {
                CallDirection.INCOMING -> Pair(R.drawable.ic_call_incoming, R.color.green)
                CallDirection.OUTGOING -> Pair(R.drawable.ic_call_outgoing, R.color.primary)
                CallDirection.MISSED -> Pair(R.drawable.ic_call_missed, R.color.red)
            }
            h.b.ivDirection.setImageResource(iconRes)
            h.b.ivDirection.colorFilter = PorterDuffColorFilter(
                h.itemView.context.getColor(colorRes), PorterDuff.Mode.SRC_IN
            )
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