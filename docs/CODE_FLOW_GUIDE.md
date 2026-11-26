# Hướng Dẫn Toàn Diện Luồng Code LoveTester (Code Flow Guide)

Tài liệu này được biên soạn dành cho lập trình viên mới học Android, bám sát 100% mã nguồn thực tế của dự án LoveTester (sử dụng Kotlin, ViewBinding, Hilt, Room, DataStore Preferences, Coroutines/Flow, Coil).

---

## Tiến độ biên soạn

- [x] **Phần 0**: Tổng quan kiến trúc, Hilt DI, cây màn hình, bản đồ thư mục và thứ tự đọc
- [x] **Phần 1**: Khởi động và Onboarding (Splash → Language → Intro → Permission → Home)
- [x] **Phần 2**: Home – Vỏ 3 tab (HomeActivity, FragmentManager, show/hide, xoay màn hình)
- [ ] **Phần 3**: Tab Home (Dữ liệu ngày yêu, Avatar, Tên, BackgroundBottomSheet, InformationBottomSheet)
- [ ] **Phần 4**: Start Date (HomeFragment → StartDateActivity → LoveDatePickerDialog)
- [ ] **Phần 5**: Love Test (NameTestActivity, HoroscopeTestActivity, LoveTestCalculator, ResultActivity)
- [ ] **Phần 6**: Memory (Room Database, Flow, MemoryViewModel, ConcatAdapter, AddMemoryActivity)
- [ ] **Phần 7**: Cài đặt và Đa ngôn ngữ (SettingsActivity, LanguageActivity, AppCompatDelegate)
- [ ] **Phần 8**: Hạ tầng dùng chung (UI Extensions, DataStore, Room, File Storage)
- [ ] **Phần 9**: Phụ lục (Bảng tra cứu "Muốn sửa X mở file nào" & Thuật ngữ cốt lõi)

---

# Phần 0: Tổng quan kiến trúc và Cách đọc dự án

## 1. Sơ đồ các tầng kiến trúc (Architecture Layers)

Dự án áp dụng mô hình kiến trúc phân lớp chuẩn của Android (Clean Architecture / Unidirectional Data Flow):

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. UI LAYER (Giao diện người dùng)                                      │
│    - Activity / Fragment / BottomSheetDialogFragment / DialogFragment  │
│    - ViewBinding (liên kết giao diện an toàn, không findViewById)       │
│    - Quan sát dữ liệu qua Flow/StateFlow bằng `collectOn`              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ sự kiện người dùng (user events)
                                    ▼ lắng nghe state (uiState / Flow)
┌────────────────────────────────────────────────────────────────────────┐
│ 2. VIEWMODEL LAYER (Trạng thái và logic giao diện)                    │
│    - Quản lý StateFlow / SharedFlow                                    │
│    - Sống sót qua xoay màn hình (Configuration Changes)                │
│    - Gọi coroutine trong `viewModelScope`                              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ gọi suspend function / observe Flow
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. REPOSITORY & STORAGE LAYER (Xử lý dữ liệu nghiệp vụ)                │
│    - LoveSettingsRepository: đọc/ghi DataStore                         │
│    - MemoryRepository: điều phối Room DAO + File Storage               │
│    - AvatarStorage / BackgroundImageStorage / MemoryImageStorage       │
│    - ImageDownscaler: nén, xoay EXIF, giảm kích thước ảnh tránh OOM    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ lưu trữ bền vững (Persistence)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. DATA SOURCES (Nguồn dữ liệu vật lý)                                 │
│    - DataStore Preferences ("settings"): cấu hình ngày yêu, tên, v.v.   │
│    - Room Database ("love_tester_memory.db"): SQLite lưu kỷ niệm       │
│    - Internal Files Dir (`context.filesDir`): thư mục ảnh avatars,     │
│      backgrounds, memories trên bộ nhớ trong thiết bị                  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. LoveTesterApplication và Dependency Injection (Hilt)

### 2.1. Lớp khởi tạo ứng dụng: `LoveTesterApplication`
- File: `LoveTesterApplication.kt`
- Lớp `LoveTesterApplication : Application()` được gắn annotation `@HiltAndroidApp`.
- **Vai trò**: Điểm bắt đầu vòng đời app. Hilt sẽ sinh code tầng ứng dụng (ApplicationComponent / SingletonComponent) để quản lý tất cả dependency có phạm vi toàn cục.

### 2.2. Hilt Module cung cấp dependency: `DatabaseModule`
- File: `di/DatabaseModule.kt`
- Được gắn `@Module` và `@InstallIn(SingletonComponent::class)`.
- **Cung cấp**:
  1. `provideMemoryDatabase(context: Context): MemoryDatabase`: Tạo Room database singleton tên `"love_tester_memory.db"`.
  2. `provideMemoryDao(database: MemoryDatabase): MemoryDao`: Trích xuất DAO từ database để inject vào Repository.

### 2.3. Bảng phân phối Dependency Injection trong toàn bộ dự án

