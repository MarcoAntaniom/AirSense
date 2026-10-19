package com.example.airsense.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
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
    private var isFahrenheit: Boolean = false

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
                        isFahrenheit = useFahrenheit
                        updateTemperatureDisplay(isFahrenheit)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            sharedViewModel.fetchRealtimeWeather()
        }
    }

    private fun renderAirSenseState(state: AirSenseState) {
        when (state) {
            is AirSenseState.Connecting -> {
                // Estado conectando
            }
            is AirSenseState.WarmingUp -> {
                binding.chipAqiStatus.text = "Calentando..."
            }
            is AirSenseState.ActiveValid -> {
                updateModernUi(
                    aqi = state.metrics.aqi,
                    temp = state.metrics.temperatureCelsius,
                    humidity = state.metrics.humidityPercentage
                )
            }
            is AirSenseState.Stale -> {
                updateModernUi(
                    aqi = state.lastValidMetrics.aqi,
                    temp = state.lastValidMetrics.temperatureCelsius,
                    humidity = state.lastValidMetrics.humidityPercentage
                )
            }
        }
    }

    private fun updateModernUi(aqi: Int, temp: Double, humidity: Int) {
        currentTempCelsius = temp

        // 1. Actualizar números principales
        binding.aqiProgressIndicator.setProgressCompat(aqi, true)
        binding.tvAqiValue.text = aqi.toString()
        updateTemperatureDisplay(isFahrenheit)
        binding.tvHumidityValue.text = "$humidity%"

        // 2. Semáforo visual dinámico (Cambia colores y acciones breves según AQI)
        val context = requireContext()
        when {
            aqi <= 50 -> {
                val colorGood = ContextCompat.getColor(context, R.color.aqi_good)
                val bgGood = ContextCompat.getColor(context, R.color.aqi_good_bg)

                binding.aqiProgressIndicator.setIndicatorColor(colorGood)
                binding.chipAqiStatus.text = "Óptimo"
                binding.chipAqiStatus.setTextColor(colorGood)
                binding.chipAqiStatus.chipBackgroundColor = ColorStateList.valueOf(bgGood)
                binding.ivRecIcon.backgroundTintList = ColorStateList.valueOf(bgGood)
                binding.ivRecIcon.imageTintList = ColorStateList.valueOf(colorGood)

                binding.tvRecTitle.text = "Ventilar ahora"
                binding.tvRecSubtitle.text = "Aire limpio · 15 min"
            }
            aqi in 51..100 -> {
                val colorMod = ContextCompat.getColor(context, R.color.aqi_moderate)
                val bgMod = ContextCompat.getColor(context, R.color.aqi_moderate_bg)

                binding.aqiProgressIndicator.setIndicatorColor(colorMod)
                binding.chipAqiStatus.text = "Moderado"
                binding.chipAqiStatus.setTextColor(colorMod)
                binding.chipAqiStatus.chipBackgroundColor = ColorStateList.valueOf(bgMod)
                binding.ivRecIcon.backgroundTintList = ColorStateList.valueOf(bgMod)
                binding.ivRecIcon.imageTintList = ColorStateList.valueOf(colorMod)

                binding.tvRecTitle.text = "Ventilación breve"
                binding.tvRecSubtitle.text = "Sensibles: precaución"
            }
            else -> {
                val colorDanger = ContextCompat.getColor(context, R.color.aqi_danger)
                val bgDanger = ContextCompat.getColor(context, R.color.aqi_danger_bg)

                binding.aqiProgressIndicator.setIndicatorColor(colorDanger)
                binding.chipAqiStatus.text = "Alerta"
                binding.chipAqiStatus.setTextColor(colorDanger)
                binding.chipAqiStatus.chipBackgroundColor = ColorStateList.valueOf(bgDanger)
                binding.ivRecIcon.backgroundTintList = ColorStateList.valueOf(bgDanger)
                binding.ivRecIcon.imageTintList = ColorStateList.valueOf(colorDanger)

                binding.tvRecTitle.text = "Cerrar ventanas"
                binding.tvRecSubtitle.text = "Encender purificador"
            }
        }
    }

    private fun updateTemperatureDisplay(useFahrenheit: Boolean) {
        val formattedTemp = UserPreferencesManager.formatTemperature(
            currentTempCelsius,
            useFahrenheit
        )
        _binding?.tvTempValue?.text = formattedTemp
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}