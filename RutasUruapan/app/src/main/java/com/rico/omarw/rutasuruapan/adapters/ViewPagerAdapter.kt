package com.rico.omarw.rutasuruapan.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.rico.omarw.rutasuruapan.AllRoutesFragment
import com.rico.omarw.rutasuruapan.ControlPanelFragment
import com.rico.omarw.rutasuruapan.ResultsFragment
import com.rico.omarw.rutasuruapan.SearchFragment

class ViewPagerAdapter(fm: FragmentManager?, private var fragments: Array<Fragment>) : FragmentPagerAdapter(fm!!) {

    override fun getItem(position: Int) = fragments[position]
    override fun getCount() = fragments.size
    override fun getPageTitle(position: Int) =
            when (fragments[position]){
                is ControlPanelFragment -> "Find Route"
                is AllRoutesFragment -> "Todas las rutas"
                is SearchFragment -> "Búsqueda"
                is ResultsFragment -> "Results"
                else -> "fragment[$position]"
            }

    public fun replace(fragment: Fragment, position: Int){
        fragments[position] = fragment
        notifyDataSetChanged()
    }





}