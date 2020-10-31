package kiol.vkapp.map.unsubscribe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commondata.data.VKGroup
import kiol.vkapp.commondata.domain.groups.GroupsUseCase
import kiol.vkapp.commonui.SpaceItemDecoration
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.R
import kiol.vkapp.map.databinding.UnsubscribeDialogBinding
import kiol.vkapp.map.plusAssign

class UnsubscribeDialog : BottomSheetDialogFragment() {

    companion object {
        private const val BoxGroups = "boxgroups"
        fun create(boxGroups: List<VKGroup>): UnsubscribeDialog {
            return UnsubscribeDialog().apply {
                arguments = bundleOf(BoxGroups to boxGroups)
            }
        }
    }

    private val binding by viewLifecycleLazy {
        UnsubscribeDialogBinding.bind(requireView())
    }

    private val compositeDisposable = CompositeDisposable()

    private val groupsUseCase = GroupsUseCase()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.unsubscribe_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.groups.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.groups_margin)))
        binding.groups.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        binding.unsubscribeBtn.isEnabled = false
        binding.unsubscribeBtn.setOnClickListener {
            handleUnsubsribe()
        }

        binding.close.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.progress.visibility = View.VISIBLE
        binding.unsubscribeBtn.visibility = View.INVISIBLE
        loadGroups()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    private fun loadGroups() {
        val groups = requireArguments().getParcelableArrayList<VKGroup>(BoxGroups)!!.toList()
        compositeDisposable += groupsUseCase.getGroups(groups).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isEmpty()) {
                    handleNoItems()
                } else {
                    binding.progress.visibility = View.GONE
                    binding.unsubscribeBtn.visibility = View.VISIBLE
                    showGroups(it)
                }
            }, {
                Toast.makeText(requireContext(), R.string.subscribed_groups_error, Toast.LENGTH_SHORT).show()
                handleNoItems()
            })
    }

    private fun showGroups(groups: List<VKGroup>) {
        binding.groups.adapter = UnsubscribeGroupsAdapter(groups) { _, selected ->
            binding.unsubscribeBtn.isEnabled = selected.isNotEmpty()
            if (selected.isNotEmpty()) {
                binding.unsubscribeBtn.icon = BadgeDrawable().apply {
                    setCount(selected.size)
                }
            } else {
                binding.unsubscribeBtn.icon = null
            }
        }
    }

    private fun handleNoItems() {
        binding.progress.visibility = View.GONE
        binding.unsubscribeBtn.visibility = View.GONE
        binding.noGroups.visibility = View.VISIBLE
    }

    private fun handleUnsubsribe() {
        with((binding.groups.adapter as UnsubscribeGroupsAdapter)) {
            val newList = removeSelectedGroups()
            binding.unsubscribeBtn.icon = null
            if (newList.isEmpty()) {
                handleNoItems()
            }
        }
    }
}