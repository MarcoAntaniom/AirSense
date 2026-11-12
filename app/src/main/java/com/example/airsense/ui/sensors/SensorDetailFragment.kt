package com.example.airsense.ui.sensors

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.R
import com.example.airsense.data.models.SensorDevice
import com.example.airsense.databinding.FragmentSensorDetailBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class SensorDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSensorDetailBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseDetail.setOnClickListener {
            dismiss()
        }

        val defaultSensorId = arguments?.getString("sensorId") ?: "sns_01"
        val defaultSensorName = arguments?.getString("sensorName") ?: "Estación AirSense"
        val defaultSensorType = arguments?.getString("sensorType") ?: "Índice General"
        val defaultBatteryLevel = arguments?.getInt("batteryLevel") ?: 95

        renderSensorDetails(
            SensorDevice(
                id = defaultSensorId,
                name = defaultSensorName,
                type = defaultSensorType,
                batteryLevel = defaultBatteryLevel,
                isOnline = true,
                lastReading = "--"
            )
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.selectedSensor.collect { sensor ->
                    if (sensor != null) {
                        renderSensorDetails(sensor)
                    }
                }
            }
        }
    }

    private fun renderSensorDetails(sensor: SensorDevice) {
        if (_binding == null) return
        val context = requireContext()

        binding.tvSensorTitle.text = sensor.name
        binding.tvSensorId.text = "ID: ${sensor.id} · ESP8266 IoT"
        binding.tvSensorType.text = sensor.type
        binding.tvBatteryLevel.text = "${sensor.batteryLevel}%"
        binding.tvSensorReading.text = sensor.lastReading

        binding.tvSensorRange.text = if (sensor.referenceRange.isNotBlank()) {
            sensor.referenceRange
        } else {
            "Parámetro normativo estándar"
        }

        binding.tvSensorDescription.text = if (sensor.description.isNotBlank()) {
            sensor.description
        } else {
            "Monitoreo continuo en tiempo real para evaluar y preservar la calidad del aire interior."
        }

        if (sensor.isOnline) {
            val goodColor = ContextCompat.getColor(context, R.color.aqi_good)
            val goodBg = ContextCompat.getColor(context, R.color.aqi_good_bg)
            binding.chipSensorStatus.text = "En Línea (Firebase)"
            binding.chipSensorStatus.setTextColor(goodColor)
            binding.chipSensorStatus.chipBackgroundColor = ColorStateList.valueOf(goodBg)
            binding.tvSensorReadingStatus.text = "Nivel Seguro / Transmitiendo"
            binding.tvSensorReadingStatus.setTextColor(goodColor)
        } else {
            val staleColor = ContextCompat.getColor(context, R.color.text_secondary)
            val staleBg = ContextCompat.getColor(context, R.color.bg_card_subtle)
            binding.chipSensorStatus.text = "Sin Señal"
            binding.chipSensorStatus.setTextColor(staleColor)
            binding.chipSensorStatus.chipBackgroundColor = ColorStateList.valueOf(staleBg)
            binding.tvSensorReadingStatus.text = "Sensor fuera de línea"
            binding.tvSensorReadingStatus.setTextColor(staleColor)
        }

        val iconRes = when {
            sensor.name.contains("AQI", ignoreCase = true) || sensor.type.contains("AQI", ignoreCase = true) -> R.drawable.ic_air_quality
            sensor.name.contains("Temp", ignoreCase = true) || sensor.type.contains("Temp", ignoreCase = true) -> R.drawable.ic_temp
            sensor.name.contains("Humedad", ignoreCase = true) || sensor.type.contains("Humedad", ignoreCase = true) -> R.drawable.ic_humidity
            sensor.name.contains("Carbono", ignoreCase = true) || sensor.name.contains("Formaldehído", ignoreCase = true) -> R.drawable.ic_sensors
            else -> R.drawable.ic_iot
        }
        binding.imgSensorIcon.setImageResource(iconRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
