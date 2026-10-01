package com.example.airsense.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.FragmentSettingsBinding
import com.example.airsense.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var userPrefs: UserPreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            userPrefs.setNotificationsEnabled(isGranted)
            _binding?.switchNotifications?.isChecked = isGranted
            val msgRes = if (isGranted) {
                R.string.notification_permission_granted
            } else {
                R.string.notification_permission_denied
            }
            Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        userPrefs = UserPreferencesManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNotificationSwitch()
        setupTestNotificationButton()
        setupDarkModeSwitch()
        setupUnitToggle()
    }

    private fun setupTestNotificationButton() {
        binding.btnTestNotification.setOnClickListener {
            val notificationHelper = NotificationHelper(requireContext())
            val sent = notificationHelper.sendTestNotification()
            val message = if (sent) {
                "Notificación de prueba enviada exitosamente"
            } else {
                "No se pudo enviar la notificación. Verifica que los permisos y notificaciones estén activos."
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNotificationSwitch() {
        lifecycleScope.launch {
            val isPrefEnabled = userPrefs.isNotificationsEnabledFlow.first()
            val isPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            _binding?.switchNotifications?.let { switch ->
                switch.setOnCheckedChangeListener(null)
                val targetChecked = isPrefEnabled && isPermissionGranted
                if (switch.isChecked != targetChecked) {
                    switch.isChecked = targetChecked
                    switch.jumpDrawablesToCurrentState()
                }

                switch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@setOnCheckedChangeListener
                        }
                    }

                    lifecycleScope.launch {
                        userPrefs.setNotificationsEnabled(isChecked)
                    }
                }
            }
        }
    }

    private fun setupDarkModeSwitch() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val isCurrentDark = currentNightMode == AppCompatDelegate.MODE_NIGHT_YES

        _binding?.switchDarkMode?.let { switch ->
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = isCurrentDark
            switch.jumpDrawablesToCurrentState()
        }

        lifecycleScope.launch {
            val isDarkPref = userPrefs.isDarkModeFlow.first()
            _binding?.switchDarkMode?.let { switch ->
                switch.setOnCheckedChangeListener(null)
                if (switch.isChecked != isDarkPref) {
                    switch.isChecked = isDarkPref
                    switch.jumpDrawablesToCurrentState()
                }

                switch.setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        userPrefs.setDarkMode(isChecked)
                        val newMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                        if (AppCompatDelegate.getDefaultNightMode() != newMode) {
                            AppCompatDelegate.setDefaultNightMode(newMode)
                        }
                    }
                }
            }
        }
    }

    private fun setupUnitToggle() {
        lifecycleScope.launch {
            val useFahrenheit = userPrefs.useFahrenheitFlow.first()
            updateUnitButtonText(useFahrenheit)
        }

        val toggleAction = View.OnClickListener {
            lifecycleScope.launch {
                val current = userPrefs.useFahrenheitFlow.first()
                val nextValue = !current
                userPrefs.setUseFahrenheit(nextValue)
                updateUnitButtonText(nextValue)

                val message = if (nextValue) {
                    "Unidad cambiada a Fahrenheit (°F)"
                } else {
                    "Unidad cambiada a Celsius (°C)"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnUnitToggle.setOnClickListener(toggleAction)
        binding.layoutUnitSelection.setOnClickListener(toggleAction)
    }

    private fun updateUnitButtonText(useFahrenheit: Boolean) {
        _binding?.btnUnitToggle?.text = if (useFahrenheit) "Fahrenheit (°F)" else "Celsius (°C)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}