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
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.databinding.FragmentHomeBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private var currentTempCelsius: Double = 22.5
    private var isFahrenheit: Boolean = false
    private var isCurrentlyConnected: Boolean = false

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
                        if (isCurrentlyConnected) {
                            updateTemperatureDisplay(isFahrenheit)
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            sharedViewModel.fetchRealtimeWeather()
        }
        binding.cardQuickRecommendation.setOnClickListener {
            try {
                androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_recommendations)
            } catch (_: Exception) {}
        }
    }

    private fun renderAirSenseState(state: AirSenseState) {
        val context = requireContext()
        val colorStale = ContextCompat.getColor(context, R.color.text_secondary)
        val bgStale = ContextCompat.getColor(context, R.color.bg_card_subtle)

        when (state) {
            is AirSenseState.Connecting -> {
                isCurrentlyConnected = false
                maskUnconfirmedReadings(
                    gasStatusText = "Conectando...",
                    aqiStatusText = "Conectando...",
                    subText = "Conectando a telemetría..."
                )
                binding.tvLocation.text = "Conectando a Firebase..."
            }
            is AirSenseState.WarmingUp -> {
                isCurrentlyConnected = false
                maskUnconfirmedReadings(
                    gasStatusText = "Calentando...",
                    aqiStatusText = "Calibrando...",
                    subText = "Estabilizando sensores (${state.remainingSeconds}s)"
                )
                binding.tvLocation.text = "Calibrando sensor (${state.remainingSeconds}s)..."
            }
            is AirSenseState.ActiveValid -> {
                isCurrentlyConnected = true
                updateOperationalUi(state.metrics)
                binding.tvLocation.text = getString(R.string.status_source_firebase)
            }
            is AirSenseState.Stale -> {
                isCurrentlyConnected = false
                maskUnconfirmedReadings(
                    gasStatusText = "Desconectado",
                    aqiStatusText = "Desconectado",
                    subText = "Lectura no confirmada (Desconectado)"
                )
                binding.chipAqiStatus.setTextColor(colorStale)
                binding.chipAqiStatus.chipBackgroundColor = ColorStateList.valueOf(bgStale)
                binding.chipGasStatus.setTextColor(colorStale)
                binding.chipGasStatus.chipBackgroundColor = ColorStateList.valueOf(bgStale)
                binding.tvLocation.text = state.reason
                binding.tvRecTitle.text = "Sin conexión"
                binding.tvRecSubtitle.text = "Verifique energía y Wi-Fi en cocina"
            }
        }
    }

    /**
     * Cumple con la directriz de diseño: en escenarios desconectados no se presentan como
     * actuales lecturas no confirmadas (se muestran placeholders '--').
     */
    private fun maskUnconfirmedReadings(
        gasStatusText: String,
        aqiStatusText: String,
        subText: String
    ) {
        binding.tvGasValue.text = "--"
        binding.tvGasSub.text = subText
        binding.chipGasStatus.text = gasStatusText
        binding.progressGasLevel.setProgressCompat(0, false)

        binding.tvAqiValue.text = "--"
        binding.chipAqiStatus.text = aqiStatusText
        binding.aqiProgressIndicator.setProgressCompat(0, false)

        binding.tvTempValue.text = "--"
        binding.tvHumidityValue.text = "--"
        binding.tvTempLabel.text = "No disponible"
        binding.tvHumidityLabel.text = "No disponible"
    }

    private fun updateOperationalUi(metrics: AirQualityMetrics) {
        val context = requireContext()
        currentTempCelsius = metrics.temperatureCelsius

        // 1. NIVEL DE GAS (OPERACIONAL)
        binding.tvGasValue.text = metrics.co2.toString()
        binding.tvGasSub.text = String.format(
            Locale.US,
            "CO: %.1f ppm · CH₂O: %.2f ppm · Raw: %d",
            metrics.co,
            metrics.ch2o,
            metrics.gasRaw
        )
        binding.progressGasLevel.setProgressCompat(metrics.co2.coerceIn(0, 2000), true)

        when {
            metrics.co2 <= 800 -> {
                val colorGood = ContextCompat.getColor(context, R.color.aqi_good)
                val bgGood = ContextCompat.getColor(context, R.color.aqi_good_bg)
                binding.chipGasStatus.text = "Normal"
                binding.chipGasStatus.setTextColor(colorGood)
                binding.chipGasStatus.chipBackgroundColor = ColorStateList.valueOf(bgGood)
                binding.progressGasLevel.setIndicatorColor(colorGood)
            }
            metrics.co2 in 801..1200 -> {
                val colorMod = ContextCompat.getColor(context, R.color.aqi_moderate)
                val bgMod = ContextCompat.getColor(context, R.color.aqi_moderate_bg)
                binding.chipGasStatus.text = "Precaución"
                binding.chipGasStatus.setTextColor(colorMod)
                binding.chipGasStatus.chipBackgroundColor = ColorStateList.valueOf(bgMod)
                binding.progressGasLevel.setIndicatorColor(colorMod)
            }
            else -> {
                val colorDanger = ContextCompat.getColor(context, R.color.aqi_danger)
                val bgDanger = ContextCompat.getColor(context, R.color.aqi_danger_bg)
                binding.chipGasStatus.text = "Peligro"
                binding.chipGasStatus.setTextColor(colorDanger)
                binding.chipGasStatus.chipBackgroundColor = ColorStateList.valueOf(bgDanger)
                binding.progressGasLevel.setIndicatorColor(colorDanger)
            }
        }

        // 2. CALIDAD DEL AIRE (AQI) - Estados: Óptimo, Moderado, Peligroso
        val aqi = metrics.aqi
        binding.aqiProgressIndicator.setProgressCompat(aqi, true)
        binding.tvAqiValue.text = aqi.toString()

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

                binding.tvRecTitle.text = "Ventilación adecuada"
                binding.tvRecSubtitle.text = "Ambiente de cocina seguro para operación"
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

                binding.tvRecTitle.text = "Activar campana extractora"
                binding.tvRecSubtitle.text = "Flujo de aire moderado recomendado"
            }
            else -> {
                val colorDanger = ContextCompat.getColor(context, R.color.aqi_danger)
                val bgDanger = ContextCompat.getColor(context, R.color.aqi_danger_bg)

                binding.aqiProgressIndicator.setIndicatorColor(colorDanger)
                binding.chipAqiStatus.text = "Peligroso"
                binding.chipAqiStatus.setTextColor(colorDanger)
                binding.chipAqiStatus.chipBackgroundColor = ColorStateList.valueOf(bgDanger)
                binding.ivRecIcon.backgroundTintList = ColorStateList.valueOf(bgDanger)
                binding.ivRecIcon.imageTintList = ColorStateList.valueOf(colorDanger)

                binding.tvRecTitle.text = "Extracción al máximo"
                binding.tvRecSubtitle.text = "Riesgo de gases: revisar quemadores"
            }
        }

        // 3. TARJETAS BENTO: TEMPERATURA Y HUMEDAD
        updateTemperatureDisplay(isFahrenheit)
        binding.tvHumidityValue.text = "${metrics.humidityPercentage}%"

        binding.tvTempLabel.text = when {
            metrics.temperature < 18 -> "Frío en cocina"
            metrics.temperature in 18f..26f -> "Confortable"
            else -> "Calor elevado"
        }

        binding.tvHumidityLabel.text = when {
            metrics.humidity < 35 -> "Seco"
            metrics.humidity in 35f..65f -> "Humedad ideal"
            else -> "Humedad alta"
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
