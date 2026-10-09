package com.sepi.market.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.UiState
import com.sepi.market.core.gone
import com.sepi.market.core.showIf
import com.sepi.market.core.visible
import com.sepi.market.databinding.ActivityAuthBinding
import com.sepi.market.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Layar Autentikasi (Masuk & Daftar) aplikasi Sepi.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    private val viewModel: AuthViewModel by viewModels {
        (application as SepiApp).appContainer.viewModelFactory
    }

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupListeners()
        observeViewModel()
    }

    private fun setupTabs() {
        binding.tabAuthMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isSignUpMode = tab?.position == 1
                updateUiMode()
                viewModel.clearError()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateUiMode() {
        binding.tilFullName.showIf(isSignUpMode)
        binding.tilConfirmPassword.showIf(isSignUpMode)
        binding.btnSubmit.text = if (isSignUpMode) {
            getString(R.string.btn_sign_up)
        } else {
            getString(R.string.btn_sign_in)
        }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            if (isSignUpMode) {
                viewModel.signUp(
                    fullName = binding.etFullName.text?.toString().orEmpty(),
                    email = binding.etEmail.text?.toString().orEmpty(),
                    pass = binding.etPassword.text?.toString().orEmpty(),
                    confirmPass = binding.etConfirmPassword.text?.toString().orEmpty()
                )
            } else {
                viewModel.signIn(
                    email = binding.etEmail.text?.toString().orEmpty(),
                    pass = binding.etPassword.text?.toString().orEmpty()
                )
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.authEvent.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<Unit>) {
        when (state) {
            is UiState.Loading -> {
                setFormEnabled(false)
                binding.btnSubmit.text = ""
                binding.pbLoading.visible()
                binding.tvServerError.gone()
            }
            is UiState.Success -> {
                setFormEnabled(true)
                binding.btnSubmit.text = if (isSignUpMode) getString(R.string.btn_sign_up) else getString(R.string.btn_sign_in)
                binding.pbLoading.gone()
                binding.tvServerError.gone()
            }
            is UiState.Error -> {
                setFormEnabled(true)
                binding.btnSubmit.text = if (isSignUpMode) getString(R.string.btn_sign_up) else getString(R.string.btn_sign_in)
                binding.pbLoading.gone()
                binding.tvServerError.text = state.message
                binding.tvServerError.visible()
            }
        }
    }

    private fun handleEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.NavigateToMain -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            is AuthUiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is AuthUiEvent.ShowInfoDialog -> {
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        binding.tabAuthMode.getTabAt(0)?.select()
                    }
                    .show()
            }
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.tabAuthMode.isEnabled = enabled
        binding.etFullName.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.etPassword.isEnabled = enabled
        binding.etConfirmPassword.isEnabled = enabled
        binding.btnSubmit.isEnabled = enabled
    }
}
