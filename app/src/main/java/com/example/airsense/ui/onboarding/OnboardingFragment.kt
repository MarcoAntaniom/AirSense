package com.example.airsense.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private lateinit var userPrefs: UserPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        userPrefs = UserPreferencesManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val slides = listOf(
            OnboardingSlide(
                R.drawable.app_logo,
                "Monitoreo en Cocinas Profesionales",
                "Supervisa en tiempo real los niveles de gas de combustión (CO₂, CO, CH₂O) y calidad de aire en tu área culinaria."
            ),
            OnboardingSlide(
                R.drawable.ic_air_quality,
                "Prioridad Operacional & Seguridad",
                "Distingue al instante estados Óptimo, Moderado o Peligroso para gestionar la extracción y proteger al equipo de trabajo."
            ),
            OnboardingSlide(
                R.drawable.ic_iot,
                "Telemetría Confiable en Vivo",
                "Conexión persistente con sensores IoT y Firebase. Lecturas confirmadas en tiempo real sin falsos positivos."
            )
        )

        val adapter = OnboardingAdapter(slides)
        binding.viewPagerOnboarding.adapter = adapter

        binding.btnNextOnboarding.setOnClickListener {
            val current = binding.viewPagerOnboarding.currentItem
            if (current < slides.size - 1) {
                binding.viewPagerOnboarding.currentItem = current + 1
            } else {
                lifecycleScope.launch {
                    userPrefs.setFirstRunCompleted()
                    findNavController().navigate(R.id.action_onboardingFragment_to_navigation_home)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
