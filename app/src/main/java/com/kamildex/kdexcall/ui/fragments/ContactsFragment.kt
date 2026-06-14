package com.kamildex.kdexcall.ui.fragments

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kamildex.kdexcall.databinding.FragmentContactsBinding
import com.kamildex.kdexcall.ui.adapters.ContactsAdapter
import com.kamildex.kdexcall.utils.Prefs
import com.kamildex.kdexcall.sip.SipManager
import com.kamildex.kdexcall.ui.CallActivity

data class Contact(val name: String, val number: String)

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadContacts()
    }

    private fun loadContacts() {
        val contacts = mutableListOf<Contact>()
        try {
            val cursor: Cursor? = requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0) ?: continue
                    val number = it.getString(1)?.replace(" ", "") ?: continue
                    contacts.add(Contact(name, number))
                }
            }
        } catch (e: Exception) {}

        if (contacts.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
            binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
            binding.rvContacts.adapter = ContactsAdapter(contacts) { number ->
                val domain = Prefs.getSipDomain(requireContext())
                if (SipManager.call(number, domain)) {
                    startActivity(Intent(requireContext(), CallActivity::class.java).apply {
                        putExtra("number", number)
                    })
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}