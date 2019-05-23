package com.rico.omarw.rutasuruapan

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter(fm: FragmentManager?, private val fragments: List<Fragment>) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int) = fragments[position]
    override fun getCount() = fragments.size
    override fun getPageTitle(position: Int) =
            when (fragments[position]){
                is ControlPanelFragment -> "Find Route"
                is AllRoutesFragment -> "All routes"
                else -> "fragment[$position]"
            }





}