package kiol.vkappvkkiolapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.docs.MainFragment
import kiol.vkappvkkiolapp.databinding.EntryTempFragmentBinding

class EntryFragment : Fragment(R.layout.entry_temp_fragment) {

    private val binding by viewLifecycleLazy {
        EntryTempFragmentBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bttmNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_docs_id -> {
                    childFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, MainFragment())
                        .commitAllowingStateLoss()
                }
                R.id.nav_map_id -> {
                    childFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, MainFragment())
                        .commitAllowingStateLoss()
                }
            }
            true
        }
        binding.bttmNav.selectedItemId = R.id.nav_map_id
    }
}