| Lớp được inject | Thuộc tính / Constructor inject | Nguồn cung cấp |
| :--- | :--- | :--- |
| `LoveSettingsRepository` | `@Inject constructor(@ApplicationContext context: Context)` | `@Singleton` tự động gắn kết |
| `MemoryRepository` | `@Inject constructor(dao: MemoryDao, imageStorage: MemoryImageStorage)` | Hilt tạo qua `DatabaseModule` + Storage |
| `AvatarStorage` | `@Inject constructor(@ApplicationContext context: Context)` | Hilt cung cấp `Context` |
| `BackgroundImageStorage` | `@Inject constructor(@ApplicationContext context: Context)` | Hilt cung cấp `Context` |
| `MemoryImageStorage` | `@Inject constructor(@ApplicationContext context: Context)` | Hilt cung cấp `Context` |
| `MemoryViewModel` | `@HiltViewModel class MemoryViewModel @Inject constructor(...)` | Hilt ViewModel Factory |
| `SplashActivity` | `@Inject lateinit var repository: LoveSettingsRepository` | Field injection qua `@AndroidEntryPoint` |
| `LanguageActivity` | `@Inject lateinit var repository: LoveSettingsRepository` | Field injection qua `@AndroidEntryPoint` |
| `IntroActivity` | `@Inject lateinit var repository: LoveSettingsRepository` | Field injection qua `@AndroidEntryPoint` |
| `PermissionActivity` | `@Inject lateinit var repository: LoveSettingsRepository` | Field injection qua `@AndroidEntryPoint` |
| `HomeActivity` | `@AndroidEntryPoint` (vỏ chứa các Fragment Hilt) | Hilt EntryPoint cho container |
| `HomeFragment` | `@Inject lateinit var repository: LoveSettingsRepository` | Field injection qua `@AndroidEntryPoint` |
| `StartDateActivity` | `@Inject lateinit var repository: LoveSettingsRepository` | Field injection qua `@AndroidEntryPoint` |
| `BackgroundBottomSheet` | `@Inject lateinit var repository`, `@Inject lateinit var storage` | Field injection qua `@AndroidEntryPoint` |
| `InformationBottomSheet` | `@Inject lateinit var repository`, `@Inject lateinit var avatarStorage` | Field injection qua `@AndroidEntryPoint` |
| `MemoryListFragment` | `viewModel: MemoryViewModel by activityViewModels()` | Hilt ViewModel chia sẻ với Activity |
| `AddMemoryActivity` | `viewModel: MemoryViewModel by viewModels()` | Hilt ViewModel theo vòng đời Activity |

> **Lưu ý**: Các Activity/Fragment không cần Hilt (`SettingsActivity`, `NameTestActivity`, `HoroscopeTestActivity`, `ResultActivity`, `LoveTestFragment`, `HoroscopePickerBottomSheet`) không cần khai báo `@AndroidEntryPoint` để tránh sinh code thừa.

---

## 3. Cây màn hình toàn dự án (Activity / Fragment / Sheet / Dialog)

Cấu trúc điều hướng phân cấp của ứng dụng:

```
[SplashActivity] (Khởi động kiểm tra OnboardingStep)
 ├── Nếu chưa xong Onboarding:
 │     └── [LanguageActivity]
 │           └── [IntroActivity]
 │                 └── [PermissionActivity]
 │                       └── [HomeActivity] (FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK)
 └── Nếu đã hoàn thành Onboarding:
       └── [HomeActivity] (vào thẳng)

[HomeActivity] (Host 3 Tab bằng FragmentManager show/hide)
 │
 ├── TAB 1: [HomeFragment]
 │    ├── BottomSheet: [BackgroundBottomSheet] (chọn hình nền, ảnh cá nhân)
 │    ├── BottomSheet: [InformationBottomSheet] (chỉnh sửa hồ sơ bạn & người ấy)
 │    │    ├── Dialog: [AvatarOptionDialog] (chọn Camera hoặc Thư viện)
 │    │    └── Dialog: [LoveDatePickerDialog] (chọn ngày sinh)
 │    ├── Mở Activity: [StartDateActivity] (chỉnh sửa ngày bắt đầu yêu)
 │    │    └── Dialog: [LoveDatePickerDialog] (chọn ngày kỉ niệm)
 │    └── Mở Activity: [SettingsActivity]
 │         └── Mở Activity: [LanguageActivity] (mở từ Settings: `fromSettings = true`)
 │
 ├── TAB 2: [LoveTestFragment]
 │    ├── Mở Activity: [NameTestActivity] (bói tình yêu theo tên)
 │    │    └── Mở Activity: [ResultActivity] (hiển thị điểm %, lời nhắn, chia sẻ)
 │    └── Mở Activity: [HoroscopeTestActivity] (bói tình yêu theo cung hoàng đạo)
 │         ├── BottomSheet: [HoroscopePickerBottomSheet] (chọn cung hoàng đạo)
 │         └── Mở Activity: [ResultActivity] (hiển thị kết quả cung tương hợp)
 │
 └── TAB 3: [MemoryListFragment]
      ├── RecyclerView danh sách kỷ niệm (`ConcatAdapter` = items + footer)
      ├── Dialog xác nhận xoá kỷ niệm
      └── Mở Activity: [AddMemoryActivity] (thêm kỷ niệm mới)
           └── Dialog: [LoveDatePickerDialog] (chọn ngày của kỷ niệm)
```

---

## 4. Bản đồ thư mục dự án (`app/src/main/java/com/example/lovetester`)

- **`LoveTesterApplication.kt`**: Lớp ứng dụng toàn cục.
- **`di/`**: Khai báo module Hilt Dependency Injection.
  - `DatabaseModule.kt`: Cung cấp `MemoryDatabase` và `MemoryDao`.
- **`data/`**: Tầng dữ liệu.
  - `local/database/`: Room Database (`MemoryDatabase.kt`, `MemoryDao.kt`, `MemoryEntity.kt`).
  - `local/model/`: Mô hình dữ liệu thực thể (`OnboardingStep.kt`, `PersonInfo.kt`).
  - `local/repository/`: `LoveSettingsRepository.kt` (quản lý DataStore Preferences).
  - `data/repository/`: `MemoryRepository.kt` (quản lý truy vấn kỷ niệm và lưu file ảnh).
  - `local/storage/`: Các lớp quản lý file trên ổ cứng (`AvatarStorage.kt`, `BackgroundImageStorage.kt`, `MemoryImageStorage.kt`, `ImageDownscaler.kt`).
