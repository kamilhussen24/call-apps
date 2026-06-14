package com.kamildex.kdexcall.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kamildex.kdexcall.databinding.FragmentDialpadBinding
import com.kamildex.kdexcall.sip.SipManager
import com.kamildex.kdexcall.ui.CallActivity
import com.kamildex.kdexcall.utils.Prefs

class DialpadFragment : Fragment() {

    private var _binding: FragmentDialpadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialpadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialpad()
    }

    private fun setupDialpad() {
        val keys = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnStar to "*", binding.btnHash to "#"
        )

        keys.forEach { (btn, digit) ->
            btn.setOnClickListener {
                binding.etNumber.append(digit)
            }
            btn.setOnLongClickListener {
                if (digit == "0") { binding.etNumber.append("+"); true }
                else false
            }
        }

        binding.btnBackspace.setOnClickListener {
            val text = binding.etNumber.text.toString()
            if (text.isNotEmpty())
                binding.etNumber.setText(text.dropLast(1))
        }

        binding.btnBackspace.setOnLongClickListener {
            binding.etNumber.setText("")
            true
        }

        binding.btnCall.setOnClickListener {
            val number = binding.etNumber.text.toString().trim()
            if (number.isEmpty()) return@setOnClickListener
            val domain = Prefs.getSipDomain(requireContext())
            if (SipManager.call(number, domain)) {
                startActivity(Intent(requireContext(), CallActivity::class.java).apply {
                    putExtra("number", number)
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}