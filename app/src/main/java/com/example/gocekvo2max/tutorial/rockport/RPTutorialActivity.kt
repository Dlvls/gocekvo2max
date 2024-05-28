package com.example.gocekvo2max.tutorial.rockport

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.gocekvo2max.R
import com.example.gocekvo2max.databinding.ActivityRptutorialBinding

class RPTutorialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRptutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRptutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewPager: ViewPager = findViewById(R.id.viewPager)
        val adapter = RPTutorialPagerAdapter(supportFragmentManager)
        viewPager.adapter = adapter
    }
}