- **`ui/`**: Tầng giao diện người dùng, chia theo từng tính năng (Feature-based packages):
  - `splash/`: Màn hình khởi động kiểm tra trạng thái Onboarding.
  - `language/`: Chọn ngôn ngữ ứng dụng (`LanguageActivity.kt`, `LanguageViewModel.kt`, v.v.).
  - `intro/`: Các trang giới thiệu tính năng ứng dụng (`IntroActivity.kt`, `IntroPagerAdapter.kt`).
  - `permission/`: Màn hình xin quyền Thông báo & Bộ sưu tập (`PermissionActivity.kt`).
  - `home/`: Màn hình chính chứa tab bar (`HomeActivity.kt`) và tab đếm ngày yêu (`HomeFragment.kt`).
  - `background/`: Chọn hình nền trang chủ (`BackgroundBottomSheet.kt`, `BackgroundAdapter.kt`).
  - `information/`: Hồ sơ người dùng (`InformationBottomSheet.kt`, `PersonType.kt`, `GenderLabel.kt`).
  - `startdate/`: Chọn ngày bắt đầu yêu (`StartDateActivity.kt`).
  - `lovetest/`: Tính năng bói tình yêu:
    - `LoveTestFragment.kt`: Hub chọn loại bói.
    - `domain/LoveTestCalculator.kt`: Thuật toán tính phần trăm theo tên và cung.
    - `name/NameTestActivity.kt`: Nhập 2 tên để tính.
    - `horoscope/`: Bói cung hoàng đạo (`HoroscopeTestActivity.kt`, `HoroscopePickerBottomSheet.kt`, v.v.).
    - `result/ResultActivity.kt`: Màn hình kết quả chung.
  - `memory/`: Nhật ký kỷ niệm (`MemoryListFragment.kt`, `MemoryViewModel.kt`, `AddMemoryActivity.kt`, v.v.).
  - `setting/`: Cài đặt ứng dụng (`SettingsActivity.kt`).
  - `common/`: Các thành phần dùng chung:
    - Dialogs: `LoveDatePickerDialog.kt`, `AvatarOptionDialog.kt`.
    - Extensions: `ViewBindingDelegate.kt`, `FlowExtensions.kt`, `InsetsExtensions.kt`, `FragmentExtensions.kt`, `KeyboardExtensions.kt`, `GridSpacingDecoration.kt`.

---

## 5. Thứ tự nên đọc các phần và lý do

Khi học một dự án Android thực tế, việc đọc file theo thứ tự ngẫu nhiên rất dễ khiến bạn bị choáng ngợp bởi lượng code lớn. Dưới đây là lộ trình tối ưu được khuyến nghị:

1. **Đọc Phần 1 (Khởi động & Onboarding)**: Hiểu cách ứng dụng bắt đầu từ `SplashActivity`, đọc trạng thái từ DataStore, điều hướng tuần tự qua Language → Intro → Permission và cơ chế điều hướng 1 chiều (không cho back ngược lại màn trước sau khi hoàn thành).
2. **Đọc Phần 2 (Vỏ HomeActivity & Tab Navigation)**: Hiểu cách điều khiển nhiều Fragment cùng sống trong một Activity mà không bị tạo lại mỗi lần chuyển tab (`show` / `hide`), cách giữ trạng thái khi xoay màn hình và xử lý phím Back quay về tab đầu tiên.
3. **Đọc Phần 3 (Tab Home & BottomSheet)**: Nắm được cách HomeFragment lấy dữ liệu thời gian thực từ Flow của DataStore, tính toán ngày yêu, cũng như cách truyền dữ liệu hai chiều và tương tác với các BottomSheetDialogFragment để lưu ảnh và thông tin cá nhân.
4. **Đọc Phần 4 (Start Date & Custom DatePicker)**: Học cách viết và tái sử dụng một custom `DialogFragment` phức tạp (`LoveDatePickerDialog`) để chọn ngày tháng năm bằng RecyclerView.
5. **Đọc Phần 5 (Love Test & Thuật toán tính toán)**: Học cách tách biệt UI và Domain Logic (`LoveTestCalculator`), cách sử dụng `FragmentResultListener` để nhận dữ liệu từ BottomSheet trong `HoroscopeTestActivity`, và cách đóng gói dữ liệu qua `Intent` vào `ResultActivity`.
6. **Đọc Phần 6 (Memory - MVI / StateFlow / Room)**: Đây là phần trọng tâm về kiến trúc hiện đại trong dự án: Room Database, Coroutine Dispatchers, Flow reactive stream, ConcatAdapter, và xử lý ảnh trên Background thread (`Dispatchers.IO`).
7. **Đọc Phần 7 (Cài đặt & Ngôn ngữ)**: Hiểu cách Android xử lý thay đổi ngôn ngữ động ở runtime thông qua `AppCompatDelegate.setApplicationLocales` và cơ chế tái tạo Activity.
8. **Đọc Phần 8 (Hạ tầng dùng chung & Kỹ thuật nâng cao)**: Nắm vững các Kotlin Extension function độc đáo giúp code gọn đẹp (ViewBindingDelegate, collectOn an toàn vòng đời, Edge-to-edge WindowInsets).
9. **Đọc Phần 9 (Phụ lục tra cứu & Thuật ngữ)**: Sử dụng làm tài liệu tra cứu nhanh khi cần chỉnh sửa bất kỳ tính năng nào trong tương lai.

---

# Phần 1: Khởi động và Onboarding (Splash → Language → Intro → Permission → Home)

## a) Đọc file theo thứ tự
1. `data/local/model/OnboardingStep.kt`: Định nghĩa Enum gồm 4 bước onboarding (`LANGUAGE`, `INTRO`, `PERMISSION`, `COMPLETED`).
2. `data/local/repository/LoveSettingsRepository.kt`: Quản lý lưu trữ trạng thái onboarding vào DataStore Preferences và phát ra luồng `onboardingStep: Flow<OnboardingStep>`.
3. `ui/splash/SplashActivity.kt`: Màn hình khởi động, chạy animation hiệu ứng 4 dấu chấm, đọc bước onboarding và điều hướng đến màn hình tương ứng.
4. `ui/language/LanguageActivity.kt`: Màn hình chọn ngôn ngữ lần đầu, cập nhật `OnboardingStep.INTRO`, áp dụng ngôn ngữ toàn app và chuyển tiếp sang `IntroActivity`.
5. `ui/intro/IntroActivity.kt`: Màn hình 3 slide giới thiệu bằng `ViewPager2`, xử lý nút Next chuyển sang `OnboardingStep.PERMISSION` và xử lý nút Back quay về `OnboardingStep.LANGUAGE`.
6. `ui/permission/PermissionActivity.kt`: Màn hình xin quyền Camera, xử lý nút Continue hoàn tất onboarding (`OnboardingStep.COMPLETED`) và điều hướng dứt điểm vào `HomeActivity`.

