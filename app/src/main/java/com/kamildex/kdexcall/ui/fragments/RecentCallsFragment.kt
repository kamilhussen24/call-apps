package com.kamildex.kdexcall.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kamildex.kdexcall.databinding.FragmentRecentCallsBinding
import com.kamildex.kdexcall.ui.adapters.CallLogAdapter
import com.kamildex.kdexcall.utils.CallLog
import com.kamildex.kdexcall.utils.Prefs
import com.kamildex.kdexcall.sip.SipManager
import com.kamildex.kdexcall.ui.CallActivity

class RecentCallsFragment : Fragment() {

    private var _binding: FragmentRecentCallsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecentCallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCalls()
    }

    private fun loadCalls() {
        val calls = CallLog.getAll(requireContext())
        if (calls.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvCalls.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvCalls.visibility = View.VISIBLE
            binding.rvCalls.layoutManager = LinearLayoutManager(requireContext())
            binding.rvCalls.adapter = CallLogAdapter(calls) { number ->
                val domain = Prefs.getSipDomain(requireContext())
                if (SipManager.call(number, domain)) {
                    startActivity(Intent(requireContext(), CallActivity::class.java).apply {
                        putExtra("number", number)
                    })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCalls()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}