package com.example.lovetester.ui.information

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.data.local.model.Gender
import com.example.lovetester.data.local.model.PersonInfo
import com.example.lovetester.data.local.model.validateName
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.data.local.storage.AvatarStorage
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import com.example.lovetester.databinding.BottomSheetInformationBinding
import com.example.lovetester.ui.common.dialog.LoveDatePickerDialog
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InformationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetInformationBinding? = null

    private val binding: BottomSheetInformationBinding
        get() = requireNotNull(_binding)

    private lateinit var personType: PersonType

    private var selectedGender: Gender = Gender.MALE

    private var selectedBirthday: LocalDate? = null

    private var currentAvatarPath: String? = null
    private var selectedAvatarUri: Uri? = null


    private val photoPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                selectedAvatarUri = uri
                binding.ivAvatar.setImageURI(uri)
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raw = requireArguments().getString(ARG_PERSON_TYPE)
            ?: error("Missing person type")

        personType = PersonType.valueOf(raw)
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

        setupInformation()
        setupGender()
        setupBirthday()
        setupSave()
        setupAvatarPicker()
    }

    private val birthdayFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun renderBirthday() {
        binding.tvBirthday.text =
            selectedBirthday?.format(birthdayFormatter)
                ?: "Select birthday"
    }

    private fun setupInformation() {
        val repository = LoveSettingsRepository(requireContext())

        val infoFlow = when (personType) {
            PersonType.YOU -> repository.youInfo
            PersonType.PARTNER -> repository.partnerInfo
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val info = infoFlow.first()

            currentAvatarPath = info.avatarPath

            currentAvatarPath?.let { path ->
                val file = File(path)

                if (file.isFile && selectedAvatarUri == null) {
                    binding.ivAvatar.setImageURI(Uri.fromFile(file))
                }
            }

            selectedGender = info.gender

            binding.etName.setText(info.name)

            binding.tvGender.text = when (info.gender) {
                Gender.MALE -> "Male"
                Gender.FEMALE -> "Female"
                Gender.OTHER -> "Other"
            }

            selectedBirthday = info.birthday
            renderBirthday()
        }


    }



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

                binding.tvGender.text = when (selectedGender) {
                    Gender.MALE -> "Male"
                    Gender.FEMALE -> "Female"
                    Gender.OTHER -> "Other"
                }

                true
            }

            popup.show()
        }
    }


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
                val repository = LoveSettingsRepository(
                    requireContext().applicationContext
                )

                val avatarStorage = AvatarStorage(
                    requireContext().applicationContext
                )

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
                        PersonType.YOU -> repository.saveYouInfo(info)
                        PersonType.PARTNER -> repository.savePartnerInfo(info)
                    }

                    savedToDataStore = true

                    // Dọn ảnh cũ sau khi DataStore đã lưu thành công.
                    val oldPath = currentAvatarPath
                    if (newAvatarPath != null &&
                        oldPath != null &&
                        oldPath != newAvatarPath
                    ) {
                        try {
                            avatarStorage.deleteAvatar(oldPath)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Lưu đã thành công; lỗi dọn ảnh cũ
                            // không được làm mất thông tin vừa lưu.
                        }
                    }

                    dismiss()

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    binding.etName.error = "Không thể lưu thông tin"
                    binding.btnSave.isEnabled = true
                } finally {
                    val pathToDelete = newAvatarPath

                    if (!savedToDataStore && pathToDelete != null) {
                        try {
                            avatarStorage.deleteAvatar(pathToDelete)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Có thể dọn file tồn dư ở lần sau.
                        }
                    }
                }
            }
        }
    }


    private fun setupBirthday() {
        binding.tvBirthday.setOnClickListener {
            LoveDatePickerDialog
                .newInstance(selectedBirthday ?: LocalDate.now())
                .show(childFragmentManager, "birthday_picker")
        }
    }

    private fun setupAvatarPicker() {
        val openPicker = View.OnClickListener {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        binding.ivAvatar.setOnClickListener(openPicker)
        binding.tvChangePhoto.setOnClickListener(openPicker)
    }



    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_PERSON_TYPE = "person_type"

        fun newInstance(personType: PersonType): InformationBottomSheet {
            return InformationBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PERSON_TYPE, personType.name)
                }
            }
        }
    }
}