---

## b) Sơ đồ chuỗi gọi (Call Chain)

### Luồng 1: Người dùng mở app lần đầu (Fresh Install)
```text
Khởi động ứng dụng
  → LoveTesterApplication.onCreate() (Hilt khởi tạo DI container)
  → SplashActivity.onCreate()
      → SplashActivity.loadOnboardingStep()
          → LoveSettingsRepository.onboardingStep.first()  [trả về OnboardingStep.LANGUAGE do chưa có trong DataStore]
      → SplashActivity.startSplashAnimation()               [chạy ValueAnimator 1200ms animate 4 chấm]
      → ValueAnimator.doOnEnd()
          → SplashActivity.navigateIfReady()
          → SplashActivity.startActivity(Intent(SplashActivity, LanguageActivity))
          → SplashActivity.finish()
  → LanguageActivity.onCreate()
      → Người dùng bấm chọn ngôn ngữ trong danh sách
          → LanguageAdapter.onClick() → LanguageViewModel.select(language)
          → LanguageViewModel.selectedId phát giá trị mới → LanguageAdapter cập nhật vị trí được chọn
      → Người dùng bấm icon xác nhận (btnConfirmLanguage)
          → LoveSettingsRepository.setOnboardingStep(OnboardingStep.INTRO)
          → SupportedLanguages.apply(selectedLanguage) → AppCompatDelegate.setApplicationLocales(...)
          → LanguageActivity.startActivity(Intent(LanguageActivity, IntroActivity))
  → IntroActivity.onCreate()
      → ViewPager2 hiển thị trang 1 (intro_relationship)
      → Người dùng bấm btnNext (lần 1) → ViewPager2 chuyển sang trang 2 (intro_day_counter)
      → Người dùng bấm btnNext (lần 2) → ViewPager2 chuyển sang trang 3 (intro_memories)
      → Người dùng bấm btnNext (lần 3 - trang cuối cùng)
          → IntroActivity.navigateToPermission()
          → LoveSettingsRepository.setOnboardingStep(OnboardingStep.PERMISSION)
          → IntroActivity.startActivity(Intent(IntroActivity, PermissionActivity))
  → PermissionActivity.onCreate()
      → PermissionActivity.syncCameraPermissionSwitch()
      → Người dùng bật công tắc (switchAllowAccess)
          → ActivityResultLauncher.launch(Manifest.permission.CAMERA)
          → Hệ thống Android hiển thị hộp thoại xin quyền Camera
      → Người dùng bấm "Continue" (btnContinue)
          → PermissionActivity.navigateToHome()
          → LoveSettingsRepository.setOnboardingStep(OnboardingStep.COMPLETED)
          → PermissionActivity.startActivity(Intent(PermissionActivity, HomeActivity) với cờ CLEAR_TASK | NEW_TASK)
  → HomeActivity.onCreate() [Vào màn hình chính, toàn bộ Onboarding hoàn tất]
```

### Luồng 2: Người dùng mở app các lần tiếp theo (Đã hoàn thành Onboarding)
```text
Khởi động ứng dụng
  → SplashActivity.onCreate()
      → SplashActivity.loadOnboardingStep()
          → LoveSettingsRepository.onboardingStep.first()  [đọc được OnboardingStep.COMPLETED từ DataStore]
      → SplashActivity.startSplashAnimation()               [chạy ValueAnimator 1200ms]
      → SplashActivity.navigateIfReady()
          → SplashActivity.startActivity(Intent(SplashActivity, HomeActivity))
          → SplashActivity.finish()
  → HomeActivity.onCreate() [Vào thẳng màn hình chính, bỏ qua toàn bộ Onboarding]
```

---

## c) Giải thích chi tiết từng bước

1. **Khởi động và kiểm tra tiến độ Onboarding**:
   - Khi app mở, `SplashActivity.onCreate()` được gọi trước tiên. Hàm `loadOnboardingStep()` khởi chạy coroutine trên `lifecycleScope` gọi `repository.onboardingStep.first()`.
   - Trong `LoveSettingsRepository.kt`, key `ONBOARDING_STEP` (`stringPreferencesKey("onboarding_step")`) được truy xuất từ DataStore Preferences (`"settings"`). Nếu chưa từng lưu (lần mở đầu tiên), giá trị mặc định trả về là `OnboardingStep.LANGUAGE`.
   - Đồng thời, hàm `startSplashAnimation()` chạy một `ValueAnimator` trong 1200ms làm nổi bật tuần tự 4 chấm (`dot1` → `dot4`).
   - Hàm `navigateIfReady()` đóng vai trò "chốt chặn": nó chỉ điều hướng khi **cả hai** điều kiện thỏa mãn: (1) đã đọc xong `onboardingStep` từ DataStore và (2) animation 1200ms đã kết thúc, đồng thời Activity đang ở trạng thái `RESUMED` (`activityResumed == true`).

2. **Bước chọn ngôn ngữ (`LanguageActivity`)**:
   - Người dùng thấy danh sách các ngôn ngữ được hỗ trợ lấy từ `SupportedLanguages.all`.
   - Khi chọn một mục, `LanguageViewModel.select(language)` lưu lại ngôn ngữ đó vào `selectedId: StateFlow<String>`.
   - Khi người dùng bấm nút tick ở góc phải (`btnConfirmLanguage`):
     - Hàm `LoveSettingsRepository.setOnboardingStep(OnboardingStep.INTRO)` ghi chữ `"INTRO"` vào DataStore.
     - Hàm `SupportedLanguages.apply(selectedLanguage)` thiết lập Locale cho toàn ứng dụng qua `AppCompatDelegate.setApplicationLocales()`.
     - Mở tiếp `IntroActivity` bằng `startActivity()`.

