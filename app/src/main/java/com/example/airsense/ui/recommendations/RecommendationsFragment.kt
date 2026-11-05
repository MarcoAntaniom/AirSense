package com.example.airsense.ui.recommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.databinding.FragmentRecommendationsBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class RecommendationsFragment : Fragment() {

    private var _binding: FragmentRecommendationsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.airQualityMetrics.collect { metrics ->
                    updateRecommendations(metrics)
                }
            }
        }
    }

    private fun updateRecommendations(metrics: AirQualityMetrics?) {
        if (_binding == null) return

        if (metrics != null) {
            val coFormatted = String.format(Locale.US, "%.1f", metrics.co)
            val ch2oFormatted = String.format(Locale.US, "%.2f", metrics.ch2o)
            val tempFormatted = String.format(Locale.US, "%.1f", metrics.temperature)
            val pm25Formatted = String.format(Locale.US, "%.1f", metrics.pm25)
            val pm10Formatted = String.format(Locale.US, "%.1f", metrics.pm10)
            val so2Formatted = String.format(Locale.US, "%.1f", metrics.so2)

            val priorityAdvice = when {
                metrics.co > AirQualityThresholds.CO_DANGER_PPM ->
                    "🚨 Evacuar o ventilar inmediatamente: Concentración de CO es de $coFormatted ppm, superando el Límite Permisible Ponderado LPP (40.0 ppm - D.S. 594)."
                metrics.ch2o > AirQualityThresholds.CH2O_DANGER_PPM ->
                    "🚨 Ventilar recinto: Concentración de Formaldehído (CH₂O) es de $ch2oFormatted ppm, superando el Límite Permisible Absoluto LPA (0.30 ppm)."
                metrics.co2 > AirQualityThresholds.CO2_DANGER_PPM ->
                    "🌬️ Ventilar inmediatamente el recinto: Concentración de CO₂ es de ${metrics.co2} ppm, superando las 1,000 ppm (Alarma de ventilación requerida)."
                metrics.temperature > AirQualityThresholds.TEMP_TGBH_DANGER_C ->
                    "🌡️ Riesgo térmico laboral: Temperatura alcanza $tempFormatted °C, superando el límite de trabajo moderado TGBH (26.7 °C - D.S. 594 Chile)."
                metrics.pm25 >= AirQualityThresholds.PM25_EMERGENCY_MIN || metrics.pm10 >= AirQualityThresholds.PM10_EMERGENCY_MIN ->
                    "🚨 Emergencia Ambiental Exterior: PM2.5 es $pm25Formatted µg/m³ y PM10 es $pm10Formatted µg/m³ (Norma MMA Chile). Usar mascarilla N95 y cerrar ventanas."
                metrics.so2 > AirQualityThresholds.SO2_DANGER_MIN ->
                    "⚠️ Alerta Ambiental Exterior: Dióxido de Azufre (SO₂) alcanza $so2Formatted µg/m³, superando el límite horario de 350 µg/m³."
                metrics.humidity < 30.0f ->
                    "💧 Ambiente Seco (${metrics.humidity.toInt()}% HR): Se sugiere utilizar un humidificador o ventilación para mayor confort de las vías respiratorias."
                metrics.humidity > 70.0f ->
                    "🌬️ Humedad Alta (${metrics.humidity.toInt()}% HR): Ventilar para reducir condensación y prevenir formación de moho."
                else ->
                    "🌿 Calidad ambiental óptima: Parámetros dentro de la normativa de confort laboral y ambiental chilena. Mantén ventilación periódica."
            }
            binding.tvActiveRecommendationDesc.text = priorityAdvice
        } else {
            binding.tvActiveRecommendationDesc.text = "🌿 Mantén una ventilación regular para conservar una calidad de aire saludable según la normativa laboral."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
