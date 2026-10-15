package com.example.airsense.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.FragmentSettingsBinding
import com.example.airsense.notifications.NotificationHelper
import com.example.airsense.ui.SharedAirSenseViewModel
import com.example.airsense.utils.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedAirSenseViewModel by activityViewModels()
    private lateinit var userPrefs: UserPreferencesManager

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            userPrefs.setNotificationsEnabled(isGranted)
            _binding?.switchNotifications?.isChecked = isGranted
            updatePermissionStatuses()
            val msgRes = if (isGranted) {
                R.string.notification_permission_granted
            } else {
                R.string.notification_permission_denied
            }
            Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
        }
    }

    private val requestAllPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        lifecycleScope.launch {
            val hasNotif = PermissionHelper.hasNotificationPermission(requireContext())
            userPrefs.setNotificationsEnabled(hasNotif)
            _binding?.switchNotifications?.isChecked = hasNotif
            updatePermissionStatuses()
            Toast.makeText(
                requireContext(),
                "Permisos procesados correctamente",
                Toast.LENGTH_SHORT
            ).show()
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

        setupTestNotificationButton()
        setupSystemPermissionsSection()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.thresholds.collect { _ ->
                        // Umbrales sincronizados
                    }
                }
                launch {
                    userPrefs.isNotificationsEnabledFlow.collect { isPrefEnabled ->
                        val isPermissionGranted = PermissionHelper.hasNotificationPermission(requireContext())
                        _binding?.switchNotifications?.let { switch ->
                            switch.setOnCheckedChangeListener(null)
                            val targetChecked = isPrefEnabled && isPermissionGranted
                            if (switch.isChecked != targetChecked) {
                                switch.isChecked = targetChecked
                                switch.jumpDrawablesToCurrentState()
                            }
                            setupNotificationSwitchListener(switch)
                        }
                    }
                }
                launch {
                    userPrefs.themeModeFlow.collect { currentMode ->
                        _binding?.toggleGroupTheme?.let { group ->
                            group.clearOnButtonCheckedListeners()
                            val targetBtnId = when (currentMode) {
                                UserPreferencesManager.THEME_LIGHT -> R.id.btn_theme_light
                                UserPreferencesManager.THEME_DARK -> R.id.btn_theme_dark
                                else -> R.id.btn_theme_system
                            }
                            if (group.checkedButtonId != targetBtnId) {
                                group.check(targetBtnId)
                            }
                            setupThemeGroupListener(group)
                        }
                    }
                }
                launch {
                    userPrefs.useFahrenheitFlow.collect { useFahrenheit ->
                        updateUnitButtonText(useFahrenheit)
                        setupUnitToggleListener()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
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

    private fun setupNotificationSwitchListener(switchButton: CompoundButton) {
        switchButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnCheckedChangeListener
                }
            }

            lifecycleScope.launch {
                userPrefs.setNotificationsEnabled(isChecked)
            }
        }
    }

    private fun setupThemeGroupListener(group: com.google.android.material.button.MaterialButtonToggleGroup) {
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btn_theme_light -> UserPreferencesManager.THEME_LIGHT
                    R.id.btn_theme_dark -> UserPreferencesManager.THEME_DARK
                    else -> UserPreferencesManager.THEME_SYSTEM
                }

                lifecycleScope.launch {
                    userPrefs.setThemeMode(newMode)
                    val targetNightMode = when (newMode) {
                        UserPreferencesManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        UserPreferencesManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
                        AppCompatDelegate.setDefaultNightMode(targetNightMode)
                    }
                }
            }
        }
    }

    private fun setupSystemPermissionsSection() {
        updatePermissionStatuses()

        binding.btnRequestAllPermissions.setOnClickListener {
            val missing = PermissionHelper.getMissingPermissions(requireContext())
            if (missing.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.toast_permissions_all_granted,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                requestAllPermissionsLauncher.launch(missing)
            }
        }
    }

    private fun updatePermissionStatuses() {
        val context = context ?: return

        val hasBt = PermissionHelper.hasBluetoothPermissions(context)
        val hasLoc = PermissionHelper.hasLocationPermissions(context)
        val hasNotif = PermissionHelper.hasNotificationPermission(context)

        val grantedText = getString(R.string.settings_perm_granted)
        val deniedText = getString(R.string.settings_perm_denied)

        val colorPrimary = ContextCompat.getColor(context, R.color.airsense_primary)
        val colorError = ContextCompat.getColor(context, android.R.color.holo_red_dark)

        _binding?.tvPermBluetoothStatus?.apply {
            text = if (hasBt) grantedText else deniedText
            setTextColor(if (hasBt) colorPrimary else colorError)
        }

        _binding?.tvPermLocationStatus?.apply {
            text = if (hasLoc) grantedText else deniedText
            setTextColor(if (hasLoc) colorPrimary else colorError)
        }

        _binding?.tvPermNotificationsStatus?.apply {
            text = if (hasNotif) grantedText else deniedText
            setTextColor(if (hasNotif) colorPrimary else colorError)
        }
    }

    private fun setupUnitToggleListener() {
        val toggleAction = View.OnClickListener {
            lifecycleScope.launch {
                val current = userPrefs.useFahrenheitFlow.first()
                val nextValue = !current
                userPrefs.setUseFahrenheit(nextValue)

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
