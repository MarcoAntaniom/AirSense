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
import androidx.navigation.fragment.findNavController
import com.example.airsense.R
import com.example.airsense.databinding.FragmentSensorDetailBinding
import com.example.airsense.ui.SharedAirSenseViewModel
import com.example.airsense.ui.TourStep
import kotlinx.coroutines.launch
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
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
                launch {
                    sharedViewModel.currentTourStep.collect { step ->
                        if (step == TourStep.SENSOR_DETAIL) {
                            showSensorDetailTour()
                        }
                    }
                }
            }
        }
    }

    private fun showSensorDetailTour() {
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(binding.tvBatteryLevel)
            .setPrimaryText("Detalles Específicos")
            .setSecondaryText("Aquí puedes ver información técnica del sensor seleccionado, como el nivel de batería y el tipo de hardware.")
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FINISHED) {
                    sharedViewModel.setTourStep(TourStep.RECOMMENDATIONS)
                    findNavController().navigate(R.id.navigation_recommendations)
                }
            }
            .show()
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