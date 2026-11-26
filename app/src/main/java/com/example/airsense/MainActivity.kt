package com.example.airsense

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.ActivityMainBinding
import com.example.airsense.notifications.SensorMonitoringService
import com.example.airsense.ui.SharedAirSenseViewModel
import com.example.airsense.utils.PermissionHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userPrefs: UserPreferencesManager
    private val sharedViewModel: SharedAirSenseViewModel by viewModels()

    private val requestAppPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        lifecycleScope.launch {
            val isNotificationGranted = PermissionHelper.hasNotificationPermission(this@MainActivity)
            userPrefs.setNotificationsEnabled(isNotificationGranted)
            if (isNotificationGranted) {
                SensorMonitoringService.startService(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        userPrefs = UserPreferencesManager(this)

        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Escuchar el Modo de Tema (Sistema, Claro, Oscuro) de forma asíncrona
        lifecycleScope.launch {
            userPrefs.themeModeFlow.collect { themeMode ->
                val targetMode = when (themeMode) {
                    UserPreferencesManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    UserPreferencesManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                    AppCompatDelegate.setDefaultNightMode(targetMode)
                }
            }
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Escuchar el estado de conexión para el pill indicador en Toolbar
        lifecycleScope.launch {
            sharedViewModel.isConnected.collect { connected ->
                binding.tvToolbarLive.text = if (connected) "EN VIVO" else "OFFLINE"
                val dotColor = androidx.core.content.ContextCompat.getColor(
                    this@MainActivity,
                    if (connected) R.color.aqi_good else R.color.text_secondary
                )
                binding.dotLive.backgroundTintList = android.content.res.ColorStateList.valueOf(dotColor)
            }
        }

        // Solicitar permisos del sistema
        checkAndRequestPermissions()

        // Configurar navegación unificada con Jetpack Navigation
        setupNavigation()

        // Router Evaluation: si es la primera ejecución, navegar a Onboarding;
        // si no es la primera ejecución y no hay sesión activa, redirigir a Login
        lifecycleScope.launch {
            val isFirstRun = userPrefs.isFirstRunFlow.first()
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            val navController = navHostFragment.navController
            if (isFirstRun) {
                if (navController.currentDestination?.id != R.id.onboardingFragment) {
                    navController.navigate(R.id.onboardingFragment)
                }
            } else if (runCatching { Firebase.auth.currentUser }.getOrNull() == null) {
                if (navController.currentDestination?.id != R.id.loginFragment) {
                    navController.navigate(R.id.action_global_to_loginFragment)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = PermissionHelper.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            requestAppPermissionsLauncher.launch(missingPermissions)
        } else {
            if (PermissionHelper.hasNotificationPermission(this)) {
                SensorMonitoringService.startService(this)
            }
        }
    }

    /**
     * Configura Jetpack Navigation asociando el BottomNavigationView directamente al NavController.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
                R.id.onboardingFragment, R.id.sensorDetailFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }
            updateToolbarTitle(destination.id)
        }
    }

    /**
     * Actualiza el título del Toolbar según la sección seleccionada.
     */
    private fun updateToolbarTitle(destinationId: Int) {
        val title = when (destinationId) {
            R.id.navigation_home -> "AirSense"
            R.id.navigation_sensors -> getString(R.string.title_sensors)
            R.id.navigation_recommendations -> getString(R.string.title_recommendations)
            R.id.navigation_settings -> getString(R.string.title_settings)
            R.id.sensorDetailFragment -> "Detalle"
            R.id.onboardingFragment -> "Bienvenido"
            R.id.loginFragment -> "Iniciar Sesión"
            else -> "AirSense"
        }
        binding.toolbarTitle.text = title
        supportActionBar?.title = ""
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return navHostFragment?.navController?.navigateUp() ?: super.onSupportNavigateUp()
    }
}
