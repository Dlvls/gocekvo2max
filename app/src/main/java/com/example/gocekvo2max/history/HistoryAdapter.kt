package com.example.gocekvo2max.history

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gocekvo2max.data.database.entity.BalkeEntity
import com.example.gocekvo2max.data.database.entity.RockPortEntity
import com.example.gocekvo2max.databinding.ItemHistoryBinding
import com.example.gocekvo2max.helper.OxygenLevelEvaluator
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val onDeleteClickListener: (Int) -> Unit,
) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    var historyList: List<Any> = ArrayList()
    private var userGenderMap: Map<Int, String?> = emptyMap()
    private var userAgeMap: Map<Int, Int?> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        when (val currentItem = historyList[position]) {
            is BalkeEntity -> {
                // Handle BalkeEntity
                holder.binding.tvName.text = "Balke"
                val durationInMinutes = currentItem.balkeDuration?.div(60000)
                holder.binding.tvValue.text = "$durationInMinutes min"

                val dateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                holder.binding.tvDate.text =
                    dateFormatter.format(currentItem.currentDate ?: System.currentTimeMillis())
                holder.binding.tvHeartRate.visibility = View.GONE

                val vo2max = currentItem.oxygenCon?.toDoubleOrNull() ?: 0.0
                Log.d("HistoryAdapter", "VO2MAX value in adapter: $vo2max")

                val userId = when (currentItem) {
                    is BalkeEntity -> currentItem.userId
                    is RockPortEntity -> currentItem.userId
                    else -> null
                }

                val gender = userId?.let { userGenderMap[it] }
                val age = userId?.let { userAgeMap[it] }

//                val oxyCon = currentItem.oxygenCon!!.toDouble()
                val oxyCon = currentItem.oxygenCon?.toDoubleOrNull() ?: 0.0
                holder.binding.tvOxygenLevel.text = "Oxygen Level: ${oxyCon.formatToOneDecimal()}"

                Log.d(TAG, "age: $age, gender: $gender, oxygenLevel: $oxyCon")

                holder.binding.tvOxygenLevel.setTextColor(getVo2maxColor(oxyCon, gender, age))
            }

            is RockPortEntity -> {
                // Handle RockPortEntity
                holder.binding.tvName.text = "Rock Port"
//                holder.binding.tvValue.text = "${currentItem.rockportDistance} km"
                holder.binding.tvValue.text = String.format("%.1f km", currentItem.rockportDistance)

                val dateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                holder.binding.tvDate.text =
                    dateFormatter.format(currentItem.currentDate ?: System.currentTimeMillis())

                // Add more views and data as needed for RockPortEntity
                holder.binding.tvHeartRate.text =
                    "Heart Rate:  ${currentItem.heartBeats?.toString()} bpm"

                val vo2max = currentItem.oxygenCon?.toDoubleOrNull() ?: 0.0

                val userId = when (currentItem) {
                    is BalkeEntity -> currentItem.userId
                    is RockPortEntity -> currentItem.userId
                    else -> null
                }
                val gender = userId?.let { userGenderMap[it] }
                val age = userId?.let { userAgeMap[it] }

//                val oxyCon = currentItem.oxygenCon!!.toDouble()
                val oxyCon = currentItem.oxygenCon?.toDoubleOrNull() ?: 0.0
                holder.binding.tvOxygenLevel.text = "Oxygen Level: ${oxyCon.formatToOneDecimal()}"

                Log.d(TAG, "age: $age, gender: $gender, oxygenLevel: $oxyCon")

                holder.binding.tvOxygenLevel.setTextColor(getVo2maxColor(oxyCon, gender, age))
            }
        }
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    fun submitList(historyList: List<Any>) {
        this.historyList = historyList
        notifyDataSetChanged()
    }

    fun updateUserMaps(userGenderMap: Map<Int, String?>, userAgeMap: Map<Int, Int?>) {
        this.userGenderMap = userGenderMap
        this.userAgeMap = userAgeMap
        notifyDataSetChanged()
    }

    private fun Double.formatToOneDecimal(): String {
        return String.format("%.1f", this)
    }

    private fun getVo2maxColor(oxyCon: Double?, gender: String?, age: Int?): Int {
        Log.d(TAG, "gender: $gender, age: $age")

        return when {
            oxyCon != null && gender != null && age != null -> {
                val genderInt = when (gender) {
                    "Female" -> 1 // Female
                    "Male" -> 0 // Male
                    else -> -1 // Use -1 or any other value to represent an unknown or invalid gender
                }

                val status = when (genderInt) {
                    0 -> OxygenLevelEvaluator.evaluateStatusMale(age, oxyCon)
                    1 -> OxygenLevelEvaluator.evaluateStatusFemale(age, oxyCon)
                    else -> "Unknown"
                }

                getColorForStatus(status)
            }

            else -> Color.RED // Default color or any color you want for null values
        }
    }

    private fun getColorForStatus(status: String): Int {
        return when (status) {
            "Superior" -> Color.parseColor("#A1C181") // Green (Superior)
            "Excellent" -> Color.parseColor("#008000") // Dark Green (Excellent)
            "Good" -> Color.parseColor("#0000FF") // Blue (Good)
            "Fair" -> Color.parseColor("#FFFF00") // Yellow (Fair)
            "Poor" -> Color.parseColor("#FF0000") // Red (Poor)
            else -> Color.RED // Default color or any color you want
        }
    }

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.icDelete.setOnClickListener {
                onDeleteClickListener.invoke(adapterPosition)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val item = historyList[position]) {
            is BalkeEntity -> item.bTrackerId.toLong() // Assuming BalkeEntity has an 'id' property
            is RockPortEntity -> item.rpTrackerId.toLong() // Assuming RockPortEntity has an 'id' property
            else -> RecyclerView.NO_ID
        }
    }

    companion object {
        const val TAG = "HistoryAdapter"
    }
}
