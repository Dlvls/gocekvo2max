package com.example.gocekvo2max.tutorial.balke

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.gocekvo2max.R
import com.example.gocekvo2max.databinding.ActivityBtutorialBinding

class BTutorialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBtutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBtutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewPager: ViewPager = findViewById(R.id.viewPager)
        val adapter = BTutorialPagerAdapter(supportFragmentManager)
        viewPager.adapter = adapter
    }
}