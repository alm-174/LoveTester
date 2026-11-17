package com.example.lovetester.ui.information

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InformationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetInformationBinding? = null

    private val binding: BottomSheetInformationBinding
        get() = requireNotNull(_binding)

    private lateinit var personType: PersonType

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

        setupInformation()
    }

    private fun setupInformation() {
        when (personType) {
            PersonType.YOU -> {
                binding.etName.setText("You")
            }

            PersonType.PARTNER -> {
                binding.etName.setText("Your partner")
            }
        }
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