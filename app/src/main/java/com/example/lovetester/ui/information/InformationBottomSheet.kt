
package com.example.lovetester.ui.information

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.data.local.model.Gender
import com.example.lovetester.data.local.model.PersonInfo
import com.example.lovetester.data.local.model.validateName
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.data.local.storage.AvatarStorage
import com.example.lovetester.databinding.BottomSheetInformationBinding
import com.example.lovetester.ui.common.dialog.avataroption.AvatarOptionDialog
import com.example.lovetester.ui.common.dialog.datepicker.LoveDatePickerDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class InformationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetInformationBinding? = null

    private val binding: BottomSheetInformationBinding
        get() = requireNotNull(_binding)

    private lateinit var personType: PersonType

    private var selectedGender: Gender = Gender.MALE
    private var selectedBirthday: LocalDate? = null

    private var currentAvatarPath: String? = null
    private var selectedAvatarUri: Uri? = null

    // Ảnh Camera đang chờ kết quả
    private var pendingCameraUri: Uri? = null
    private var pendingCameraPath: String? = null

    // Ảnh Camera tạm đã chụp thành công
    private var selectedCameraPath: String? = null

    private val birthdayFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // =========================
    // PHOTO PICKER
    // =========================

    private val photoPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                cleanupSelectedCameraFile()

                selectedAvatarUri = uri
                _binding?.ivAvatar?.setImageURI(uri)
            }
        }

    // =========================
    // CAMERA
    // =========================

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            val uri = pendingCameraUri
            val path = pendingCameraPath

            pendingCameraUri = null
            pendingCameraPath = null

            if (success && uri != null && path != null) {

                cleanupSelectedCameraFile()

                selectedCameraPath = path
                selectedAvatarUri = uri

                _binding?.ivAvatar?.setImageURI(uri)

            } else {

                deleteTemporaryCameraFile(path)
            }
        }

    // =========================
    // LIFECYCLE
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raw = requireArguments().getString(ARG_PERSON_TYPE)
            ?: error("Missing person type")

        personType = PersonType.valueOf(raw)

        pendingCameraUri = savedInstanceState
            ?.getString(KEY_PENDING_CAMERA_URI)
            ?.let(Uri::parse)

        pendingCameraPath = savedInstanceState
            ?.getString(KEY_PENDING_CAMERA_PATH)

        selectedAvatarUri = savedInstanceState
            ?.getString(KEY_SELECTED_AVATAR_URI)
            ?.let(Uri::parse)

        selectedCameraPath = savedInstanceState
            ?.getString(KEY_SELECTED_CAMERA_PATH)

        selectedBirthday = savedInstanceState
            ?.getString(KEY_SELECTED_BIRTHDAY)
            ?.let(LocalDate::parse)

        savedInstanceState
            ?.getString(KEY_SELECTED_GENDER)
            ?.let {
                selectedGender = Gender.valueOf(it)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetInformationBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePickerResult()
        setupAvatarOptionResult()

        setupInformation()
        setupGender()
        setupBirthday()
        setupAvatarPicker()
        setupSave()

        selectedAvatarUri?.let { uri ->
            binding.ivAvatar.setImageURI(uri)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(
            KEY_PENDING_CAMERA_URI,
            pendingCameraUri?.toString()
        )

        outState.putString(
            KEY_PENDING_CAMERA_PATH,
            pendingCameraPath
        )

        outState.putString(
            KEY_SELECTED_AVATAR_URI,
            selectedAvatarUri?.toString()
        )

        outState.putString(
            KEY_SELECTED_CAMERA_PATH,
            selectedCameraPath
        )

        outState.putString(
            KEY_SELECTED_BIRTHDAY,
            selectedBirthday?.toString()
        )

        outState.putString(
            KEY_SELECTED_GENDER,
            selectedGender.name
        )
    }

    // =========================
    // LOAD INFORMATION
    // =========================

    private fun setupInformation() {

        val repository = LoveSettingsRepository(
            requireContext().applicationContext
        )

        val infoFlow = when (personType) {
            PersonType.YOU -> repository.youInfo
            PersonType.PARTNER -> repository.partnerInfo
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val info = infoFlow.first()

            currentAvatarPath = info.avatarPath

            if (selectedAvatarUri == null) {
                currentAvatarPath?.let { path ->

                    val file = File(path)

                    if (file.isFile) {
                        binding.ivAvatar.setImageURI(
                            Uri.fromFile(file)
                        )
                    }
                }
            }

            if (binding.etName.text.isNullOrEmpty()) {
                binding.etName.setText(info.name)
            }

            if (selectedBirthday == null) {
                selectedBirthday = info.birthday
            }

            selectedGender = info.gender

            renderGender()
            renderBirthday()
        }
    }

    // =========================
    // GENDER
    // =========================

    private fun setupGender() {

        binding.tvGender.setOnClickListener { view ->

            val popup = PopupMenu(requireContext(), view)

            popup.menu.add("Male")
            popup.menu.add("Female")
            popup.menu.add("Other")

            popup.setOnMenuItemClickListener { item ->

                selectedGender = when (item.title.toString()) {
                    "Male" -> Gender.MALE
                    "Female" -> Gender.FEMALE
                    "Other" -> Gender.OTHER
                    else -> selectedGender
                }

                renderGender()
                true
            }

            popup.show()
        }
    }

    private fun renderGender() {

        binding.tvGender.text = when (selectedGender) {
            Gender.MALE -> "Male"
            Gender.FEMALE -> "Female"
            Gender.OTHER -> "Other"
        }
    }

    // =========================
    // BIRTHDAY
    // =========================

    private fun setupDatePickerResult() {

        childFragmentManager.setFragmentResultListener(
            LoveDatePickerDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->

            val raw = bundle.getString(
                LoveDatePickerDialog.RESULT_DATE
            ) ?: return@setFragmentResultListener

            selectedBirthday = LocalDate.parse(raw)
            renderBirthday()
        }
    }

    private fun setupBirthday() {

        binding.tvBirthday.setOnClickListener {

            LoveDatePickerDialog
                .newInstance(
                    selectedBirthday ?: LocalDate.now()
                )
                .show(
                    childFragmentManager,
                    "birthday_picker"
                )
        }
    }

    private fun renderBirthday() {

        binding.tvBirthday.text =
            selectedBirthday?.format(birthdayFormatter)
                ?: "Select birthday"
    }

    // =========================
    // AVATAR OPTION DIALOG
    // =========================

    private fun setupAvatarPicker() {

        val openDialog = View.OnClickListener {

            if (
                childFragmentManager.findFragmentByTag(
                    "avatar_option_dialog"
                ) == null
            ) {
                AvatarOptionDialog().show(
                    childFragmentManager,
                    "avatar_option_dialog"
                )
            }
        }

        binding.ivAvatar.setOnClickListener(openDialog)
        binding.tvChangePhoto.setOnClickListener(openDialog)
    }

    private fun setupAvatarOptionResult() {

        childFragmentManager.setFragmentResultListener(
            AvatarOptionDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->

            when (
                bundle.getString(
                    AvatarOptionDialog.RESULT_OPTION
                )
            ) {

                AvatarOptionDialog.OPTION_CAMERA -> {
                    openCamera()
                }

                AvatarOptionDialog.OPTION_GALLERY -> {
                    openPhotoPicker()
                }
            }
        }
    }

    private fun openPhotoPicker() {

        photoPickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    // =========================
    // OPEN CAMERA
    // =========================

    private fun openCamera() {

        var photoFile: File? = null

        try {

            val context = requireContext()

            val directory = File(
                context.cacheDir,
                CAMERA_DIRECTORY
            )

            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException(
                    "Cannot create camera directory"
                )
            }

            photoFile = File(
                directory,
                "avatar_${UUID.randomUUID()}.jpg"
            )

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )

            pendingCameraUri = uri
            pendingCameraPath = photoFile.absolutePath

            cameraLauncher.launch(uri)

        } catch (e: Exception) {

            pendingCameraUri = null
            pendingCameraPath = null

            deleteTemporaryCameraFile(
                photoFile?.absolutePath
            )

            Toast.makeText(
                requireContext(),
                "Không thể mở Camera",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // SAVE INFORMATION
    // =========================

    private fun setupSave() {

        binding.btnSave.setOnClickListener {

            val rawName = binding.etName.text.toString()

            val name = try {
                validateName(rawName)
            } catch (e: IllegalArgumentException) {

                binding.etName.error = e.message
                return@setOnClickListener
            }

            binding.btnSave.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {

                val context = requireContext().applicationContext

                val repository = LoveSettingsRepository(context)
                val avatarStorage = AvatarStorage(context)

                var newAvatarPath: String? = null
                var savedToDataStore = false

                try {

                    val avatarPath = selectedAvatarUri?.let { uri ->

                        avatarStorage.saveAvatar(uri).also {
                            newAvatarPath = it
                        }

                    } ?: currentAvatarPath

                    val info = PersonInfo(
                        name = name,
                        gender = selectedGender,
                        birthday = selectedBirthday,
                        avatarPath = avatarPath
                    )

                    when (personType) {

                        PersonType.YOU -> {
                            repository.saveYouInfo(info)
                        }

                        PersonType.PARTNER -> {
                            repository.savePartnerInfo(info)
                        }
                    }

                    savedToDataStore = true

                    // Chỉ xóa Avatar cũ sau khi lưu thành công.
                    val oldPath = currentAvatarPath

                    if (
                        newAvatarPath != null &&
                        oldPath != null &&
                        oldPath != newAvatarPath
                    ) {

                        try {
                            avatarStorage.deleteAvatar(oldPath)

                        } catch (e: CancellationException) {
                            throw e

                        } catch (_: Exception) {
                            // Không làm mất kết quả lưu thành công.
                        }
                    }

                    // Ảnh Camera đã được sao chép vào AvatarStorage.
                    cleanupSelectedCameraFile()

                    dismiss()

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {

                    binding.etName.error =
                        "Không thể lưu thông tin"

                    binding.btnSave.isEnabled = true

                } finally {

                    if (!savedToDataStore) {

                        newAvatarPath?.let { path ->

                            try {
                                avatarStorage.deleteAvatar(path)

                            } catch (e: CancellationException) {
                                throw e

                            } catch (_: Exception) {
                                // Có thể dọn file tồn dư sau.
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================
    // CLEANUP CAMERA CACHE
    // =========================

    private fun cleanupSelectedCameraFile() {

        deleteTemporaryCameraFile(selectedCameraPath)
        selectedCameraPath = null
    }

    private fun deleteTemporaryCameraFile(path: String?) {

        if (path == null) return

        val context = context ?: return

        val directory = File(
            context.cacheDir,
            CAMERA_DIRECTORY
        ).canonicalFile

        val file = File(path).canonicalFile

        // Chỉ cho phép xóa file trong thư mục Camera tạm.
        if (file.parentFile == directory) {
            file.delete()
        }
    }

    override fun onDestroyView() {

        _binding = null
        super.onDestroyView()
    }

    companion object {

        private const val ARG_PERSON_TYPE = "person_type"

        private const val CAMERA_DIRECTORY = "avatar_camera"

        private const val KEY_PENDING_CAMERA_URI =
            "pending_camera_uri"

        private const val KEY_PENDING_CAMERA_PATH =
            "pending_camera_path"

        private const val KEY_SELECTED_AVATAR_URI =
            "selected_avatar_uri"

        private const val KEY_SELECTED_CAMERA_PATH =
            "selected_camera_path"

        private const val KEY_SELECTED_BIRTHDAY =
            "selected_birthday"

        private const val KEY_SELECTED_GENDER =
            "selected_gender"

        fun newInstance(
            personType: PersonType
        ): InformationBottomSheet {

            return InformationBottomSheet().apply {

                arguments = Bundle().apply {
                    putString(
                        ARG_PERSON_TYPE,
                        personType.name
                    )
                }
            }
        }
    }
}