3. **Bước xem giới thiệu (`IntroActivity`)**:
   - Gồm 3 trang giới thiệu được cấu hình qua danh sách `IntroPage` và gán vào `ViewPager2` với `IntroPagerAdapter`.
   - Khi người dùng lướt hoặc bấm nút `btnNext`, `ViewPager2.OnPageChangeCallback` kích hoạt hàm `updateIndicators(binding, position)` để đổi kích thước và màu sắc của 3 dấu chấm (dấu chấm trang hiện tại dãn dài thành 20dp màu hồng `bg_intro_dot_active`).
   - Khi đang ở trang thứ 3 (trang cuối cùng, `currentPage == lastPage`), bấm `btnNext` sẽ kích hoạt hàm `navigateToPermission()`. Hàm này lưu `OnboardingStep.PERMISSION` vào DataStore và khởi động `PermissionActivity`.

4. **Bước cấp quyền (`PermissionActivity`)**:
   - `PermissionActivity.onCreate()` gọi `syncCameraPermissionSwitch()` kiểm tra xem quyền `Manifest.permission.CAMERA` đã được cấp chưa bằng `ContextCompat.checkSelfPermission`.
   - Nếu chưa cấp, người dùng gạt `switchAllowAccess` sẽ kích hoạt `cameraPermissionLauncher.launch(Manifest.permission.CAMERA)` để gọi hộp thoại quyền hệ thống.
   - Khi người dùng bấm `btnContinue`:
     - Hàm `navigateToHome()` chạy `repository.setOnboardingStep(OnboardingStep.COMPLETED)` để ghi nhận đã hoàn thành toàn bộ onboarding.
     - Khởi chạy `HomeActivity` kèm hai cờ quan trọng: `Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK`. Điều này xoá sạch toàn bộ backstack (Splash, Language, Intro, Permission) khỏi bộ nhớ task.

5. **Vì sao lần sau mở app vào thẳng Home?**:
   - Khi `PermissionActivity` ghi `OnboardingStep.COMPLETED` vào DataStore Preferences, giá trị này được lưu vĩnh viễn trên bộ nhớ máy (trong file `settings.preferences_pb`).
   - Ở lần khởi động sau, `SplashActivity.loadOnboardingStep()` đọc được giá trị `OnboardingStep.COMPLETED`.
   - Khi animation kết thúc, `navigateIfReady()` nhảy vào nhánh `OnboardingStep.COMPLETED -> HomeActivity::class.java`, lập tức mở `HomeActivity` và gọi `finish()` đóng Splash. Người dùng không bao giờ bị lặp lại các bước onboarding.

6. **Cơ chế nút Back ở từng màn hình**:
   - Tại `SplashActivity`: Không can thiệp nút Back. Sau 1.2s màn hình tự đóng.
   - Tại `LanguageActivity`: Kiểm tra `fromSettings`. Ở luồng Onboarding (`fromSettings = false`), nút `btnBack` bị ẩn (`binding.btnBack.isVisible = false`). Nếu bấm nút Back vật lý của điện thoại, ứng dụng sẽ thoát ra ngoài màn hình chính của máy.
   - Tại `IntroActivity`: Sử dụng `onBackPressedDispatcher.addCallback`. Hàm `handleIntroBack()` kiểm tra: nếu `currentPage > 0`, nó lùi lại trang trước (`viewPagerIntro.setCurrentItem(currentPage - 1, true)`). Nếu đang ở trang đầu tiên (`currentPage == 0`), nó gọi `navigateToLanguage()`: lùi trạng thái về `OnboardingStep.LANGUAGE` và mở lại `LanguageActivity` với cờ `CLEAR_TASK | NEW_TASK`.
   - Tại `PermissionActivity`: Sử dụng `onBackPressedDispatcher.addCallback`. Khi bấm Back, hàm `navigateBackToIntro()` được gọi: cập nhật lại `OnboardingStep.INTRO` vào DataStore và quay trở lại `IntroActivity` với cờ `CLEAR_TASK | NEW_TASK`.

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`SplashActivity`**:
  - `onCreate()`: Kích hoạt tải dữ liệu nền và khởi chạy animation.
  - `onPostResume()`: Đặt cờ `activityResumed = true` và thử gọi `navigateIfReady()`. Việc này ngăn chặn lỗi crash điều hướng nếu Activity chưa sẵn sàng hiển thị (ví dụ app vừa mở đã bị che bởi cuộc gọi đến).
  - `onPause()`: Đặt `activityResumed = false` để dừng mọi nỗ lực chuyển trang khi app đang nằm ở background.
  - `onDestroy()`: Gọi `animator?.cancel()` và xoá toàn bộ listener của `ValueAnimator` để chống rò rỉ bộ nhớ (memory leak).
- **`LanguageActivity`**:
  - Ngôn ngữ đang được highlight được lưu trong `LanguageViewModel.selectedId` (`StateFlow`).
  - Khi xoay màn hình (Configuration Change), `LanguageViewModel` không bị huỷ nên mục đang chọn vẫn được bảo toàn nguyên vẹn.
- **`IntroActivity`**:
  - `ViewPager2` tự động lưu và phục hồi vị trí trang (`currentItem`) qua `savedInstanceState` mặc định của Android.
  - `onDestroy()`: Huỷ đăng ký `pageChangeCallback` khỏi `ViewPager2` (`unregisterOnPageChangeCallback`).
