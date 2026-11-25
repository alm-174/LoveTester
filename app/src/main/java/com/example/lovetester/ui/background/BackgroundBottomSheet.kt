
package com.example.lovetester.ui.background

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lovetester.R
import com.example.lovetester.data.local.BackgroundImageStorage
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.BottomSheetBackgroundBinding
import com.example.lovetester.ui.common.GridSpacingDecoration
import com.example.lovetester.ui.common.expandWithMaxHeight
import com.example.lovetester.ui.common.toast
import com.example.lovetester.ui.common.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var repository: LoveSettingsRepository

    @Inject
    lateinit var storage: BackgroundImageStorage

    private val binding by viewBinding(BottomSheetBackgroundBinding::bind)

    private val backgrounds = listOf(
        BackgroundItem.AddPhoto,
        BackgroundItem.BuiltIn(
            id = "romantic_1",
            drawableRes = R.drawable.background_romantic_1
        ),
        BackgroundItem.BuiltIn(
            id = "romantic_2",
            drawableRes = R.drawable.background_romantic_2
        ),
        BackgroundItem.BuiltIn(
            id = "romantic_3",
            drawableRes = R.drawable.background_romantic_3
        )
    )

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedPhoto(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_background, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupBackgroundList()
    }

    override fun onStart() {
        super.onStart()
        expandWithMaxHeight(R.dimen.sheet_max_height_background)
    }

    override fun onDestroyView() {
        binding.rvBackgrounds.adapter = null
        super.onDestroyView()
    }

    private fun setupBackgroundList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val paths = storage.getSavedImages()

            val customItems = paths.map { path ->
                BackgroundItem.Custom(path)
            }

            val allItems = backgrounds + customItems

            val adapter = BackgroundAdapter(
                items = allItems,
                selectedId = repository.selectedBackgroundId.first()
            ) { item ->
                when (item) {
                    BackgroundItem.AddPhoto -> {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }

                    is BackgroundItem.BuiltIn -> {
                        saveSelectedBackground(item.id)
                    }

                    is BackgroundItem.Custom -> {
                        saveSelectedBackground("custom:${item.path}")
                    }
                }
            }

            binding.rvBackgrounds.layoutManager =
                GridLayoutManager(requireContext(), SPAN_COUNT)

            val spacing =
                (12 * resources.displayMetrics.density + 0.5f).toInt()

            if (binding.rvBackgrounds.itemDecorationCount == 0) {
                binding.rvBackgrounds.addItemDecoration(
                    GridSpacingDecoration(spacing, SPAN_COUNT)
                )
            }

            binding.rvBackgrounds.adapter = adapter
        }
    }

    private fun handleSelectedPhoto(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val path = storage.saveImage(uri)

                repository.setSelectedBackgroundId("custom:$path")

                dismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                context?.toast(R.string.background_save_error)
            }
        }
    }

    private fun saveSelectedBackground(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.setSelectedBackgroundId(id)

            dismiss()
        }
    }

    private companion object {
        const val SPAN_COUNT = 2
    }
}
