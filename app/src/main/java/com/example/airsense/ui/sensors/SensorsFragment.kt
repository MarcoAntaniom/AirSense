package com.example.airsense.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.FragmentSensorsBinding
import kotlinx.coroutines.launch

class SensorsFragment : Fragment() {

    private var _binding: FragmentSensorsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorsViewModel by viewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private var currentTempCelsius: Double = 22.5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorsBinding.inflate(inflater, container, false)
        userPrefs = UserPreferencesManager(requireContext())
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
                        _binding?.tvSensorsTempVal?.text = UserPreferencesManager.formatTemperature(
                            currentTempCelsius,
                            useFahrenheit
                        )
                    }
                }
            }
        }
    }

    private fun renderUiState(state: SensorsUiState) {
        when (state) {
            is SensorsUiState.Loading -> {
                // Loading State
            }
            is SensorsUiState.Success -> {
                // Render sensors
            }
            is SensorsUiState.Empty -> {
                // Empty State
            }
            is SensorsUiState.Error -> {
                // Error State
            }
        }
    }

    fun navigateToSensorDetail(sensorId: String, sensorName: String, sensorType: String, batteryLevel: Int) {
        val args = bundleOf(
            "sensorId" to sensorId,
            "sensorName" to sensorName,
            "sensorType" to sensorType,
            "batteryLevel" to batteryLevel
        )
        findNavController().navigate(R.id.action_navigation_sensors_to_sensorDetailFragment, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}