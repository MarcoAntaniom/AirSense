package com.example.airsense.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.FragmentHomeBinding
import com.example.airsense.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private var currentTempCelsius: Double = 22.5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        userPrefs = UserPreferencesManager(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }
                launch {
                    userPrefs.useFahrenheitFlow.collect { useFahrenheit ->
                        updateTemperatureDisplay(useFahrenheit)
                    }
                }
            }
        }
    }

    private fun renderUiState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> {
                // Render Loading State
            }
            is HomeUiState.Success -> {
                currentTempCelsius = state.metrics.temperatureCelsius
                updateTemperatureDisplay(false)

                // Evaluar umbrales y disparar notificación amigable si corresponde
                viewLifecycleOwner.lifecycleScope.launch {
                    val notificationsEnabled = userPrefs.isNotificationsEnabledFlow.first()
                    notificationHelper.sendFriendlyThresholdAlertIfNeeded(
                        metrics = state.metrics,
                        userNotificationsEnabled = notificationsEnabled
                    )
                }
            }
            is HomeUiState.Empty -> {
                // Render Empty State
            }
            is HomeUiState.Error -> {
                // Render Error State
            }
        }
    }

    private fun updateTemperatureDisplay(useFahrenheit: Boolean) {
        _binding?.tvHomeTempVal?.text = UserPreferencesManager.formatTemperature(
            currentTempCelsius,
            useFahrenheit
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
