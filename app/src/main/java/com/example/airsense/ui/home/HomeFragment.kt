package com.example.airsense.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.databinding.FragmentHomeBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private var currentTempCelsius: Double = 22.5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        userPrefs = UserPreferencesManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.airSenseState.collect { state ->
                        renderAirSenseState(state)
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

    private fun setupListeners() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.airsense_primary,
            R.color.airsense_secondary
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            sharedViewModel.fetchRealtimeWeather()
        }

        binding.btnRefreshWeather.setOnClickListener {
            binding.swipeRefreshLayout.isRefreshing = true
            sharedViewModel.fetchRealtimeWeather()
        }

        binding.btnRetry.setOnClickListener {
            binding.swipeRefreshLayout.isRefreshing = true
            sharedViewModel.fetchRealtimeWeather()
        }
    }

    private fun renderAirSenseState(state: AirSenseState) {
        binding.swipeRefreshLayout.isRefreshing = false

        when (state) {
            is AirSenseState.Connecting -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.cardErrorState.visibility = View.GONE
            }
            is AirSenseState.WarmingUp -> {
                binding.progressLoading.visibility = View.GONE
                binding.cardErrorState.visibility = View.GONE
                binding.tvHomeAqiStatus.text = "Precalentando MQ (${state.remainingSeconds}s)"
            }
            is AirSenseState.ActiveValid -> {
                binding.progressLoading.visibility = View.GONE
                binding.cardErrorState.visibility = View.GONE
                displayMetrics(state.metrics, isStale = false, statusLabel = state.level.label)
            }
            is AirSenseState.Stale -> {
                binding.progressLoading.visibility = View.GONE
                binding.cardErrorState.visibility = View.GONE
                displayMetrics(state.lastValidMetrics, isStale = true, statusLabel = "Lectura Desactualizada")
            }
        }
    }

    private fun displayMetrics(metrics: AirQualityMetrics, isStale: Boolean, statusLabel: String) {
        currentTempCelsius = metrics.temperatureCelsius

        binding.tvHomeAqiVal.text = "${metrics.aqi} AQI"
        binding.tvHomeAqiStatus.text = statusLabel
        binding.tvHomeHumidityVal.text = "${metrics.humidityPercentage} %"

        binding.tvHomeLastUpdated.text = if (isStale) {
            "⚠️ Lectura Desactualizada"
        } else {
            "📡 ${metrics.lastUpdated}"
        }

        binding.tvHomeFeatureAqiReading.text = "Lectura: ${metrics.co2Ppm} PPM Gas • ${metrics.aqi} AQI • PM2.5: ${metrics.pm25Ug} µg/m³"
        binding.tvHomeFeatureHumidityReading.text = "Lectura: ${metrics.humidityPercentage}% HR (DHT11)"
    }

    private fun updateTemperatureDisplay(useFahrenheit: Boolean) {
        val formattedTemp = UserPreferencesManager.formatTemperature(
            currentTempCelsius,
            useFahrenheit
        )
        _binding?.tvHomeTempVal?.text = formattedTemp
        _binding?.tvHomeFeatureTempReading?.text = "Lectura: $formattedTemp"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
