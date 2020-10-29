package kiol.vkapp.map

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.GMapFragment.Companion.placesUseCase
import kiol.vkapp.map.databinding.AddBoxLayoutBinding

class AddBoxFragment : Fragment(R.layout.add_box_layout), CamFragment.OnPictureListener {
    companion object {
        private const val ADDR = "addr"

        fun create(addr: String): AddBoxFragment {
            return AddBoxFragment().apply {
                arguments = bundleOf(ADDR to addr)
            }
        }
    }

    private val binding by viewLifecycleLazy {
        AddBoxLayoutBinding.bind(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction().replace(R.id.camFrame, CamFragment()).commitAllowingStateLoss()

        binding.address.text = requireArguments().getString(ADDR)

        binding.sendBtn.apply {
            setOnApplyWindowInsetsListener { v, insets ->
                view.updatePadding(bottom = insets.systemWindowInsetBottom)
                insets
            }
        }

        binding.sendBtn.setOnClickListener {
            if (photoUri == null) {
                Toast.makeText(requireContext(), R.string.no_photo_error, Toast.LENGTH_SHORT).show()
            }

            photoUri?.let {
                placesUseCase.addBoxForCheck(requireArguments().getString(ADDR).orEmpty(), it)
                parentFragmentManager.popBackStack()
            }
        }
    }

    private var photoUri: Uri? = null

    override fun onTaken(uri: Uri) {
        photoUri = uri
        binding.previewIcon.load(uri) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }
}