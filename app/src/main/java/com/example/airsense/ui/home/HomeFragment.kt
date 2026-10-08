package com.example.airsense.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.FragmentHomeBinding
import com.example.airsense.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var userPrefs: UserPreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private var currentTempCelsius: Double = 22.5
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchLocationAndLoadMetrics()
        } else {
            viewModel.loadMetrics()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        userPrefs = UserPreferencesManager(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        checkLocationPermissionAndLoad()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }
                launch {
                    userPrefs.useFahrenheitFlow.collect { useFahrenheit ->
                        updateTemperatureDisplay(useFahrenheit)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            com.example.airsense.R.color.airsense_primary,
            com.example.airsense.R.color.airsense_secondary
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            checkLocationPermissionAndLoad()
        }

        binding.btnRefreshWeather.setOnClickListener {
            checkLocationPermissionAndLoad()
        }

        binding.btnRetry.setOnClickListener {
            checkLocationPermissionAndLoad()
        }
    }

    private fun checkLocationPermissionAndLoad() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchLocationAndLoadMetrics()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @Suppress("MissingPermission")
    private fun fetchLocationAndLoadMetrics() {
        try {
            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            var location: Location? = null
            if (locationManager != null) {
                if (isNetworkEnabled) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                if (location == null && isGpsEnabled) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
            }

            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                viewModel.loadMetrics(currentLat, currentLon, userPrefs)
            } else {
                viewModel.loadMetrics(userPrefs = userPrefs)
            }
        } catch (_: Exception) {
            viewModel.loadMetrics(userPrefs = userPrefs)
        }
    }

    private fun renderUiState(state: HomeUiState) {
        if (state !is HomeUiState.Loading) {
            binding.swipeRefreshLayout.isRefreshing = false
        }

        when (state) {
            is HomeUiState.Loading -> {
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.progressLoading.visibility = View.VISIBLE
                }
                binding.cardErrorState.visibility = View.GONE
            }
            is HomeUiState.Success -> {
                binding.progressLoading.visibility = View.GONE
                binding.cardErrorState.visibility = View.GONE

                currentTempCelsius = state.metrics.temperatureCelsius

                binding.tvHomeAqiVal.text = "${state.metrics.aqi} AQI"
                binding.tvHomeAqiStatus.text = state.metrics.aqiStatus
                binding.tvHomeHumidityVal.text = "${state.metrics.humidityPercentage} %"
                binding.tvHomeLastUpdated.text = if (state.isOffline) {
                    "⚠️ Modo Sin Conexión"
                } else {
                    "📡 ${state.metrics.lastUpdated} • API Google Weather"
                }

                // Actualización de métricas en tarjetas de detalles
                binding.tvHomeFeatureAqiReading.text = "Lectura: ${state.metrics.aqi} AQI • PM2.5: ${state.metrics.pm25Ug} µg/m³"
                binding.tvHomeFeatureHumidityReading.text = "Lectura: ${state.metrics.humidityPercentage}% de Humedad Relativa"

                viewLifecycleOwner.lifecycleScope.launch {
                    val useFahrenheit = userPrefs.useFahrenheitFlow.first()
                    updateTemperatureDisplay(useFahrenheit)
                }

                // Evaluar umbrales y disparar notificación amigable si corresponde
                viewLifecycleOwner.lifecycleScope.launch {
                    val notificationsEnabled = userPrefs.isNotificationsEnabledFlow.first()
                    notificationHelper.sendFriendlyThresholdAlertIfNeeded(
                        metrics = state.metrics,
                        userNotificationsEnabled = notificationsEnabled
                    )
                }
            }
            is HomeUiState.Empty -> {
                binding.progressLoading.visibility = View.GONE
                binding.cardErrorState.visibility = View.GONE
            }
            is HomeUiState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.cardErrorState.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun updateTemperatureDisplay(useFahrenheit: Boolean) {
        val formattedTemp = UserPreferencesManager.formatTemperature(
            currentTempCelsius,
            useFahrenheit
        )
        _binding?.tvHomeTempVal?.text = formattedTemp
        _binding?.tvHomeFeatureTempReading?.text = "Lectura: $formattedTemp"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