- **`PermissionActivity`**:
  - `onResume()`: Luôn gọi lại `syncCameraPermissionSwitch()`. Nhờ đó, nếu người dùng chuyển sang ứng dụng Cài đặt của máy (Settings) để bật/tắt quyền Camera rồi quay lại app, giao diện switch và màu sắc sẽ tự động cập nhật ngay lập tức.

---

## e) Đoạn code then chốt

### Đoạn 1: Điều hướng an toàn ở `SplashActivity.navigateIfReady()`
```kotlin
// SplashActivity.kt
private fun navigateIfReady() {
    val step = onboardingStep ?: return
    if (!animationFinished || !activityResumed || completionHandled || isFinishing || isDestroyed) return
    completionHandled = true
    val destination = when (step) {
        OnboardingStep.LANGUAGE -> LanguageActivity::class.java
        OnboardingStep.INTRO -> IntroActivity::class.java
        OnboardingStep.PERMISSION -> PermissionActivity::class.java
        OnboardingStep.COMPLETED -> HomeActivity::class.java
    }
    startActivity(Intent(this, destination))
    finish()
}
```
*Giải thích*: Kết hợp cả hai điều kiện bất đồng bộ (animation chấm dứt và DataStore đọc xong) cùng trạng thái vòng đời (`activityResumed`, `!isFinishing`) để chuyển trang đúng đích mà không gây crash.

### Đoạn 2: Lùi trang mượt mà bằng phím Back ở `IntroActivity.handleIntroBack()`
```kotlin
// IntroActivity.kt
private fun handleIntroBack() {
    if (isNavigating) return
    val currentPage = binding.viewPagerIntro.currentItem
    if (currentPage > 0) {
        binding.viewPagerIntro.setCurrentItem(currentPage - 1, true)
        return
    }
    navigateToLanguage()
}
```
*Giải thích*: Tận dụng `OnBackPressedCallback` để biến phím Back thành nút chuyển lùi slide trong `ViewPager2`, chỉ khi ở trang đầu tiên mới quay về màn hình chọn ngôn ngữ trước đó.

### Đoạn 3: Hoàn thành Onboarding và xóa sạch backstack ở `PermissionActivity.navigateToHome()`
```kotlin
// PermissionActivity.kt
lifecycleScope.launch {
    repository.setOnboardingStep(OnboardingStep.COMPLETED)
    val intent = Intent(this@PermissionActivity, HomeActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
}
```
*Giải thích*: Đánh dấu bước `COMPLETED` vĩnh viễn vào DataStore và khởi tạo `HomeActivity` đồng thời hủy toàn bộ các màn hình chào hỏi trước đó để người dùng bấm Back ở màn Home không bị lọt lại vào Onboarding.

---

## f) Tự kiểm tra (Self-Check)

1. Nếu người dùng đang ở `IntroActivity` trang thứ 2 và nhấn nút Back của điện thoại, sự kiện gì sẽ xảy ra và hàm nào trong `IntroActivity.kt` điều khiển việc đó?
2. Giả sử người dùng đang ở `PermissionActivity` và bấm nút "Home" để ra ngoài Cài đặt của Android rồi tắt quyền Camera, khi mở lại app thì hàm nào đảm bảo công tắc Switch hiển thị đúng trạng thái tắt?
3. Tại sao ở `PermissionActivity.navigateToHome()` lại bắt buộc phải dùng cờ `Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK` khi chuyển sang `HomeActivity`?

---

# Phần 2: Home – Vỏ 3 Tab (HomeActivity, FragmentManager, show/hide, xoay màn hình)

## a) Đọc file theo thứ tự
1. `ui/home/HomeActivity.kt`: Lớp vỏ trung tâm quản lý 3 tab điều hướng (Home, Love Test, Memory) bằng FragmentManager theo cơ chế add/show/hide, xử lý nút Back quay về tab Home, khôi phục tab khi xoay màn hình và nhận `onNewIntent`.
2. `ui/home/HomeFragment.kt`: Fragment nội dung của Tab 1 (`Tab.HOME`, tag `"tab_home"`), hiển thị đồng hồ đếm ngày yêu và ảnh đôi bạn.
3. `ui/lovetest/LoveTestFragment.kt`: Fragment nội dung của Tab 2 (`Tab.LOVE_TEST`, tag `"tab_love_test"`), hiển thị các tùy chọn bói tình yêu.
4. `ui/memory/MemoryListFragment.kt`: Fragment nội dung của Tab 3 (`Tab.MEMORY`, tag `"tab_memory"`), hiển thị danh sách nhật ký kỷ niệm.
5. `ui/lovetest/result/ResultActivity.kt`: Màn hình kết quả kiểm tra, nơi minh họa cơ chế gửi `Intent` chứa `HomeActivity.EXTRA_TAB` kèm cờ `SINGLE_TOP` để quay trở lại tab Love Test của `HomeActivity`.

---

## b) Sơ đồ chuỗi gọi (Call Chain)

### Luồng 1: Người dùng chuyển tab từ Tab Home sang Tab Love Test
```text
Người dùng chạm vào mục "Love Test" trên Bottom Navigation
  → HomeActivity.binding.layoutBottomNav.navLoveTest.setOnClickListener()
      → HomeActivity.selectTab(Tab.LOVE_TEST)
          → supportFragmentManager.beginTransaction()
          → supportFragmentManager.findFragmentByTag("tab_love_test")  [null ở lần đầu bấm]
          → FragmentTransaction.add(R.id.homeTabContainer, LoveTestFragment(), "tab_love_test")
          → FragmentTransaction.hide(HomeFragment)                      [ẩn tab Home đang hiển thị]
          → FragmentTransaction.commit()
          → HomeActivity.currentTab = Tab.LOVE_TEST
          → HomeActivity.backToHomeCallback.isEnabled = true            [bật chặn phím Back]
          → HomeActivity.renderTabs()
              → Đổi màu icon/text của tab Love Test thành R.color.primary (hồng đậm)
              → Đổi màu icon/text tab Home thành R.color.tab_inactive
              → Đổi nền layoutBottomNav thành Color.WHITE
              → Bật biểu tượng thanh trạng thái tối màu: isAppearanceLightStatusBars = true
```

