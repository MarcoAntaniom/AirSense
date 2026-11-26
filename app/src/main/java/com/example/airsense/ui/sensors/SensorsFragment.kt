package com.example.airsense.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.databinding.FragmentSensorsBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class SensorsFragment : Fragment() {

    private var _binding: FragmentSensorsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private lateinit var sensorsAdapter: SensorsAdapter
    private var currentTempCelsius: Double = 22.4
    private var isFahrenheit: Boolean = false
    private var isCurrentlyConnected: Boolean = false

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

        setupRecyclerView()

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.airsense_primary,
            R.color.airsense_secondary
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            sharedViewModel.refreshTelemetry()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.airSenseState.collect { state ->
                        renderAirSenseState(state)
                    }
                }
                launch {
                    sharedViewModel.sensors.collect { devices ->
                        if (devices.isNotEmpty()) {
                            sensorsAdapter.updateSensors(devices)
                        }
                    }
                }
                launch {
                    userPrefs.useFahrenheitFlow.collect { useFahrenheit ->
                        isFahrenheit = useFahrenheit
                        if (isCurrentlyConnected) {
                            updateTemperatureDisplay(useFahrenheit)
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        sensorsAdapter = SensorsAdapter { sensor ->
            sharedViewModel.selectSensor(sensor)
            navigateToSensorDetail(
                sensorId = sensor.id,
                sensorName = sensor.name,
                sensorType = sensor.type,
                batteryLevel = sensor.batteryLevel
            )
        }
        binding.rvSensorsList.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = sensorsAdapter
        }
    }

    private fun renderAirSenseState(state: AirSenseState) {
        binding.swipeRefreshLayout.isRefreshing = false

        when (state) {
            is AirSenseState.ActiveValid -> {
                isCurrentlyConnected = true
                updateSensorsUi(state.metrics)
            }
            is AirSenseState.Stale, is AirSenseState.WarmingUp, is AirSenseState.Connecting -> {
                isCurrentlyConnected = false
                maskSensorsUi()
            }
        }
    }

    private fun maskSensorsUi() {
        binding.tvSensorsAqiVal.text = "--"
        binding.progressSensorsAqi.progress = 0

        binding.tvSensorsCo2Val.text = "--"
        binding.tvSensorsGasesSub.text = "Sin telemetría activa"
        binding.progressSensorsCo2.progress = 0

        binding.tvSensorsTempVal.text = "--"
        binding.tvSensorsTempSub.text = "DHT11 · No disponible"
        binding.progressSensorsTemp.progress = 0

        binding.tvSensorsHumidityVal.text = "--"
        binding.tvSensorsHumiditySub.text = "DHT11 · No disponible"
        binding.progressSensorsHumidity.progress = 0
    }

    private fun updateSensorsUi(metrics: AirQualityMetrics) {
        currentTempCelsius = metrics.temperatureCelsius

        binding.tvSensorsAqiVal.text = "${metrics.aqi} AQI"
        binding.progressSensorsAqi.progress = metrics.aqi.coerceIn(0, 100)

        val coStr = String.format(Locale.US, "%.1f", metrics.co)
        val ch2oStr = String.format(Locale.US, "%.2f", metrics.ch2o)
        binding.tvSensorsCo2Val.text = "${metrics.co2} ppm"
        binding.tvSensorsGasesSub.text = "CO: $coStr ppm · CH₂O: $ch2oStr ppm"
        binding.progressSensorsCo2.progress = (metrics.co2 / 20).coerceIn(0, 100)

        updateTemperatureDisplay(isFahrenheit)
        binding.tvSensorsTempSub.text = when {
            metrics.temperature < 18f -> "DHT11 · Frío en cocina"
            metrics.temperature in 18f..26f -> "DHT11 · Confortable"
            else -> "DHT11 · Calor elevado"
        }
        binding.progressSensorsTemp.progress = (metrics.temperatureCelsius * 2).toInt().coerceIn(0, 100)

        binding.tvSensorsHumidityVal.text = "${metrics.humidityPercentage} %"
        binding.tvSensorsHumiditySub.text = when {
            metrics.humidity < 35f -> "DHT11 · Seco"
            metrics.humidity in 35f..65f -> "DHT11 · Humedad ideal"
            else -> "DHT11 · Humedad alta"
        }
        binding.progressSensorsHumidity.progress = metrics.humidityPercentage.coerceIn(0, 100)
    }

    private fun updateTemperatureDisplay(useFahrenheit: Boolean) {
        _binding?.tvSensorsTempVal?.text = UserPreferencesManager.formatTemperature(
            currentTempCelsius,
            useFahrenheit
        )
    }

    private fun navigateToSensorDetail(
        sensorId: String,
        sensorName: String,
        sensorType: String,
        batteryLevel: Int
    ) {
        val args = bundleOf(
            "sensorId" to sensorId,
            "sensorName" to sensorName,
            "sensorType" to sensorType,
            "batteryLevel" to batteryLevel
        )
        try {
            findNavController().navigate(R.id.action_global_sensorDetailFragment, args)
        } catch (_: Exception) {
            try {
                val navHostFragment = requireActivity().supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
                navHostFragment?.navController?.navigate(R.id.action_global_sensorDetailFragment, args)
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
