package com.example.gocekvo2max.history

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gocekvo2max.MainActivity
import com.example.gocekvo2max.R
import com.example.gocekvo2max.SettingActivity
import com.example.gocekvo2max.data.database.entity.BalkeEntity
import com.example.gocekvo2max.data.database.entity.RockPortEntity
import com.example.gocekvo2max.data.viewmodel.BalkeViewModel
import com.example.gocekvo2max.data.viewmodel.RockPortViewModel
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityChartBinding
import com.example.gocekvo2max.helper.OxygenLevelEvaluator
import com.example.gocekvo2max.profile.ProfileActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class ChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var rockPortViewModel: RockPortViewModel
    private lateinit var balkeViewModel: BalkeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        rockPortViewModel = ViewModelProvider(this)[RockPortViewModel::class.java]
        balkeViewModel = ViewModelProvider(this)[BalkeViewModel::class.java]

        navBottom()
        countPie()
        historyRecyclerView()
    }

    private fun setupPieChart(categories: List<String?>) {
        // Create your PieChart
        val pieChart: PieChart = binding.lineChart
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)

        pieChart.setHoleColor(Color.TRANSPARENT)

        // Set hole radius (adjust as needed)
        pieChart.holeRadius = 0f // Set the hole radius in dp
        // Set background color to transparent
        pieChart.transparentCircleRadius = 0f

        // Create Pie Entries
        val entries = ArrayList<PieEntry>()
        val categoryCounts = categories.groupingBy { it }.eachCount()
        val totalCount = categoryCounts.values.sum()

        categoryCounts.forEach { (category, count) ->
            val percentage = (count.toFloat() / totalCount) * 100
            val formattedPercentage = "${percentage.toInt()}%"
            // i change unknown to poor
            entries.add(PieEntry(percentage, "$formattedPercentage ${category ?: "Poor"}"))
        }


        if (categories.isEmpty()) {
            // If categories are empty, show a TextView instead of the PieChart
            pieChart.visibility = View.GONE
            binding.tvNoHistory.visibility = View.VISIBLE
            binding.tvNoHistory.text = "No History Yet"
            binding.rvHistory.visibility = View.GONE
            binding.tvHistory.visibility = View.GONE
            return
        } else {
            pieChart.visibility = View.VISIBLE
            binding.tvNoHistory.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            binding.tvHistory.visibility = View.VISIBLE
        }

        Log.d(TAG, "Categories: $categories")

        // Create DataSet
        val dataSet = PieDataSet(entries, getString(R.string.vo2max_categories))
        dataSet.colors = getColors()
        dataSet.setDrawValues(false)

        // Create PieData
        val data = PieData(dataSet)

        // Set Data to PieChart
        pieChart.data = data
        pieChart.invalidate() // Refresh the chart
    }

    private fun countPie() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->
                val userId = user?.userId.toString()
                Log.d(TAG, "userId: $userId")

                var balkeCategories: List<String?> = emptyList()
                var rockPortCategories: List<String?> = emptyList()

                balkeViewModel.oxygenConList.observe(this) { balkeOxygenConList ->
                    // Handle the updated oxygenConList here
                    balkeCategories = balkeOxygenConList.map { oxygenCon ->
                        categorizeOxygenCon(
                            oxygenCon?.toDouble(),
                            user?.gender
                                ?: "Unknown",  // Provide the correct default value for gender
                            user?.age ?: 0      // Provide the correct default value for age
                        )
                    }
                    Log.d(TAG, "Balke Categories: $balkeCategories")

                    setupPieChart(balkeCategories + rockPortCategories)
                }


                rockPortViewModel.oxygenConList.observe(this) { rockPortOxygenConList ->
                    // Handle the updated oxygenConList here
                    rockPortCategories =
                        rockPortOxygenConList.map { oxygenCon ->
                            categorizeOxygenCon(
                                oxygenCon?.toDouble(),
                                user?.gender ?: "Unknown",
                                user?.age ?: 0
                            )
                        }
                    Log.d(TAG, "Rock Port Categories: $rockPortCategories")

                    setupPieChart(balkeCategories + rockPortCategories)
                }
                // Trigger the ViewModel to fetch the data for the specific user
                balkeViewModel.getAllOxygenCon(userId)
                rockPortViewModel.getAllOxygenCon(userId)
            }
    }

    private fun categorizeOxygenCon(oxygenCon: Double?, gender: String?, age: Int): String {
        return when {
            oxygenCon == null -> "Poor" // or handle null values in a specific way
            gender.equals("Female", ignoreCase = true) -> OxygenLevelEvaluator.evaluateStatusFemale(
                age,
                oxygenCon
            )

            gender.equals("Male", ignoreCase = true) -> OxygenLevelEvaluator.evaluateStatusMale(
                age,
                oxygenCon
            )

            else -> "Unknown"
        }
    }

    private fun getColors(): List<Int> {
        // Define colors for each category
        return listOf(
            Color.parseColor("#83D630"), // Bright Green (Superior)
            Color.parseColor("#1A659E"), // Dark Green (Excellent)
            Color.parseColor("#004E89"), // Blue (Good)
            Color.parseColor("#FFBE0B"), // Yellow (Fair)
            Color.parseColor("#FF0000")  // Red (Poor)
        )
    }