### Luồng 2: Người dùng nhấn nút Back khi đang ở Tab Love Test hoặc Tab Memory
```text
Người dùng nhấn nút Back của điện thoại (hệ thống)
  → HomeActivity.backToHomeCallback.handleOnBackPressed()  [vì backToHomeCallback.isEnabled == true]
      → HomeActivity.selectTab(Tab.HOME)
          → supportFragmentManager.beginTransaction()
          → supportFragmentManager.findFragmentByTag("tab_home")  [đã tồn tại trong FragmentManager]
          → FragmentTransaction.show(HomeFragment)                 [hiển thị lại Tab Home]
          → FragmentTransaction.hide(LoveTestFragment)            [ẩn Tab Love Test đi]
          → FragmentTransaction.commit()
          → HomeActivity.currentTab = Tab.HOME
          → HomeActivity.backToHomeCallback.isEnabled = false   [tắt chặn phím Back]
          → HomeActivity.renderTabs()
              → Đổi màu icon/text tab Home thành hồng đậm
              → Đổi nền layoutBottomNav thành Color.TRANSPARENT
              → Tắt biểu tượng thanh trạng thái tối màu: isAppearanceLightStatusBars = false
```

### Luồng 3: Màn hình ResultActivity điều hướng quay về Tab Love Test qua Intent
```text
Người dùng ở ResultActivity bấm nút Home hoặc nút Back
  → ResultActivity.navigateToTestMenu()
      → startActivity(Intent(ResultActivity, HomeActivity) với cờ CLEAR_TOP | SINGLE_TOP, extra: EXTRA_TAB = TAB_LOVE_TEST)
      → ResultActivity.finish()
  → Android kích hoạt lại HomeActivity hiện có trên đỉnh task (không tạo Activity mới)
  → HomeActivity.onNewIntent(intent)
      → HomeActivity.setIntent(intent)
      → HomeActivity.tabFromIntent(intent)  [đọc extra EXTRA_TAB và khớp ra Tab.LOVE_TEST]
      → HomeActivity.selectTab(Tab.LOVE_TEST)
```

---

## c) Giải thích chi tiết từng bước

1. **Khởi tạo và cấu hình vỏ chứa 3 tab**:
   - `HomeActivity` không sử dụng `ViewPager2` hay lệnh `replace()` thông thường vì cách đó sẽ huỷ giao diện (`onDestroyView`) của các Fragment mỗi lần chuyển tab, làm mất trạng thái cuộn và phải tải lại dữ liệu.
   - Thay vào đó, `HomeActivity` định nghĩa `private enum class Tab(val tag: String)` với 3 giá trị:
     - `HOME("tab_home")`
     - `LOVE_TEST("tab_love_test")`
     - `MEMORY("tab_memory")`
   - Mỗi giá trị có hàm `createFragment()` tạo đúng lớp Fragment tương ứng.

2. **Cơ chế chọn tab thông minh `selectTab(tab: Tab)`**:
   - Bước an toàn: Nếu `supportFragmentManager.isStateSaved == true` (Activity đã đi vào nền và lưu trạng thái), hàm sẽ `return` ngay lập tức để tránh lỗi crash nổi tiếng `IllegalStateException: Can not perform this action after onSaveInstanceState`.
   - Bắt đầu transaction với `setReorderingAllowed(true)` để tối ưu hiệu năng hiển thị fragment.
   - Duyệt qua tất cả các tab (`Tab.entries.forEach`):
     - Dùng `supportFragmentManager.findFragmentByTag(t.tag)` để kiểm tra Fragment đã từng được thêm vào container hay chưa.
     - Nếu tab đó là tab muốn chọn (`t == tab`) và **chưa từng được tạo** (`fragment == null`): gọi `tx.add(R.id.homeTabContainer, t.createFragment(), t.tag)`.
     - Nếu tab đó là tab muốn chọn (`t == tab`) và **đã có sẵn**: gọi `tx.show(fragment)`.
     - Nếu tab đó là các tab còn lại và đã tồn tại: gọi `tx.hide(fragment)` để ẩn đi mà **không huỷ View hay huỷ Fragment**.
   - Sau khi commit transaction, cập nhật biến `currentTab = tab` và gọi `renderTabs()` để làm mới giao diện.

3. **Cập nhật giao diện điều hướng và thanh trạng thái (`renderTabs`)**:
   - Tô màu hồng (`R.color.primary`) cho icon và text của tab đang kích hoạt, các tab còn lại dùng màu xám nhạt (`R.color.tab_inactive`).
   - Riêng Tab Home có hình nền tràn viền (Edge-to-edge), do đó nền của Bottom Navigation bar được đặt là trong suốt (`Color.TRANSPARENT`) và chữ trên status bar màu trắng.
   - Khi ở Tab Love Test hoặc Tab Memory, nền của Bottom Navigation bar chuyển sang màu trắng (`Color.WHITE`), đồng thời bật chữ status bar màu tối (`isAppearanceLightStatusBars = true`) để người dùng dễ đọc chữ trên nền sáng.

4. **Xử lý phím Back quay về Tab Home**:
   - Sử dụng `OnBackPressedCallback` với giá trị ban đầu là `false`:
     ```kotlin
     private val backToHomeCallback = object : OnBackPressedCallback(false) {
         override fun handleOnBackPressed() = selectTab(Tab.HOME)
     }
     ```
   - Trong `selectTab(tab: Tab)`, trạng thái bật/tắt của callback được quyết định bởi:
     `backToHomeCallback.isEnabled = tab != Tab.HOME`
   - Khi người dùng đang ở Tab Love Test hoặc Memory: bấm Back sẽ không đóng app mà kích hoạt callback chuyển về `Tab.HOME`.
   - Khi đã ở `Tab.HOME`: callback bị vô hiệu hóa (`isEnabled = false`), nếu người dùng tiếp tục bấm Back thì hệ thống sẽ đóng `HomeActivity` và thoát app ra ngoài màn hình chính.

