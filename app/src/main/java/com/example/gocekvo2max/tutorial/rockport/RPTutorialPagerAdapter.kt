package com.example.gocekvo2max.tutorial.rockport

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.gocekvo2max.R
import com.example.gocekvo2max.tutorial.balke.slider.BTutorialFragment

class RPTutorialPagerAdapter(fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val layoutResIds = listOf(
        R.layout.tutorial_rock_port_1,
        R.layout.tutorial_rock_port_2,
        R.layout.tutorial_rock_port_3,
        R.layout.tutorial_rock_port_4,
        R.layout.tutorial_rock_port_5,
        R.layout.tutorial_rock_port_6,
        R.layout.tutorial_rock_port_7,
        R.layout.tutorial_rock_port_8,
        R.layout.tutorial_rock_port_9,
    )

    override fun getItem(position: Int): Fragment {
        return BTutorialFragment.newInstance(layoutResIds[position])
    }

    override fun getCount(): Int {
        return layoutResIds.size
    }
}