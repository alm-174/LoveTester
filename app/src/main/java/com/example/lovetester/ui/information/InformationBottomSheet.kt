package com.example.lovetester.ui.information

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.example.lovetester.R
import com.example.lovetester.ui.common.hideKeyboard
import com.example.lovetester.ui.common.onImeDone
import com.example.lovetester.ui.common.showOnce
import com.example.lovetester.ui.common.toast
import com.example.lovetester.ui.common.viewBinding
import com.example.lovetester.data.local.model.Gender
import com.example.lovetester.data.local.model.PersonInfo
import com.example.lovetester.data.local.model.MAX_NAME_LENGTH
import com.example.lovetester.data.local.model.validateName
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.data.local.storage.AvatarStorage
import com.example.lovetester.databinding.BottomSheetInformationBinding
import com.example.lovetester.ui.common.dialog.avataroption.AvatarOptionDialog
import com.example.lovetester.ui.common.dialog.datepicker.LoveDatePickerDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class InformationBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var repository: LoveSettingsRepository

    @Inject
    lateinit var avatarStorage: AvatarStorage

    private val binding by viewBinding(BottomSheetInformationBinding::bind)

    private lateinit var personType: PersonType

    private var selectedGender: Gender? = null
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



    private val photoPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                cleanupSelectedCameraFile()

                selectedAvatarUri = uri
                if (view != null) binding.ivAvatar.load(uri)
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

                if (view != null) binding.ivAvatar.load(uri)

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
                selectedGender = savedInstanceState
                    ?.getString(KEY_SELECTED_GENDER)
                    ?.let(Gender::valueOf)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_information, container, false)

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
            binding.ivAvatar.load(uri)
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

        outState.putString(KEY_SELECTED_GENDER, selectedGender?.name)
    }

    // =========================
    // LOAD INFORMATION
    // =========================

    private fun setupInformation() {

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
                        binding.ivAvatar.load(file)
                    }
                }
            }

            if (binding.etName.text.isNullOrEmpty()) {
                binding.etName.setText(info.name)
            }

            if (selectedBirthday == null) {
                selectedBirthday = info.birthday
            }

            if (selectedGender == null) {
                selectedGender = info.gender
            }

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

            Gender.entries.forEachIndexed { index, gender ->
                popup.menu.add(Menu.NONE, index, index, gender.labelRes())
            }

            popup.setOnMenuItemClickListener { item ->
                selectedGender = Gender.entries[item.itemId]
                renderGender()
                true
            }

            popup.show()
        }
    }

    private fun renderGender() {
        binding.tvGender.text = selectedGender?.let { getString(it.labelRes()) }.orEmpty()
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

            childFragmentManager.showOnce(
                LoveDatePickerDialog.newInstance(selectedBirthday ?: LocalDate.now()),
                "birthday_picker"
            )
        }
    }

    private fun renderBirthday() {

        binding.tvBirthday.text =
            selectedBirthday?.format(birthdayFormatter)
                ?: getString(R.string.information_select_birthday)
    }

    // =========================
    // AVATAR OPTION DIALOG
    // =========================

    private fun setupAvatarPicker() {

        val openDialog = View.OnClickListener {

            childFragmentManager.showOnce(AvatarOptionDialog(), "avatar_option_dialog")
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

            requireContext().toast(R.string.information_camera_error)
        }
    }

    // =========================
    // SAVE INFORMATION
    // =========================

    private fun setupSave() {

        binding.etName.onImeDone()

        binding.btnSave.setOnClickListener {

            binding.etName.hideKeyboard()

            val rawName = binding.etName.text.toString()
            val trimmedName = rawName.trim()

            // UI checks the name first and shows a localized message.
            if (trimmedName.isBlank()) {
                binding.etName.error = getString(R.string.information_error_name_empty)
                return@setOnClickListener
            }
            if (trimmedName.length > MAX_NAME_LENGTH) {
                binding.etName.error = getString(R.string.information_error_name_too_long)
                return@setOnClickListener
            }

            val name = try {
                validateName(rawName)
            } catch (e: IllegalArgumentException) {

                // Unreachable after the checks above; validateName is only a developer guard.
                Log.w("InformationBottomSheet", "validateName rejected a checked name", e)
                binding.etName.error = getString(R.string.information_error_save)
                return@setOnClickListener
            }

            binding.btnSave.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {

                var newAvatarPath: String? = null
                var savedToDataStore = false
                var errorRes = R.string.information_error_save

                try {

                    val avatarPath = selectedAvatarUri?.let { uri ->

                        run {
                            errorRes = R.string.information_error_read_photo
                            avatarStorage.saveAvatar(uri)
                        }.also {
                            newAvatarPath = it
                            errorRes = R.string.information_error_save
                        }

                    } ?: currentAvatarPath

                    val info = PersonInfo(
                        name = name,
                        gender = selectedGender ?: Gender.MALE,
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

                    Log.e("AvatarSave", "Lỗi khi lưu avatar", e)

                    binding.etName.error =
                        getString(errorRes)

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
