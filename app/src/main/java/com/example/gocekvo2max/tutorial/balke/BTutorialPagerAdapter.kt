package com.example.gocekvo2max.tutorial.balke

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.gocekvo2max.R
import com.example.gocekvo2max.tutorial.balke.slider.BTutorialFragment

class BTutorialPagerAdapter(fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val layoutResIds = listOf(
        R.layout.tutorial_balke_1,
        R.layout.tutorial_balke_2,
        R.layout.tutorial_balke_3,
        R.layout.tutorial_balke_4,
        R.layout.tutorial_balke_5,
        R.layout.tutorial_balke_6,
        R.layout.tutorial_balke_7,
        R.layout.tutorial_balke_8,
    )

    override fun getItem(position: Int): Fragment {
        return BTutorialFragment.newInstance(layoutResIds[position])
    }

    override fun getCount(): Int {
        return layoutResIds.size
    }
}