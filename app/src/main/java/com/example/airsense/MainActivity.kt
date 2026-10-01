package com.example.airsense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.ActivityMainBinding
import com.example.airsense.ui.main.MainPagerAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userPrefs: UserPreferencesManager
    private lateinit var pagerAdapter: MainPagerAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            userPrefs.setNotificationsEnabled(isGranted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        userPrefs = UserPreferencesManager(this)

        // Apply saved Dark Mode preference BEFORE super.onCreate to prevent activity recreate flicker
        runBlocking {
            val isDark = userPrefs.isDarkModeFlow.first()
            val targetMode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode)
            }
        }

        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupViewPagerAndNavigation()
        setupNavHostListener()

        // Router Evaluation: si es la primera ejecución, navegar a Onboarding
        lifecycleScope.launch {
            val isFirstRun = userPrefs.isFirstRunFlow.first()
            if (isFirstRun) {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                navHostFragment.navController.navigate(R.id.onboardingFragment)
            }
        }
    }

    /**
     * Configura el ViewPager2 y sincroniza bidireccionalmente con el BottomNavigationView.
     */
    private fun setupViewPagerAndNavigation() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 3

        // Sincronización: Al deslizar horizontalmente en el ViewPager2, actualiza la BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val targetItemId = when (position) {
                    0 -> R.id.navigation_home
                    1 -> R.id.navigation_sensors
                    2 -> R.id.navigation_recommendations
                    3 -> R.id.navigation_settings
                    else -> R.id.navigation_home
                }
                if (binding.bottomNavigation.selectedItemId != targetItemId) {
                    binding.bottomNavigation.selectedItemId = targetItemId
                }
                updateToolbarTitle(position)
            }
        })

        // Sincronización: Al pulsar un elemento en la BottomNavigationView, desplaza suavemente el ViewPager2
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val pageIndex = when (item.itemId) {
                R.id.navigation_home -> 0
                R.id.navigation_sensors -> 1
                R.id.navigation_recommendations -> 2
                R.id.navigation_settings -> 3
                else -> 0
            }
            if (binding.viewPager.currentItem != pageIndex) {
                binding.viewPager.setCurrentItem(pageIndex, true)
            }
            true
        }
    }

    /**
     * Actualiza el título del Toolbar según la sección seleccionada.
     */
    private fun updateToolbarTitle(position: Int) {
        val titleRes = when (position) {
            0 -> R.string.title_home
            1 -> R.string.title_sensors
            2 -> R.string.title_recommendations
            3 -> R.string.title_settings
            else -> R.string.title_home
        }
        supportActionBar?.setTitle(titleRes)
    }

    /**
     * Escucha cambios de destino en el NavHostFragment para manejar sub-pantallas
     * (Onboarding, Detalle de Sensor).
     */
    private fun setupNavHostListener() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.onboardingFragment, R.id.sensorDetailFragment -> {
                    binding.viewPager.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.GONE
                    binding.navHostFragmentContentMain.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                }
                else -> {
                    binding.viewPager.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.navHostFragmentContentMain.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return navHostFragment?.navController?.navigateUp() ?: super.onSupportNavigateUp()
    }
}
