package com.example.gocekvo2max.tutorial.rockport.slider

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.gocekvo2max.MainActivity
import com.example.gocekvo2max.R
import com.example.gocekvo2max.tutorial.balke.slider.BTutorialFragment

class RPTutorialFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_RES_ID = "layout_res_id"

        fun newInstance(layoutResId: Int): BTutorialFragment {
            val fragment = BTutorialFragment()
            val args = Bundle()
            args.putInt(ARG_LAYOUT_RES_ID, layoutResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutResId = arguments?.getInt(ARG_LAYOUT_RES_ID, R.layout.tutorial_rock_port_1)
            ?: R.layout.tutorial_rock_port_1

        val view = inflater.inflate(layoutResId, container, false)

        // Find and set click listener for icHome
        val icHome = view.findViewById<ImageView>(R.id.icHome)
        icHome?.setOnClickListener {
            // Handle the click event for icHome
            navigateToMainActivity()
        }

        return view
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(requireActivity(), MainActivity::class.java))
    }
}