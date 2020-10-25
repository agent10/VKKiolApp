package kiol.vkappvkkiolapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.docs.MainFragment
import kiol.vkapp.map.GMapFragment
import kiol.vkappvkkiolapp.databinding.EntryTempFragmentBinding

class EntryFragment : Fragment(R.layout.entry_temp_fragment) {

    private val binding by viewLifecycleLazy {
        EntryTempFragmentBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, GMapFragment())
            .commitAllowingStateLoss()
    }
}