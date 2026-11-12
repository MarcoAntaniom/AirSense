package com.example.airsense.ui.sensors

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.airsense.R
import com.example.airsense.data.models.SensorDevice
import com.example.airsense.databinding.ItemSensorCardBinding

class SensorsAdapter(
    private var sensors: List<SensorDevice> = emptyList(),
    private val onSensorClick: (SensorDevice) -> Unit
) : RecyclerView.Adapter<SensorsAdapter.SensorViewHolder>() {

    fun updateSensors(newSensors: List<SensorDevice>) {
        sensors = newSensors
        notifyDataSetChanged()
    }

    inner class SensorViewHolder(val binding: ItemSensorCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val binding = ItemSensorCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SensorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val sensor = sensors[position]
        val context = holder.itemView.context

        holder.binding.tvSensorName.text = sensor.name
        holder.binding.tvSensorType.text = sensor.type
        holder.binding.tvSensorMainMetric.text = sensor.lastReading

        val statusColor = if (sensor.isOnline) {
            ContextCompat.getColor(context, R.color.aqi_good)
        } else {
            ContextCompat.getColor(context, R.color.text_secondary)
        }
        holder.binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(statusColor)

        val iconRes = when {
            sensor.name.contains("AQI", ignoreCase = true) || sensor.type.contains("AQI", ignoreCase = true) -> R.drawable.ic_air_quality
            sensor.name.contains("Temp", ignoreCase = true) || sensor.type.contains("Temp", ignoreCase = true) -> R.drawable.ic_temp
            sensor.name.contains("Humedad", ignoreCase = true) || sensor.type.contains("Humedad", ignoreCase = true) -> R.drawable.ic_humidity
            sensor.name.contains("Carbono", ignoreCase = true) || sensor.name.contains("Formaldehído", ignoreCase = true) -> R.drawable.ic_sensors
            else -> R.drawable.ic_iot
        }
        holder.binding.ivSensorIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onSensorClick(sensor)
        }
    }

    override fun getItemCount(): Int = sensors.size
}