//    private fun historyRecyclerView() {
//        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
//        val userEmail = sharedPreferences.getString("user_email", null)
//        val userPassword = sharedPreferences.getString("user_password", null)
//
//        val bSharedPreferences =
//            getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
//        bSharedPreferences.getString("id", null)
//
//        val rpSharedPreferences =
//            getSharedPreferences("rock_port_credentials", Context.MODE_PRIVATE)
//        rpSharedPreferences.getString("id", null)
//
//        recyclerView = binding.rvHistory
//        adapter = HistoryAdapter(
//            onDeleteClickListener = { position -> onDeleteItem(position) },
//            userGenderMap = emptyMap(),
//            userAgeMap = emptyMap()
//        )
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = adapter
//
//        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
//            .observe(this) { user ->
//                val userId = user?.userId.toString()
//                Log.d(TAG, "UserId: $userId")
//                Log.d(TAG, "User data for chart: $user")
//                val mergedList = mutableListOf<Any>()
//
//                balkeViewModel.balkeList.observe(this) { balkeList ->
//                    Log.d(TAG, "Balke data for chart: $balkeList")
//                    val userGenderMap = balkeList.associate { it.userId to user?.gender }
//                    val userAgeMap = balkeList.associate { it.userId to user?.age }
//                    val adapter = HistoryAdapter(
//                        { position -> onDeleteItem(position) },
//                        userGenderMap,
//                        userAgeMap
//                    )
//                    mergedList.addAll(balkeList)
//                    adapter.submitList(mergedList)
//                    recyclerView.adapter = adapter
//                }
//
//                balkeViewModel.getAllBalkeData(userId)
//
//                rockPortViewModel.rockPortList.observe(this) { rockPortList ->
//                    Log.d(TAG, "RockPort data for chart: $rockPortList")
//                    mergedList.addAll(rockPortList)
//                    adapter.submitList(mergedList)
//                }
//
//                rockPortViewModel.getAllRockPortData(userId)
//            }
//    }

    private fun historyRecyclerView() {
        Log.d(TAG, "Called!")
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val bSharedPreferences =
            getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
        bSharedPreferences.getString("id", null)

        val rpSharedPreferences =
            getSharedPreferences("rock_port_credentials", Context.MODE_PRIVATE)
        rpSharedPreferences.getString("id", null)

        recyclerView = binding.rvHistory
        adapter = HistoryAdapter(
            onDeleteClickListener = { position -> onDeleteItem(position) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->
                val userId = user?.userId.toString()
                Log.d(TAG, "UserId: $userId")
                Log.d(TAG, "User data for chart: $user")
                val mergedList = mutableListOf<Any>()

                val userGenderMap = mutableMapOf<Int, String?>()
                val userAgeMap = mutableMapOf<Int, Int?>()

                balkeViewModel.balkeList.observe(this) { balkeList ->
                    Log.d(TAG, "Balke data for chart: $balkeList")
                    userGenderMap.putAll(balkeList.associate { it.userId to user?.gender })
                    userAgeMap.putAll(balkeList.associate { it.userId to user?.age })

                    mergedList.addAll(balkeList)
                    adapter.updateUserMaps(userGenderMap, userAgeMap)
                    adapter.submitList(mergedList)
                }

                balkeViewModel.getAllBalkeData(userId)

                rockPortViewModel.rockPortList.observe(this) { rockPortList ->
                    Log.d(TAG, "RockPort data for chart: $rockPortList")
                    mergedList.addAll(rockPortList)
                    adapter.submitList(mergedList)
                }

                rockPortViewModel.getAllRockPortData(userId)
            }
    }

    private fun onDeleteItem(position: Int) {
        when (val selectedItem = adapter.historyList[position]) {
            is BalkeEntity -> {
                balkeViewModel.deleteBalkeDataById(selectedItem)
            }

            is RockPortEntity -> {
                rockPortViewModel.deleteRockPortData(selectedItem)
            }
            // Add more cases if you have other types
        }
        val updatedList = adapter.historyList.toMutableList()
        updatedList.removeAt(position)
//        adapter.submitList(updatedList)
        runOnUiThread {
            adapter.submitList(updatedList)
        }
        adapter.notifyItemRemoved(position)
    }

    private fun navBottom() {
        binding.navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_history -> {
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_setting -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }
        binding.navView.selectedItemId = R.id.action_history
    }

    companion object {
        const val TAG = "ChartActivity"
    }
}
