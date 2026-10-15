package com.example.airsense.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.databinding.FragmentSensorDetailBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import kotlinx.coroutines.launch

class SensorDetailFragment : Fragment() {

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

        // Valores por defecto obtenidos desde Navigation Bundle
        val defaultSensorId = arguments?.getString("sensorId") ?: "sns_01"
        val defaultSensorName = arguments?.getString("sensorName") ?: "Sensor IoT"
        val defaultSensorType = arguments?.getString("sensorType") ?: "AQI + Temp"
        val defaultBatteryLevel = arguments?.getInt("batteryLevel") ?: 88

        renderSensorDetails(
            sensorId = defaultSensorId,
            sensorName = defaultSensorName,
            sensorType = defaultSensorType,
            batteryLevel = defaultBatteryLevel
        )

        // Observación reactiva desde el SharedViewModel (SSOT)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.selectedSensor.collect { sensor ->
                    if (sensor != null) {
                        renderSensorDetails(
                            sensorId = sensor.id,
                            sensorName = sensor.name,
                            sensorType = sensor.type,
                            batteryLevel = sensor.batteryLevel
                        )
                    }
                }
            }
        }
    }

    private fun renderSensorDetails(
        sensorId: String,
        sensorName: String,
        sensorType: String,
        batteryLevel: Int
    ) {
        if (_binding == null) return
        binding.tvSensorTitle.text = sensorName
        binding.tvSensorId.text = "ID: $sensorId"
        binding.tvSensorType.text = sensorType
        binding.tvBatteryLevel.text = "$batteryLevel %"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
