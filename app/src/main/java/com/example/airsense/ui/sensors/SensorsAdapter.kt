package com.example.airsense.ui.sensors

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

        holder.binding.tvItemSensorName.text = sensor.name
        holder.binding.tvItemSensorType.text = "${sensor.type} • ID: ${sensor.id}"
        holder.binding.tvItemSensorReading.text = sensor.lastReading

        if (sensor.isOnline) {
            holder.binding.tvItemSensorStatus.text = "• En Línea (${sensor.batteryLevel}% Bat)"
            holder.binding.tvItemSensorStatus.setTextColor(
                ContextCompat.getColor(context, R.color.status_good)
            )
        } else {
            holder.binding.tvItemSensorStatus.text = "• Desconectado"
            holder.binding.tvItemSensorStatus.setTextColor(
                ContextCompat.getColor(context, R.color.airsense_text_muted)
            )
        }

        val iconRes = when {
            sensor.type.contains("AQI", ignoreCase = true) -> R.drawable.ic_air_quality
            sensor.type.contains("Temp", ignoreCase = true) -> R.drawable.ic_temp
            sensor.type.contains("Humedad", ignoreCase = true) -> R.drawable.ic_humidity
            else -> R.drawable.ic_iot
        }
        holder.binding.imgSensorTypeIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onSensorClick(sensor)
        }
    }

    override fun getItemCount(): Int = sensors.size
}
