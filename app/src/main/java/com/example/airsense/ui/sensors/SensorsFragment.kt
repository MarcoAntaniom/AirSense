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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.databinding.FragmentSensorsBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import com.example.airsense.ui.TourStep
import kotlinx.coroutines.launch
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class SensorsFragment : Fragment() {

    private var _binding: FragmentSensorsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private lateinit var sensorsAdapter: SensorsAdapter
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

        setupRecyclerView()

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.airsense_primary,
            R.color.airsense_secondary
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            sharedViewModel.fetchRealtimeWeather()
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
                        updateTemperatureDisplay(useFahrenheit)
                    }
                }
                launch {
                    sharedViewModel.currentTourStep.collect { step ->
                        if (step == TourStep.SENSORS) {
                            showSensorsTour()
                        }
                    }
                }
            }
        }
    }

    private fun showSensorsTour() {
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(binding.progressSensorsAqi)
            .setPrimaryText("Sensores Detallados")
            .setSecondaryText("Aquí ves el desglose de cada sensor (AQI, CO2, Temp, Humedad) con barras de progreso para una lectura rápida.")
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FINISHED) {
                    showTourStep2()
                }
            }
            .show()
    }

    private fun showTourStep2() {
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(binding.rvSensorsList)
            .setPrimaryText("Dispositivos IoT")
            .setSecondaryText("Lista de sensores físicos. Pulsa en uno para ver el detalle. Durante el tour, entraremos automáticamente al primero.")
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FINISHED) {
                    val firstSensor = sharedViewModel.sensors.value.firstOrNull()
                    if (firstSensor != null) {
                        sharedViewModel.selectSensor(firstSensor)
                        sharedViewModel.setTourStep(TourStep.SENSOR_DETAIL)
                        navigateToSensorDetail(
                            sensorId = firstSensor.id,
                            sensorName = firstSensor.name,
                            sensorType = firstSensor.type,
                            batteryLevel = firstSensor.batteryLevel
                        )
                    } else {
                        // Fallback if no sensors are found
                        sharedViewModel.setTourStep(TourStep.RECOMMENDATIONS)
                        findNavController().navigate(R.id.navigation_recommendations)
                    }
                }
            }
            .show()
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
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sensorsAdapter
        }
    }

    private fun renderAirSenseState(state: AirSenseState) {
        binding.swipeRefreshLayout.isRefreshing = false

        when (state) {
            is AirSenseState.ActiveValid -> {
                updateSensorsUi(state.metrics)
            }
            is AirSenseState.Stale -> {
                updateSensorsUi(state.lastValidMetrics)
            }
            is AirSenseState.WarmingUp, is AirSenseState.Connecting -> {
                // Estado de espera o carga
            }
        }
    }

    private fun updateSensorsUi(metrics: AirQualityMetrics) {
        currentTempCelsius = metrics.temperatureCelsius

        binding.tvSensorsAqiVal.text = "${metrics.aqi} AQI"
        binding.progressSensorsAqi.progress = metrics.aqi.coerceIn(0, 100)

        binding.tvSensorsCo2Val.text = "${metrics.co2Ppm} ppm"
        binding.progressSensorsCo2.progress = (metrics.co2Ppm / 20).coerceIn(0, 100)

        binding.tvSensorsHumidityVal.text = "${metrics.humidityPercentage} %"
        binding.progressSensorsHumidity.progress = metrics.humidityPercentage.coerceIn(0, 100)

        binding.progressSensorsTemp.progress = (metrics.temperatureCelsius * 2).toInt().coerceIn(0, 100)
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
            findNavController().navigate(R.id.sensorDetailFragment, args)
        } catch (_: Exception) {
            try {
                val navHostFragment = requireActivity().supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
                navHostFragment?.navController?.navigate(R.id.sensorDetailFragment, args)
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}