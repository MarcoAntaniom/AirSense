package com.example.airsense.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.airsense.databinding.FragmentSensorDetailBinding

class SensorDetailFragment : Fragment() {

    private var _binding: FragmentSensorDetailBinding? = null
    private val binding get() = _binding!!

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

        val sensorId = arguments?.getString("sensorId") ?: "sns_01"
        val sensorName = arguments?.getString("sensorName") ?: "Sensor IoT"
        val sensorType = arguments?.getString("sensorType") ?: "AQI + Temp"
        val batteryLevel = arguments?.getInt("batteryLevel") ?: 88

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