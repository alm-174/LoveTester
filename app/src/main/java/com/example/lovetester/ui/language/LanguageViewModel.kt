package com.example.lovetester.ui.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the language that is only HIGHLIGHTED (not applied yet).
 * Backed by SavedStateHandle so it survives rotation and process death.
 */
class LanguageViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val selectedId: StateFlow<String> = savedStateHandle.getStateFlow(
        KEY_SELECTED_ID,
        SupportedLanguages.current().id
    )

    val selected: Language
        get() = SupportedLanguages.fromTag(selectedId.value) ?: SupportedLanguages.default

    fun select(language: Language) {
        savedStateHandle[KEY_SELECTED_ID] = language.id
    }

    private companion object {
        const val KEY_SELECTED_ID = "selected_language_id"
    }
}
