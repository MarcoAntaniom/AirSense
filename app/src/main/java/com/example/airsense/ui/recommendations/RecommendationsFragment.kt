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
import com.example.airsense.databinding.FragmentRecommendationsBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import kotlinx.coroutines.launch

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
            val advice = when {
                metrics.aqi > 100 -> "⚠️ AQI Elevado (${metrics.aqi}): Se recomienda mantener ventanas cerradas y utilizar purificadores de aire si están disponibles."
                metrics.humidityPercentage < 30 -> "💧 Ambiente Seco (${metrics.humidityPercentage}%): Considera usar un humidificador o ventilación cruzada para mayor confort."
                metrics.humidityPercentage > 60 -> "🌬️ Humedad Alta (${metrics.humidityPercentage}%): Se aconseja ventilar ambientes cerrados para evitar la acumulación de condensación."
                else -> "🌿 Calidad de Aire Excelente (${metrics.aqi} AQI): El ambiente actual es óptimo para realizar actividades y ventilar el hogar libremente."
            }
            binding.tvActiveRecommendationDesc.text = advice
        } else {
            binding.tvActiveRecommendationDesc.text = "🌿 Mantén una ventilación regular para conservar una calidad de aire saludable en tu espacio."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
