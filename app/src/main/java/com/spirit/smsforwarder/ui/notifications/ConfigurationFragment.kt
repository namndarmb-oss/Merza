package com.spirit.smsforwarder.ui.notifications

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.spirit.smsforwarder.databinding.FragmentConfigurationBinding

class ConfigurationFragment : Fragment() {

    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!
    private lateinit var configurationViewModel: ConfigurationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        configurationViewModel = ViewModelProvider(this)[ConfigurationViewModel::class.java]
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)

        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        configurationViewModel.telegramToken.observe(viewLifecycleOwner) {
            if (binding.telegramTokenInput.text.toString() != it) {
                binding.telegramTokenInput.setText(it)
            }
        }
        configurationViewModel.userId.observe(viewLifecycleOwner) {
            if (binding.whoToMessageID.text.toString() != it) {
                binding.whoToMessageID.setText(it)
            }
        }

        binding.telegramTokenInput.addTextChangedListener(SimpleTextWatcher {
            configurationViewModel.saveTelegramToken(it)
        })
        binding.whoToMessageID.addTextChangedListener(SimpleTextWatcher {
            configurationViewModel.saveUserId(it)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SimpleTextWatcher(private val onTextChanged: (String) -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        s?.toString()?.let(onTextChanged)
    }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
