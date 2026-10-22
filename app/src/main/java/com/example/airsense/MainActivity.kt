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
        checkAndRequestPermissions()
        setupNavigation()

        lifecycleScope.launch {
            val isLoggedIn = userPrefs.isLoggedInFlow.first()
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            val navController = navHostFragment.navController

            if (!isLoggedIn) {
                if (navController.currentDestination?.id != R.id.loginFragment) {
                    navController.navigate(R.id.loginFragment)
                }
            } else {
                // Redirección simplificada: Siempre vamos a Home.
                // La lógica del tour interactivo se maneja dentro de cada fragmento basándose en el ViewModel compartido.
                if (navController.currentDestination?.id == R.id.loginFragment || 
                    navController.currentDestination?.id == R.id.onboardingFragment) {
                    navController.navigate(R.id.navigation_home)
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

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.onboardingFragment, R.id.sensorDetailFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(destination.id != R.id.loginFragment)
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }
            updateToolbarTitle(destination.id)
        }
    }

    private fun updateToolbarTitle(destinationId: Int) {
        when (destinationId) {
            R.id.navigation_home -> supportActionBar?.setTitle(R.string.title_home)
            R.id.navigation_sensors -> supportActionBar?.setTitle(R.string.title_sensors)
            R.id.navigation_recommendations -> supportActionBar?.setTitle(R.string.title_recommendations)
            R.id.navigation_settings -> supportActionBar?.setTitle(R.string.title_settings)
            R.id.sensorDetailFragment -> supportActionBar?.title = "Detalle del Sensor"
            R.id.onboardingFragment -> supportActionBar?.title = "Bienvenido"
            R.id.loginFragment -> supportActionBar?.title = "Iniciar Sesión"
            else -> supportActionBar?.setTitle(R.string.title_home)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return navHostFragment?.navController?.navigateUp() ?: super.onSupportNavigateUp()
    }
}