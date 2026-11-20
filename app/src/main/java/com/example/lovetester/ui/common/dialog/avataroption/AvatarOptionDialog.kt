package com.example.lovetester.ui.common.dialog.avataroption

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.lovetester.databinding.DialogAvatarOptionBinding

class AvatarOptionDialog : DialogFragment() {

    private var _binding: DialogAvatarOptionBinding? = null

    private val binding: DialogAvatarOptionBinding
        get() = requireNotNull(_binding)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            window?.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {

        _binding = DialogAvatarOptionBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: android.view.View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTakePhoto.setOnClickListener {
            sendResult(OPTION_CAMERA)
        }

        binding.btnUploadPhoto.setOnClickListener {
            sendResult(OPTION_GALLERY)
        }
    }

    private fun sendResult(option: String) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(RESULT_OPTION, option)
            }
        )

        dismiss()
    }

    override fun onStart() {
        super.onStart()

        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalMargin = (32 * resources.displayMetrics.density).toInt()

        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            setLayout(
                (screenWidth - horizontalMargin).coerceAtLeast(1),
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            setDimAmount(0.4f)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "avatar_option_request"
        const val RESULT_OPTION = "avatar_option_result"

        const val OPTION_CAMERA = "camera"
        const val OPTION_GALLERY = "gallery"
    }
}