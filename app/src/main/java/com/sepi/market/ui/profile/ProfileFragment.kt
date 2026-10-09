package com.sepi.market.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.sepi.market.R
import com.sepi.market.SepiApp
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.ProfileDto
import com.sepi.market.databinding.FragmentProfileBinding
import com.sepi.market.ui.auth.AuthActivity
import kotlinx.coroutines.launch

/**
 * Fragment Layar Profil Akun Pengguna (PRD Bagian 9.3 Screen 11).
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        (requireActivity().application as SepiApp).appContainer.viewModelFactory
    }

    private var currentProfile: ProfileDto? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()

        viewModel.loadProfile()
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProfile()
        }

        binding.btnEditProfile.setOnClickListener {
            val profile = currentProfile ?: return@setOnClickListener
            val sheet = EditProfileSheet(
                currentName = profile.fullName,
                currentPhone = profile.phone,
                onSave = { name, phone ->
                    viewModel.updateProfile(name, phone)
                }
            )
            sheet.show(childFragmentManager, "EditProfileSheet")
        }

        // Pintasan Status Pesanan
        binding.shortcutUnpaid.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment, Bundle().apply { putInt("initialTab", 1) })
        }
        binding.shortcutProcess.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment, Bundle().apply { putInt("initialTab", 2) })
        }
        binding.shortcutShipped.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment, Bundle().apply { putInt("initialTab", 3) })
        }
        binding.shortcutDelivered.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment, Bundle().apply { putInt("initialTab", 4) })
        }

        // Menu Pengaturan
        binding.menuAddresses.setOnClickListener {
            findNavController().navigate(R.id.addressesFragment)
        }
        binding.menuFavorites.setOnClickListener {
            findNavController().navigate(R.id.favoritesFragment)
        }
        binding.menuVouchers.setOnClickListener {
            findNavController().navigate(R.id.vouchersFragment)
        }
        binding.menuLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        val appContainer = (requireActivity().application as SepiApp).appContainer

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is ProfileUiEvent.LogoutSuccess -> {
                                val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                requireActivity().finish()
                            }
                            is ProfileUiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleState(state: UiState<ProfileDto>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Loading -> {}
            is UiState.Success -> {
                val profile = state.data
                currentProfile = profile

                binding.tvProfileName.text = profile.fullName.ifBlank { "Pengguna Sepi" }
                binding.tvAvatarInitial.text = profile.fullName.take(1).uppercase().ifBlank { "S" }

                val email = (requireActivity().application as SepiApp).appContainer.sessionManager.userEmail
                binding.tvProfileEmail.text = email.orEmpty()
            }
            is UiState.Error -> {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.menu_logout) { dialog, _ ->
                dialog.dismiss()
                viewModel.logout()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
