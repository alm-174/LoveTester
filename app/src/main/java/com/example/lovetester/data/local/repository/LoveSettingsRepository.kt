package com.example.lovetester.data.local.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lovetester.data.local.model.Gender
import com.example.lovetester.data.local.model.OnboardingStep
import com.example.lovetester.data.local.model.PersonInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton


val Context.settingsStore by preferencesDataStore(
    name = "settings"
)


@Singleton
class LoveSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val store = context.applicationContext.settingsStore
    private val LOVE_START_DATE =
        stringPreferencesKey("love_start_date")

    private val ONBOARDING_STEP =
        stringPreferencesKey("onboarding_step")

    private val SELECTED_BACKGROUND_ID =
        stringPreferencesKey("selected_background_id")

    private val YOU_NAME = stringPreferencesKey("you_name")
    private val YOU_GENDER = stringPreferencesKey("you_gender")

    private val PARTNER_NAME = stringPreferencesKey("partner_name")
    private val PARTNER_GENDER = stringPreferencesKey("partner_gender")

    private val YOU_BIRTHDAY =
        stringPreferencesKey("you_birthday")

    private val PARTNER_BIRTHDAY =
        stringPreferencesKey("partner_birthday")

    private val YOU_AVATAR_PATH =
        stringPreferencesKey("you_avatar_path")

    private val PARTNER_AVATAR_PATH =
        stringPreferencesKey("partner_avatar_path")

    val loveStartDate: Flow<LocalDate?> =
        store.data.map { preferences ->
            preferences[LOVE_START_DATE]?.let { value ->
                LocalDate.parse(value)
            }
        }

    val onboardingStep: Flow<OnboardingStep> =
        store.data.map { preferences ->

            val raw = preferences[ONBOARDING_STEP]

            if (raw == null) {
                OnboardingStep.LANGUAGE
            } else {
                runCatching {
                    OnboardingStep.valueOf(raw)
                }.getOrDefault(OnboardingStep.LANGUAGE)
            }
        }

    val selectedBackgroundId: Flow<String?> =
        store.data.map { preferences ->
            preferences[SELECTED_BACKGROUND_ID]
        }

    val youInfo: Flow<PersonInfo> = store.data.map { preferences ->
        PersonInfo(
            name = preferences[YOU_NAME] ?: "You",
            gender = runCatching {
                Gender.valueOf(preferences[YOU_GENDER] ?: "MALE")
            }.getOrDefault(Gender.MALE),

            birthday = preferences[YOU_BIRTHDAY]?.let{
                    raw ->
                runCatching {
                    LocalDate.parse(raw)
                }.getOrNull()
            },
            avatarPath = preferences[YOU_AVATAR_PATH]
        )
    }

    val partnerInfo: Flow<PersonInfo> = store.data.map { preferences ->
        PersonInfo(
            name = preferences[PARTNER_NAME] ?: "Your partner",
            gender = runCatching {
                Gender.valueOf(preferences[PARTNER_GENDER] ?: "FEMALE")
            }.getOrDefault(Gender.FEMALE),
            birthday = preferences[PARTNER_BIRTHDAY]?.let{
                    raw ->
                runCatching {
                    LocalDate.parse(raw)
                }.getOrNull()
            },
            avatarPath = preferences[PARTNER_AVATAR_PATH]
        )
    }


    suspend fun setLoveStartDate(value: LocalDate) {
        store.edit { preferences ->
            preferences[LOVE_START_DATE] = value.toString()
        }
    }

    suspend fun setOnboardingStep(step: OnboardingStep){
        store.edit { preferences ->
            preferences[ONBOARDING_STEP] = step.name
        }
    }

    suspend fun setSelectedBackgroundId(id: String) {
        store.edit { preferences ->
            preferences[SELECTED_BACKGROUND_ID] = id
        }
    }

    suspend fun saveYouInfo(info: PersonInfo) {
        store.edit { preferences ->
            preferences[YOU_NAME] = info.name
            preferences[YOU_GENDER] = info.gender.name
            if (info.birthday != null) {
                preferences[YOU_BIRTHDAY] = info.birthday.toString()
            } else {
                preferences.remove(YOU_BIRTHDAY)
            }

            if (info.avatarPath != null) {
                preferences[YOU_AVATAR_PATH] = info.avatarPath
            } else {
                preferences.remove(YOU_AVATAR_PATH)
            }
        }
    }

    suspend fun savePartnerInfo(info: PersonInfo) {
        store.edit { preferences ->
            preferences[PARTNER_NAME] = info.name
            preferences[PARTNER_GENDER] = info.gender.name
            if (info.birthday != null) {
                preferences[PARTNER_BIRTHDAY] = info.birthday.toString()
            } else {
                preferences.remove(PARTNER_BIRTHDAY)
            }

            if (info.avatarPath != null) {
                preferences[PARTNER_AVATAR_PATH] = info.avatarPath
            } else {
                preferences.remove(PARTNER_AVATAR_PATH)
            }
        }
    }
}