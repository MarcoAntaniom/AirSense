package com.example.airsense.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.airsense.ui.home.HomeFragment
import com.example.airsense.ui.recommendations.RecommendationsFragment
import com.example.airsense.ui.sensors.SensorsFragment
import com.example.airsense.ui.settings.SettingsFragment

/**
 * FragmentStateAdapter que gestiona los fragmentos de las secciones principales de AirSense:
 * 0: Inicio (HomeFragment)
 * 1: Sensores (SensorsFragment)
 * 2: Recomendaciones (RecommendationsFragment)
 * 3: Ajustes (SettingsFragment)
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> SensorsFragment()
            2 -> RecommendationsFragment()
            3 -> SettingsFragment()
            else -> HomeFragment()
        }
    }
}
