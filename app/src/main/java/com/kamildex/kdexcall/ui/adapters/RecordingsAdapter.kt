package com.kamildex.kdexcall.ui.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kamildex.kdexcall.databinding.ItemRecordingBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingsAdapter(
    private val files: List<File>
) : RecyclerView.Adapter<RecordingsAdapter.VH>() {

    private var mediaPlayer: MediaPlayer? = null
    private var playingPosition = -1

    inner class VH(val b: ItemRecordingBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemRecordingBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        files[i].also { f ->
            h.b.tvFileName.text = f.nameWithoutExtension
            h.b.tvDate.text = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(Date(f.lastModified()))
            h.b.tvSize.text = "${f.length() / 1024} KB"

            val isPlaying = playingPosition == i
            h.b.btnPlay.text = if (isPlaying) "Stop" else "Play"

            h.b.btnPlay.setOnClickListener {
                if (isPlaying) {
                    stopPlaying()
                } else {
                    playFile(f, i)
                }
                notifyDataSetChanged()
            }

            h.b.btnDelete.setOnClickListener {
                f.delete()
                notifyDataSetChanged()
            }
        }
    }

    private fun playFile(file: File, position: Int) {
        stopPlaying()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener {
                playingPosition = -1
                notifyDataSetChanged()
            }
        }
        playingPosition = position
    }

    private fun stopPlaying() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        playingPosition = -1
    }

    override fun getItemCount() = files.size
}