5. **Giữ nguyên Tab khi xoay màn hình (Configuration Changes)**:
   - Khi thiết bị xoay ngang/dọc, Android huỷ và tạo lại `HomeActivity`.
   - Trước khi bị huỷ, hàm `onSaveInstanceState(outState: Bundle)` được gọi và lưu lại tên tab: `outState.putString(KEY_CURRENT_TAB, currentTab.name)`.
   - Khi `onCreate` chạy lại, nó kiểm tra:
     ```kotlin
     currentTab = savedInstanceState?.getString(KEY_CURRENT_TAB)?.let(Tab::valueOf)
         ?: tabFromIntent(intent)
         ?: Tab.HOME
     ```
   - Quan trọng: `FragmentManager` của Android tự động lưu và khôi phục các Fragment đã thêm theo `tag`. Vì vậy trong `selectTab()`, lệnh `findFragmentByTag` tìm thấy ngay các Fragment cũ, chỉ cần gọi `show`/`hide` mà không tạo mới lại, giữ nguyên 100% dữ liệu và trạng thái của người dùng.

6. **Xử lý `onNewIntent` nhận `EXTRA_TAB`**:
   - `HomeActivity` được cấu hình mở với chế độ điều hướng giữ nguyên instance đơn (`SINGLE_TOP`).
   - Khi `ResultActivity` gọi `navigateToTestMenu()`, nó đính kèm `putExtra(HomeActivity.EXTRA_TAB, HomeActivity.TAB_LOVE_TEST)`.
   - Android kích hoạt hàm `onNewIntent(intent: Intent)`. Tại đây, `HomeActivity` gọi `setIntent(intent)` để cập nhật intent mới nhất, sau đó hàm `tabFromIntent(intent)` bóc tách giá trị chuỗi (`TAB_LOVE_TEST` -> `Tab.LOVE_TEST`) và kích hoạt `selectTab(Tab.LOVE_TEST)` đưa người dùng quay lại đúng tab bói tình yêu.

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`HomeActivity.onCreate()`**:
  - `enableEdgeToEdge()`: Kích hoạt hiển thị tràn viền hệ thống.
  - `applyBottomNavInsets()`: Lắng nghe `WindowInsetsCompat.Type.navigationBars()`, tự động cộng thêm phần lề đáy (`navInsets.bottom`) vào `layoutBottomNav.root` để thanh điều hướng 3 tab không bị che khuất bởi thanh cử chỉ hoặc 3 phím điều hướng ảo của điện thoại.
  - Đăng ký `backToHomeCallback` vào `onBackPressedDispatcher`.
  - Khôi phục `currentTab` từ `savedInstanceState` (nếu xoay) hoặc `intent` (nếu mở từ ngoài), sau đó gọi `selectTab(currentTab)`.
- **`HomeActivity.onNewIntent()`**:
  - Chạy khi Activity đã tồn tại dưới backstack và nhận được một `Intent` mới.
  - Gọi `setIntent(intent)` đảm bảo intent của Activity luôn mang dữ liệu mới nhất.
- **`HomeActivity.onSaveInstanceState()`**:
  - Lưu trữ `currentTab.name` với key `KEY_CURRENT_TAB`.

---

## e) Đoạn code then chốt

### Đoạn 1: Cơ chế Add/Show/Hide 3 Tab tại `HomeActivity.selectTab()`
```kotlin
// HomeActivity.kt
val tx = fm.beginTransaction().setReorderingAllowed(true)
Tab.entries.forEach { t ->
    val fragment = fm.findFragmentByTag(t.tag)
    when {
        t == tab && fragment == null -> tx.add(R.id.homeTabContainer, t.createFragment(), t.tag)
        t == tab -> tx.show(fragment!!)
        fragment != null -> tx.hide(fragment)
    }
}
tx.commit()
```
*Giải thích*: Duyệt qua 3 tab, chỉ tạo và thêm mới Fragment nếu chưa tồn tại; nếu đã có thì dùng `show` cho tab được chọn và `hide` cho các tab khác, bảo toàn trạng thái UI và tránh huỷ View.

### Đoạn 2: Xử lý phím Back thông minh về tab Home
```kotlin
// HomeActivity.kt
private val backToHomeCallback = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() = selectTab(Tab.HOME)
}
// Được kích hoạt/vô hiệu hóa linh hoạt bên trong selectTab:
backToHomeCallback.isEnabled = tab != Tab.HOME
```
*Giải thích*: Khi ở tab con (Love Test hoặc Memory) thì bật chặn Back để quay về tab chính (Home); khi đã ở tab Home thì tắt chặn để người dùng thoát ứng dụng tự nhiên.

### Đoạn 3: Nhận Intent chuyển tab từ màn hình khác tại `HomeActivity.onNewIntent()`
```kotlin
// HomeActivity.kt
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    tabFromIntent(intent)?.let(::selectTab)
}
```
*Giải thích*: Cập nhật intent mới nhất khi Activity được gọi lại với `FLAG_ACTIVITY_SINGLE_TOP`, bóc tách `EXTRA_TAB` và tự động chuyển về đúng tab mà màn hình gọi mong muốn.

---

## f) Tự kiểm tra (Self-Check)

1. Tại sao `HomeActivity` lại dùng `tx.show()` và `tx.hide()` thay vì dùng `tx.replace(R.id.homeTabContainer, ...)` mỗi lần người dùng chuyển tab?
2. Khi người dùng đang ở Tab Memory và xoay ngang điện thoại, nhờ cơ chế nào mà Tab Memory vẫn tiếp tục hiển thị sau khi Activity bị tạo lại?
3. Nếu xóa dòng `setIntent(intent)` trong hàm `HomeActivity.onNewIntent(intent)` thì điều gì có thể xảy ra khi Activity đọc lại `intent` sau đó?


