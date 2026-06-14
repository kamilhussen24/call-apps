package com.kamildex.kdexcall.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kamildex.kdexcall.databinding.FragmentRecordingsBinding
import com.kamildex.kdexcall.ui.adapters.RecordingsAdapter
import java.io.File

class RecordingsFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRecordings()
    }

    private fun loadRecordings() {
        val dir = requireContext().getExternalFilesDir("recordings") ?: requireContext().filesDir
        val files = dir.listFiles { f -> f.extension == "wav" || f.extension == "mp3" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvRecordings.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvRecordings.visibility = View.VISIBLE
            binding.rvRecordings.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecordings.adapter = RecordingsAdapter(files)
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecordings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}