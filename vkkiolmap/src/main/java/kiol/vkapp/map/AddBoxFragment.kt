package kiol.vkapp.map

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.domain.Box
import kiol.vkapp.commondata.domain.BoxType
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.GMapFragment.Companion.placesUseCase
import kiol.vkapp.map.databinding.AddBoxLayoutBinding
import kiol.vkapp.map.databinding.CamLayoutBinding
import kiol.vkapp.map.databinding.DescriptionDialogBinding

class AddBoxFragment : Fragment(R.layout.add_box_layout), CamFragment.OnPictureListener {
    companion object {
        private const val ADDR = "addr"

        fun create(addr: String): AddBoxFragment {
            return AddBoxFragment().apply {
                arguments = Bundle().apply {
                    putString(ADDR, addr)
                }
            }
        }
    }

    private val binding by viewLifecycleLazy {
        AddBoxLayoutBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction().replace(R.id.camFrame, CamFragment()).commitAllowingStateLoss()

        binding.sendBtn.setOnClickListener {
            photoUri?.let {
                placesUseCase.addBoxForCheck(requireArguments().getString(ADDR).orEmpty(), it)
                parentFragmentManager.popBackStack()
            }
        }
    }

    private var photoUri: Uri? = null

    override fun onTaken(uri: Uri) {
        photoUri = uri
        binding.address.text = requireArguments().getString(ADDR)
        binding.previewIcon.load(uri) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }
}