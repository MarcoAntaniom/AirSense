package com.example.airsense.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.airsense.databinding.ItemOnboardingSlideBinding

data class OnboardingSlide(
    val iconRes: Int,
    val title: String,
    val description: String
)

class OnboardingAdapter(
    private val slides: List<OnboardingSlide>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: ItemOnboardingSlideBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val slide = slides[position]
        holder.binding.imgSlideIcon.setImageResource(slide.iconRes)
        holder.binding.tvSlideTitle.text = slide.title
        holder.binding.tvSlideDescription.text = slide.description
    }

    override fun getItemCount(): Int = slides.size
}