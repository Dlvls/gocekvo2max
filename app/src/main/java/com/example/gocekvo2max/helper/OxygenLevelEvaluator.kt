package com.example.gocekvo2max.helper

object OxygenLevelEvaluator {

    fun evaluateStatusFemale(age: Int, vo2max: Double): String {
        return when (age) {
            in 20..29 -> evaluateForAgeGroup(vo2max, 36.0, 39.0, 43.0, 49.0)
            in 30..39 -> evaluateForAgeGroup(vo2max, 34.0, 36.0, 40.0, 45.0)
            in 40..49 -> evaluateForAgeGroup(vo2max, 32.0, 34.0, 38.0, 44.0)
            in 50..59 -> evaluateForAgeGroup(vo2max, 25.0, 28.0, 30.0, 34.0)
            in 60..69 -> evaluateForAgeGroup(vo2max, 26.0, 28.0, 31.0, 35.0)
            in 70..79 -> evaluateForAgeGroup(vo2max, 24.0, 26.0, 29.0, 35.0)
            else -> "Poor"
        }
    }

    fun evaluateStatusMale(age: Int, vo2max: Double): String {
        return when (age) {
            in 20..29 -> evaluateForAgeGroup(vo2max, 42.0, 45.0, 50.0, 55.0)
            in 30..39 -> evaluateForAgeGroup(vo2max, 41.0, 43.0, 47.0, 53.0)
            in 40..49 -> evaluateForAgeGroup(vo2max, 38.0, 41.0, 45.0, 52.0)
            in 50..59 -> evaluateForAgeGroup(vo2max, 35.0, 37.0, 42.0, 49.0)
            in 60..69 -> evaluateForAgeGroup(vo2max, 31.0, 34.0, 38.0, 45.0)
            in 70..79 -> evaluateForAgeGroup(vo2max, 28.0, 30.0, 35.0, 41.0)
            else -> "Poor"
        }
    }

    private fun evaluateForAgeGroup(
        vo2max: Double,
        poor: Double,
        fair: Double,
        good: Double,
        excellent: Double
    ): String {
        return when {
            vo2max > excellent -> "Superior"
            vo2max >= good -> "Excellent"
            vo2max >= fair -> "Good"
            vo2max >= poor -> "Fair"
            else -> "Poor"
        }
    }
}