# Hướng Dẫn Toàn Diện Luồng Code LoveTester (Code Flow Guide)

Tài liệu này được biên soạn dành cho lập trình viên mới học Android, bám sát 100% mã nguồn thực tế của dự án LoveTester (sử dụng Kotlin, ViewBinding, Hilt, Room, DataStore Preferences, Coroutines/Flow, Coil).

---

## Tiến độ biên soạn

- [x] **Phần 0**: Tổng quan kiến trúc, Hilt DI, cây màn hình, bản đồ thư mục và thứ tự đọc
- [x] **Phần 1**: Khởi động và Onboarding (Splash → Language → Intro → Permission → Home)
- [x] **Phần 2**: Home – Vỏ 3 tab (HomeActivity, FragmentManager, show/hide, xoay màn hình)
- [x] **Phần 3**: Tab Home (Dữ liệu ngày yêu, Avatar, Tên, BackgroundBottomSheet, InformationBottomSheet)
- [x] **Phần 4**: Start Date (HomeFragment → StartDateActivity → LoveDatePickerDialog)
- [x] **Phần 5**: Love Test (NameTestActivity, HoroscopeTestActivity, LoveTestCalculator, ResultActivity)
- [x] **Phần 6**: Memory (Room Database, Flow, MemoryViewModel, ConcatAdapter, AddMemoryActivity)
- [x] **Phần 7**: Cài đặt và Đa ngôn ngữ (SettingsActivity, LanguageActivity, AppCompatDelegate)
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

---

# Phần 3: Tab Home (Dữ liệu ngày yêu, Avatar, Tên, BackgroundBottomSheet, InformationBottomSheet)

## a) Đọc file theo thứ tự
1. `ui/home/HomeFragment.kt`: Fragment hiển thị màn hình chính (đếm ngày yêu, tên, tuổi, giới tính, avatar đôi bạn, ảnh nền); quan sát DataStore qua Flow để tự cập nhật giao diện.
2. `data/local/repository/LoveSettingsRepository.kt`: Nguồn dữ liệu duy nhất (Single Source of Truth) cung cấp Flow và hàm cập nhật cho ngày yêu, thông tin người dùng, ảnh đại diện và hình nền.
3. `ui/background/BackgroundBottomSheet.kt`: BottomSheet chọn hình nền (ảnh có sẵn hoặc chọn ảnh từ máy qua PhotoPicker), thu nhỏ và lưu đường dẫn vào DataStore.
4. `ui/background/BackgroundAdapter.kt`: Adapter hiển thị danh sách hình nền dạng lưới 2 cột.
5. `data/local/storage/BackgroundImageStorage.kt`: Lưu ảnh nền vào thư mục `context.filesDir/backgrounds`, dùng `ImageDownscaler` nén tối đa 1440px.
6. `ui/information/InformationBottomSheet.kt`: BottomSheet chỉnh sửa hồ sơ (tên, giới tính, ngày sinh, avatar), điều phối Camera/Gallery, xác thực dữ liệu và dọn rác ảnh cũ.
7. `ui/common/dialog/avataroption/AvatarOptionDialog.kt`: Dialog cho phép chọn chụp ảnh mới (Camera) hoặc chọn ảnh từ thư viện (Gallery) qua Fragment Result API.
8. `data/local/storage/AvatarStorage.kt`: Lưu avatar vào thư mục `context.filesDir/avatars` và xóa avatar cũ an toàn.
9. `data/local/storage/ImageDownscaler.kt`: Đối tượng tiện ích tối ưu hóa kích thước ảnh xuống tối đa 1440px trên `Dispatchers.IO` trước khi ghi xuống đĩa để chống OOM.

---

## b) Sơ đồ chuỗi gọi (Call Chain)

### Luồng 1: Người dùng đổi hình nền từ bộ sưu tập điện thoại
```text
Người dùng chạm vào nút Gallery (btnGallery) trên HomeFragment
  → HomeFragment.binding.btnGallery.setOnClickListener()
      → childFragmentManager.showOnce(BackgroundBottomSheet(), "home_sheet")
  → BackgroundBottomSheet.onViewCreated()
      → BackgroundBottomSheet.setupBackgroundList()
          → storage.getSavedImages()                        [quét thư mục context.filesDir/backgrounds]
          → BackgroundAdapter hiển thị danh sách ảnh
  → Người dùng chạm vào ô "Add Photo" (BackgroundItem.AddPhoto)
      → photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
  → Người dùng chọn 1 bức ảnh từ hệ thống Android
      → BackgroundBottomSheet.handleSelectedPhoto(uri)
          → BackgroundImageStorage.saveImage(uri)
              → ImageDownscaler.saveDownscaled()            [thu nhỏ <= 1440px, lưu vào filesDir/backgrounds/{UUID}.img]
          → LoveSettingsRepository.setSelectedBackgroundId("custom:$path")
          → BackgroundBottomSheet.dismiss()
  → Dữ liệu DataStore thay đổi
      → LoveSettingsRepository.selectedBackgroundId (Flow) phát giá trị mới
      → HomeFragment.observeSettings() nhận giá trị "custom:/path/to/img"
      → HomeFragment.binding.ivHomeBackground.load(File(path))  [Coil tải và hiển thị ảnh nền mới]
```

### Luồng 2: Người dùng chỉnh sửa hồ sơ và chụp ảnh đại diện mới
```text
Người dùng chạm vào khung avatar/tên của mình (layoutPersonOne)
  → HomeFragment.openInformation(PersonType.YOU)
      → childFragmentManager.showOnce(InformationBottomSheet.newInstance(PersonType.YOU), "home_sheet")
  → InformationBottomSheet.onViewCreated()
      → InformationBottomSheet.setupInformation()
          → LoveSettingsRepository.youInfo.first()           [nạp thông tin hiện tại vào View]
  → Người dùng chạm vào avatar (binding.ivAvatar)
      → childFragmentManager.showOnce(AvatarOptionDialog(), "avatar_option_dialog")
  → Người dùng chọn "Take Photo"
      → AvatarOptionDialog.sendResult(OPTION_CAMERA)
  → InformationBottomSheet.setupAvatarOptionResult() nhận kết quả
      → InformationBottomSheet.openCamera()
          → Tạo file tạm tại context.cacheDir/avatar_camera/avatar_{UUID}.jpg
          → Lấy Uri tạm thời qua FileProvider.getUriForFile()
          → cameraLauncher.launch(uri)
  → Người dùng chụp ảnh và bấm tick xác nhận trong Camera
      → cameraLauncher trả về success = true
      → InformationBottomSheet.binding.ivAvatar.load(uri)   [xem trước ảnh vừa chụp]
  → Người dùng nhập tên mới, chọn giới tính từ PopupMenu, chọn ngày sinh từ LoveDatePickerDialog
  → Người dùng bấm "Save" (binding.btnSave)
      → InformationBottomSheet.setupSave()
          → InformationBottomSheet.validateName(rawName)    [kiểm tra không rỗng, độ dài <= 20]
          → AvatarStorage.saveAvatar(selectedAvatarUri)
              → ImageDownscaler.saveDownscaled()            [lưu ảnh vĩnh viễn vào filesDir/avatars/{UUID}.img]
          → LoveSettingsRepository.saveYouInfo(info)        [ghi dữ liệu mới vào DataStore]
          → AvatarStorage.deleteAvatar(oldPath)             [xoá file avatar cũ trên ổ cứng để tiết kiệm bộ nhớ]
          → InformationBottomSheet.cleanupSelectedCameraFile() [xoá file tạm trong cacheDir]
          → InformationBottomSheet.dismiss()
  → Dữ liệu DataStore thay đổi
      → LoveSettingsRepository.youInfo (Flow) phát PersonInfo mới
      → HomeFragment.observeSettings() nhận giá trị mới
      → HomeFragment cập nhật: tên, icon giới tính, tuổi (calculateAge), Coil nạp ảnh avatar mới
```

---

## c) Giải thích chi tiết từng bước

1. **HomeFragment lấy dữ liệu từ đâu?**:
   - `HomeFragment` không lưu dữ liệu trong bộ nhớ tạm thời mà lắng nghe trực tiếp từ `LoveSettingsRepository` (Single Source of Truth) thông qua 4 luồng `Flow`:
     - `loveStartDate: Flow<LocalDate?>`: Ngày bắt đầu yêu.
     - `selectedBackgroundId: Flow<String?>`: ID hình nền (mã string hoặc tiền tố `custom:`).
     - `youInfo: Flow<PersonInfo>`: Tên, giới tính, ngày sinh, đường dẫn avatar của người dùng.
     - `partnerInfo: Flow<PersonInfo>`: Tên, giới tính, ngày sinh, đường dẫn avatar của đối phương.
   - Việc lắng nghe được bọc trong extension `collectOn(viewLifecycleOwner)` (chạy trong `Lifecycle.State.STARTED`), đảm bảo UI chỉ nhận dữ liệu khi màn hình đang hiển thị và tự động ngừng khi View bị huỷ.

2. **Cách tính số ngày yêu và tuổi**:
   - **Số ngày yêu**:
     ```kotlin
     val days = ChronoUnit.DAYS.between(startDate, LocalDate.now()).coerceAtLeast(0)
     ```
     - Dùng `ChronoUnit.DAYS.between` từ thư viện `java.time` chuẩn. Hàm `coerceAtLeast(0)` đảm bảo nếu người dùng chọn ngày trong tương lai thì số ngày hiển thị tối thiểu vẫn là 0 chứ không bị số âm.
     - Định dạng bằng chuỗi số nhiều (Plural string) `resources.getQuantityString(R.plurals.home_love_days, days.toInt(), days)`. Nếu chưa chọn ngày (`startDate == null`), hiển thị chữ mời gọi "Start your story".
   - **Số tuổi**:
     - Hàm `HomeFragment.calculateAge(birthday: LocalDate?)`:
     - Nếu ngày sinh là `null` hoặc ngày sinh lớn hơn ngày hiện tại (`birthday.isAfter(today)`), trả về `"--"`.
     - Ngược lại, tính số năm chênh lệch chính xác qua `Period.between(birthday, today).years.toString()`.

3. **Cơ chế đổi hình nền (`BackgroundBottomSheet`)**:
   - Hiển thị danh sách kết hợp giữa các ảnh tĩnh có sẵn trong ứng dụng (`romantic_1`, `romantic_2`, `romantic_3`) và các ảnh người dùng đã từng chọn được lưu trong thư mục `filesDir/backgrounds` (lấy qua hàm `storage.getSavedImages()`).
   - Khi người dùng bấm "Add Photo": Khởi chạy `photoPickerLauncher` dùng PhotoPicker chuẩn của Android (`PickVisualMediaRequest`).
   - Khi nhận được `Uri`: Hàm `BackgroundImageStorage.saveImage(uri)` được gọi trên `Dispatchers.IO`. Hàm này chuyển qua `ImageDownscaler.saveDownscaled()`, đọc `ExifInterface` để xoay đúng chiều, nén ảnh sao cho cạnh dài nhất không quá 1440px và lưu vào `context.filesDir/backgrounds/{UUID}.img`.
   - Lưu chuỗi `"custom:" + path` vào DataStore.
   - Khi DataStore cập nhật, `HomeFragment.observeSettings()` bắt được giá trị mới: nhận diện tiền tố `"custom:"`, lấy đường dẫn file và dùng thư viện **Coil** nạp ảnh: `binding.ivHomeBackground.load(File(path))`.

4. **Cơ chế chỉnh sửa hồ sơ (`InformationBottomSheet`)**:
   - Màn hình nhận diện đối tượng đang chỉnh sửa qua argument `personType` (`PersonType.YOU` hoặc `PersonType.PARTNER`).
   - **Chọn Avatar (Camera / Gallery)**:
     - Bấm vào avatar sẽ mở `AvatarOptionDialog`. Dialog này trả về kết quả qua `FragmentResultListener` với key `AvatarOptionDialog.REQUEST_KEY`.
     - Nếu chọn Gallery: Khởi chạy PhotoPicker chuẩn.
     - Nếu chọn Camera: Tạo một file tạm trong `context.cacheDir/avatar_camera/avatar_{UUID}.jpg`, lấy Uri bảo mật qua `FileProvider.getUriForFile` và mở camera hệ thống qua `ActivityResultContracts.TakePicture()`.
   - **Chọn Giới tính**: Bấm vào ô giới tính sẽ mở một `PopupMenu` của Android với 3 lựa chọn: Nam (`Gender.MALE`), Nữ (`Gender.FEMALE`), Khác (`Gender.OTHER`).
   - **Chọn Ngày sinh**: Mở `LoveDatePickerDialog` và lắng nghe kết quả qua `FragmentResultListener`.
   - **Kiểm tra hợp lệ tên người dùng**:
     - Trước khi lưu, hàm `validateName()` kiểm tra chuỗi nhập vào: nếu rỗng sẽ báo lỗi `R.string.information_error_name_empty`, nếu dài quá 20 ký tự sẽ báo lỗi `R.string.information_error_name_too_long`.

5. **Quy trình lưu Avatar và dọn dẹp bộ nhớ an toàn (Atomic Save & Clean)**:
   - Khi người dùng bấm "Save":
     1. Nén và lưu avatar mới vào thư mục vĩnh viễn: `avatarStorage.saveAvatar(selectedAvatarUri)` -> file được lưu tại `context.filesDir/avatars/{UUID}.img`.
     2. Ghi đè đối tượng `PersonInfo` mới vào DataStore Preferences (`saveYouInfo` hoặc `savePartnerInfo`).
     3. Đánh dấu `savedToDataStore = true`.
     4. **Xóa avatar cũ**: Gọi `avatarStorage.deleteAvatar(oldPath)`. Nhờ đó, máy người dùng không bị phình to dung lượng bởi hàng loạt ảnh đại diện cũ không còn dùng.
     5. **Xóa file tạm**: Gọi `cleanupSelectedCameraFile()` để xóa sạch ảnh chụp tạm thời trong `cacheDir/avatar_camera`.
     6. Khối `finally`: Nếu quá trình lưu vào DataStore thất bại (`savedToDataStore == false`), file avatar mới tạo sẽ lập tức bị xóa bỏ để tránh để lại rác trên ổ cứng.

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`HomeFragment`**:
  - `onViewCreated()`: Thiết lập lề trên với thanh trạng thái qua `applyTopInsets()`, gán listener cho các nút và bắt đầu lắng nghe `observeSettings()`.
  - `onResume()`: Luôn đặt lại `isNavigating = false` để kích hoạt lại khả năng bấm mở màn hình tiếp theo, phòng ngừa trường hợp người dùng bấm liên tục gây mở đúp màn hình.
- **`BackgroundBottomSheet`**:
  - `onStart()`: Gọi `expandWithMaxHeight(R.dimen.sheet_max_height_background)` để mở rộng chiều cao BottomSheet tối đa theo kích thước chuẩn.
  - `onDestroyView()`: Hủy gắn kết `binding.rvBackgrounds.adapter = null` để giải phóng bộ nhớ View.
- **`InformationBottomSheet`**:
  - `onSaveInstanceState()`: Lưu giữ toàn bộ trạng thái tạm thời vào Bundle gồm: `pendingCameraUri`, `pendingCameraPath`, `selectedAvatarUri`, `selectedCameraPath`, `selectedBirthday`, `selectedGender`.
  - Khi người dùng chụp ảnh bằng ứng dụng Camera của hệ thống, Android có thể tạm thời huỷ Activity/BottomSheet ở nền do thiếu RAM. Nhờ cơ chế `onSaveInstanceState` và phục hồi lại trong `onCreate()`, toàn bộ dữ liệu đang nhập dở và ảnh chụp xong không bao giờ bị mất.

---

## e) Đoạn code then chốt

### Đoạn 1: Tính số ngày yêu và tải hình nền tại `HomeFragment.observeSettings()`
```kotlin
// HomeFragment.kt
repository.loveStartDate.collectOn(viewLifecycleOwner) { startDate ->
    if (startDate == null) {
        binding.tvLoveDays.text = getString(R.string.home_love_days_empty)
    } else {
        val days = ChronoUnit.DAYS.between(startDate, LocalDate.now()).coerceAtLeast(0)
        binding.tvLoveDays.text = resources.getQuantityString(
            R.plurals.home_love_days, days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), days
        )
    }
}
```
*Giải thích*: Lắng nghe Flow ngày yêu từ DataStore, tính toán chênh lệch ngày bằng `ChronoUnit.DAYS` và tự động cập nhật text dạng số nhiều (plurals) theo thời gian thực.

### Đoạn 2: Xử lý lưu avatar và dọn rác ảnh cũ tại `InformationBottomSheet.setupSave()`
```kotlin
// InformationBottomSheet.kt
val avatarPath = selectedAvatarUri?.let { uri ->
    avatarStorage.saveAvatar(uri)
} ?: currentAvatarPath

val info = PersonInfo(name = name, gender = selectedGender ?: Gender.MALE, birthday = selectedBirthday, avatarPath = avatarPath)
if (personType == PersonType.YOU) repository.saveYouInfo(info) else repository.savePartnerInfo(info)
savedToDataStore = true

val oldPath = currentAvatarPath
if (newAvatarPath != null && oldPath != null && oldPath != newAvatarPath) {
    avatarStorage.deleteAvatar(oldPath)
}
cleanupSelectedCameraFile()
```
*Giải thích*: Đảm bảo tính toàn vẹn dữ liệu: chỉ xoá ảnh cũ và dọn cache tạm sau khi ảnh mới đã được lưu thành công vào DataStore Preferences.

### Đoạn 3: Nén ảnh và xoay EXIF chống OOM tại `ImageDownscaler.saveDownscaled()`
```kotlin
// ImageDownscaler.kt
// Mở stream ĐÚNG 1 LẦN duy nhất để tránh lỗi mất quyền đọc Uri ở lần mở thứ 2, 3
val imageBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext false
val bounds = decodeBounds(imageBytes) ?: return@withContext false
val sampleSize = calculateInSampleSize(bounds.width, bounds.height, maxEdge)
val bitmap = decodeSampled(imageBytes, sampleSize) ?: return@withContext false
val orientation = readExifOrientation(imageBytes)
val rotated = rotateBitmap(bitmap, orientation)
val scaled = scaleDown(rotated, maxEdge)
destination.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
```
*Giải thích*: Đọc byte mảng đúng 1 lần từ Uri, giải mã theo kích thước mẫu (`inSampleSize`), xoay theo chiều EXIF gốc và nén JPEG chất lượng 85% để ảnh luôn nhỏ gọn (<= 1440px), không bao giờ gây tràn RAM (OutOfMemory).

---

## f) Tự kiểm tra (Self-Check)

1. Khi người dùng đổi hình nền mới trong `BackgroundBottomSheet`, tại sao `HomeFragment` không cần viết hàm callback nào mà ảnh nền trên màn hình chính vẫn tự động cập nhật ngay lập tức?
2. Nếu người dùng chọn chụp ảnh từ Camera trong `InformationBottomSheet`, ảnh tạm thời được lưu ở thư mục nào và được dọn dẹp vào những thời điểm nào?
3. Tại sao trong `ImageDownscaler.kt`, việc đọc toàn bộ byte của Uri vào mảng `imageBytes` ngay từ đầu lại giải quyết được lỗi mất quyền truy cập (SecurityException / Permission Denial) trên Android?

---

# Phần 4: Start Date (HomeFragment → StartDateActivity → LoveDatePickerDialog)

## a) Đọc file theo thứ tự
1. `ui/home/HomeFragment.kt`: Lắng nghe sự kiện người dùng bấm vào khu vực đếm ngày (`layoutLoveDays`) để mở `StartDateActivity`, đồng thời quan sát luồng Flow `loveStartDate` để cập nhật số ngày yêu trên giao diện chính.
2. `ui/startdate/StartDateActivity.kt`: Màn hình chỉnh sửa ngày bắt đầu yêu, hiển thị ngày đã chọn, mở dialog chọn ngày, nhận kết quả từ Fragment Result API và lưu vào DataStore.
3. `ui/common/dialog/datepicker/LoveDatePickerDialog.kt`: Custom `DialogFragment` hiển thị bộ chọn ngày tháng năm dạng lưới lịch, giới hạn ngày tối đa là ngày hiện tại (`maximumDate`).
4. `ui/common/dialog/datepicker/CalendarDay.kt`: Data class biểu diễn một ô ngày trên lịch (ngày, thuộc tháng hiện tại, đang được chọn, có được phép bấm hay không).
5. `ui/common/dialog/datepicker/CalendarDayAdapter.kt`: `ListAdapter` hiển thị lưới lịch 7 cột (các ngày trong tuần từ Chủ Nhật đến Thứ Bảy).
6. `ui/common/dialog/datepicker/CalendarYearAdapter.kt`: `ListAdapter` hiển thị lưới 4 cột chọn năm khi người dùng chạm vào thanh tiêu đề tháng/năm.
7. `data/local/repository/LoveSettingsRepository.kt`: Nơi lưu trữ ngày bắt đầu yêu vào DataStore Preferences (`setLoveStartDate`) và phát ra luồng dữ liệu `loveStartDate: Flow<LocalDate?>`.

---

## b) Sơ đồ chuỗi gọi (Call Chain)

```text
Người dùng chạm vào ô đếm ngày trên HomeFragment (layoutLoveDays)
  → HomeFragment.binding.layoutLoveDays.setOnClickListener()
      → startActivity(Intent(requireContext(), StartDateActivity::class.java))
  → StartDateActivity.onCreate()
      → supportFragmentManager.setFragmentResultListener(LoveDatePickerDialog.REQUEST_KEY)
      → StartDateActivity.loadSavedDate()
          → LoveSettingsRepository.loveStartDate.first()   [đọc ngày lưu trong DataStore]
          → StartDateActivity.updateSelectedDate()          [hiển thị định dạng dd/MM/yyyy]
  → Người dùng chạm vào khung chọn ngày (layoutDateOfLove)
      → StartDateActivity.showDatePicker()
          → LoveDatePickerDialog.newInstance(selectedDate ?: LocalDate.now())
          → supportFragmentManager.showOnce(dialog, "LoveDatePickerDialog")
  → LoveDatePickerDialog.onViewCreated()
      → LoveDatePickerDialog.setupCalendar()
          → Cài đặt GridLayoutManager 7 cột cho CalendarDayAdapter
          → Cài đặt GridLayoutManager 4 cột cho CalendarYearAdapter
      → LoveDatePickerDialog.renderCalendar()
          → LoveDatePickerDialog.buildCalendarDays()        [tính offset thứ trong tuần, tạo danh sách CalendarDay]
          → dayAdapter.submitDays()                         [hiển thị các ngày lên lưới]
  → Người dùng đổi tháng (bấm btnPreviousMonth / btnNextMonth) hoặc đổi năm (bấm tvCalendarMonth)
      → Người dùng bấm chọn 1 ngày hợp lệ trên lưới
          → dayAdapter.onDayClick(clickedDate)
          → LoveDatePickerDialog.selectedDate = clickedDate
          → LoveDatePickerDialog.renderCalendar()           [vẽ lại ô ngày được chọn]
  → Người dùng bấm "Save" trên Dialog (btnSaveDate)
      → LoveDatePickerDialog.binding.btnSaveDate.setOnClickListener()
          → parentFragmentManager.setFragmentResult(REQUEST_KEY, bundle(RESULT_DATE = resultDate.toString()))
          → LoveDatePickerDialog.dismiss()
  → StartDateActivity nhận kết quả từ setFragmentResultListener
      → StartDateActivity.selectedDate = LocalDate.parse(dateString)
      → StartDateActivity.updateSelectedDate()              [cập nhật ngày mới lên TextView]
  → Người dùng bấm "Done" (binding.btnDone)
      → StartDateActivity.saveAndFinish()
          → LoveSettingsRepository.setLoveStartDate(date)   [ghi ngày vào DataStore Preferences]
          → StartDateActivity.finish()                      [đóng màn hình StartDateActivity]
  → Quay trở lại HomeFragment
      → LoveSettingsRepository.loveStartDate (Flow) tự động phát ra giá trị LocalDate mới
      → HomeFragment.observeSettings() nhận ngày mới
      → ChronoUnit.DAYS.between(startDate, LocalDate.now()) tính toán lại số ngày
      → HomeFragment.binding.tvLoveDays.text cập nhật số ngày yêu mới ngay lập tức
```

---

## c) Giải thích chi tiết từng bước

1. **Khởi tạo và hiển thị ngày hiện tại trong `StartDateActivity`**:
   - Khi người dùng bấm vào khu vực đếm ngày trên `HomeFragment`, `StartDateActivity` được khởi động.
   - Trong `StartDateActivity.onCreate()`:
     - Đăng ký `supportFragmentManager.setFragmentResultListener` với key `LoveDatePickerDialog.REQUEST_KEY` để sẵn sàng đón nhận ngày trả về từ dialog.
     - Kiểm tra `savedInstanceState`: Nếu người dùng vừa xoay màn hình trong khi đang chọn dở, ngày tạm thời sẽ được khôi phục từ `KEY_PENDING_DATE`.
     - Nếu mở mới hoàn toàn, hàm `loadSavedDate()` chạy một coroutine đọc giá trị từ DataStore: `repository.loveStartDate.first()`. Sau đó định dạng hiển thị thành chuỗi ngày tháng năm (`dd/MM/yyyy`) qua hàm `updateSelectedDate()`.

2. **Cấu tạo và cơ chế hoạt động của `LoveDatePickerDialog`**:
   - `LoveDatePickerDialog` là một custom `DialogFragment` hiển thị lịch dạng lưới, được mở qua hàm tĩnh `newInstance(initialDate: LocalDate)`.
   - **Chặn chọn ngày tương lai (`maximumDate`)**:
     - Biến `maximumDate` được gán bằng `LocalDate.now()`. Bất kỳ ngày nào nằm sau ngày hôm nay (`date.isAfter(maximumDate)`) sẽ có cờ `isEnabled = false`, chữ bị mờ đi và click listener bị vô hiệu hóa.
   - **Thuật toán dựng lưới ngày (`buildCalendarDays`)**:
     1. Lấy ngày đầu tiên của tháng: `firstDay = displayedMonth.atDay(1)`.
     2. Xác định thứ trong tuần của ngày mùng 1: Java quy ước Monday = 1 đến Sunday = 7. Hàm đổi sang hệ Chủ Nhật = 0 bằng công thức: `val offset = firstDay.dayOfWeek.value % 7`.
     3. Xác định ngày bắt đầu của cả khung lưới (có thể là những ngày cuối của tháng trước): `gridStartDate = firstDay.minusDays(offset.toLong())`.
     4. Tính tổng số ô cần vẽ theo bội số của 7 cột: `totalCells = ((cells + 6) / 7) * 7`.
     5. Dùng vòng lặp tạo ra danh sách đối tượng `CalendarDay`, đánh dấu ô nào thuộc tháng hiện tại (`isCurrentMonth`), ô nào đang được chọn (`isSelected`) và ô nào hợp lệ (`isEnabled`).

3. **Chuyển đổi linh hoạt giữa chọn Ngày và chọn Năm**:
   - Bình thường, lịch hiển thị lưới 7 cột ngày (`rvCalendarDays`).
   - Khi người dùng chạm vào tiêu đề tháng năm (`tvCalendarMonth`), biến `isYearPickerVisible` đảo trạng thái từ `false` sang `true`.
   - Hàm `renderCalendar()` sẽ ẩn `rvCalendarDays` và hiện `rvCalendarYears` (lưới 4 cột chọn năm).
   - Danh sách năm được sinh từ năm 1 đến năm hiện tại (`1..maximumDate.year`). Khi người dùng chạm vào một năm trong `CalendarYearAdapter`, tháng hiển thị chuyển về năm đó, `isYearPickerVisible` được tắt lại về `false` và lưới ngày của tháng tương ứng được vẽ lại.

4. **Giao tiếp giữa Dialog và Activity qua Fragment Result API**:
   - Khi người dùng chọn ngày ưng ý và bấm nút "Save" (`btnSaveDate`) trong dialog:
     ```kotlin
     parentFragmentManager.setFragmentResult(
         REQUEST_KEY,
         Bundle().apply { putString(RESULT_DATE, resultDate.toString()) }
     )
     dismiss()
     ```
   - Không cần dùng interface callback hay ép kiểu Activity (`as? OnDateSelectedListener`), `StartDateActivity` tự động nhận Bundle qua `setFragmentResultListener` đã đăng ký trong `onCreate`, bóc tách chuỗi ISO (dạng `"2024-05-20"`), parse lại thành `LocalDate` và hiển thị lên màn hình.

5. **Lưu trữ và cơ chế Reactive tự động cập nhật ở Home**:
   - Khi người dùng bấm nút "Done" (`btnDone`), hàm `saveAndFinish()` được kích hoạt:
     - Nút Done bị vô hiệu hóa tạm thời (`binding.btnDone.isEnabled = false`) để tránh gửi yêu cầu ghi trùng lặp.
     - Gọi `repository.setLoveStartDate(date)`. Trong `LoveSettingsRepository.kt`, hàm này dùng `store.edit` để ghi đè chuỗi ngày vào key DataStore `LOVE_START_DATE`.
     - Sau khi lưu xong, gọi `finish()` để đóng `StartDateActivity`.
   - **Vì sao màn hình Home tự động đổi số ngày mà không cần code lắng nghe kết quả từ StartDateActivity?**:
     - Trong `HomeFragment.kt`, hàm `observeSettings()` đang lắng nghe luồng `repository.loveStartDate.collectOn(viewLifecycleOwner)`.
     - DataStore Preferences trong Android hoạt động dựa trên Kotlin `Flow`. Ngay khi `StartDateActivity` ghi giá trị mới xuống ổ cứng, DataStore sẽ tự động phát (emit) giá trị `LocalDate` mới này tới toàn bộ các nơi đang lắng nghe nó.
     - Khối code trong `HomeFragment` lập tức nhận được ngày mới, thực hiện phép tính `ChronoUnit.DAYS.between(startDate, LocalDate.now())` và gán vào `tvLoveDays.text`. Mọi thứ diễn ra hoàn toàn tự động, phân tách trách nhiệm tuyệt đối.

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`StartDateActivity`**:
  - `onCreate()`: Đăng ký lắng nghe Fragment Result từ Dialog trước khi có bất kỳ sự kiện nào xảy ra; khôi phục ngày từ `savedInstanceState` (nếu có) hoặc tải từ DataStore.
  - `onSaveInstanceState()`: Lưu `selectedDate` vào Bundle với key `KEY_PENDING_DATE`. Nhờ vậy, nếu người dùng đã chọn ngày mới trong dialog nhưng chưa kịp bấm "Done" mà xoay ngang màn hình, giá trị vừa chọn không bao giờ bị mất về ngày cũ.
- **`LoveDatePickerDialog`**:
  - `onSaveInstanceState()`: Lưu giữ 3 trạng thái gồm `STATE_SELECTED_DATE`, `STATE_DISPLAYED_MONTH`, và `STATE_YEAR_PICKER_VISIBLE`. Khi xoay màn hình trong lúc dialog đang mở, lịch phục hồi đúng tháng và chế độ xem (lưới năm hay lưới ngày).
  - `onStart()`: Can thiệp vào `dialog?.window?.setLayout` để đặt chiều rộng dialog bằng 90% chiều rộng màn hình thiết bị (`widthPixels * 0.90f`), đảm bảo lịch hiển thị cân đối trên cả điện thoại màn hình nhỏ và màn hình lớn.
  - `onDestroyView()`: Giải phóng adapter của 2 RecyclerView (`rvCalendarDays.adapter = null`, `rvCalendarYears.adapter = null`) để tránh rò rỉ bộ nhớ View.

---

## e) Đoạn code then chốt

### Đoạn 1: Tính toán lưới ngày lịch trong `LoveDatePickerDialog.buildCalendarDays()`
```kotlin
// LoveDatePickerDialog.kt
val firstDay = displayedMonth.atDay(1)
val offset = firstDay.dayOfWeek.value % 7 // Monday=1..Sunday=7 đổi thành Sunday=0
val gridStartDate = firstDay.minusDays(offset.toLong())
val cells = offset + displayedMonth.lengthOfMonth()
val totalCells = ((cells + 6) / 7) * 7
return (0 until totalCells).map { index ->
    val date = gridStartDate.plusDays(index.toLong())
    CalendarDay(date = date, isCurrentMonth = YearMonth.from(date) == displayedMonth,
        isSelected = date == selectedDate, isEnabled = !date.isAfter(maximumDate))
}
```
*Giải thích*: Thuật toán tính toán chuẩn xác ngày bắt đầu của lưới (bao gồm các ngày cuối tháng trước để bù đủ tuần), xác định ô ngày hợp lệ (không vượt quá ngày hiện tại) và ô đang được chọn.

### Đoạn 2: Truyền kết quả không phụ thuộc vòng đời qua Fragment Result API
```kotlin
// LoveDatePickerDialog.kt (Gửi kết quả)
parentFragmentManager.setFragmentResult(
    REQUEST_KEY,
    Bundle().apply { putString(RESULT_DATE, resultDate.toString()) }
)
dismiss()

// StartDateActivity.kt (Nhận kết quả trong onCreate)
supportFragmentManager.setFragmentResultListener(LoveDatePickerDialog.REQUEST_KEY, this) { _, bundle ->
    val dateString = bundle.getString(LoveDatePickerDialog.RESULT_DATE)
    selectedDate = dateString?.let(LocalDate::parse)
    updateSelectedDate()
}
```
*Giải thích*: Cơ chế giao tiếp hiện đại giữa Fragment/Dialog và Activity, không gây rò rỉ tham chiếu (leak reference) và sống sót an toàn qua việc huỷ/tạo lại Activity khi xoay màn hình.

### Đoạn 3: Lưu DataStore và kết thúc màn hình tại `StartDateActivity.saveAndFinish()`
```kotlin
// StartDateActivity.kt
private fun saveAndFinish() {
    val date = selectedDate ?: return
    binding.btnDone.isEnabled = false
    lifecycleScope.launch {
        try {
            repository.setLoveStartDate(date)
            finish()
        } catch (e: Exception) {
            binding.btnDone.isEnabled = true
            toast(R.string.start_date_save_error)
        }
    }
}
```
*Giải thích*: Vô hiệu hóa nút bấm tránh spam click, ghi bất đồng bộ vào DataStore trên coroutine và đóng Activity để nhường quyền cập nhật tự động cho Flow ở HomeFragment.

---

## f) Tự kiểm tra (Self-Check)

1. Trong `LoveDatePickerDialog.kt`, tại sao biến `maximumDate` lại được gán bằng `LocalDate.now()` và điều gì ngăn người dùng chọn một ngày trong tương lai?
2. Tại sao `StartDateActivity` không cần truyền ngày mới qua `setResult()` hay `startActivityForResult` về cho `HomeFragment` mà `HomeFragment` vẫn biết để cập nhật số ngày yêu?
3. Nếu người dùng mở `LoveDatePickerDialog`, chuyển sang xem danh sách chọn năm rồi xoay màn hình điện thoại, nhờ đoạn code nào mà dialog vẫn giữ nguyên chế độ xem danh sách năm sau khi xoay?

---

# Phần 5: Love Test (NameTestActivity, HoroscopeTestActivity, LoveTestCalculator, ResultActivity)

## a) Đọc file theo thứ tự
1. `ui/lovetest/LoveTestFragment.kt`: Tab 2 trên `HomeActivity`, hiển thị 2 thẻ chọn kiểm tra tình yêu: theo Tên (`cardNameTest`) và theo Cung hoàng đạo (`cardHoroscopeTest`).
2. `ui/lovetest/name/NameTestActivity.kt`: Màn hình bói tình yêu bằng cách nhập 2 tên, kiểm tra rỗng, tính điểm và chuyển sang `ResultActivity`.
3. `ui/lovetest/domain/LoveTestCalculator.kt`: Thuật toán băm SHA-256 kết hợp chuẩn hoá và sắp xếp tên, sinh điểm phần trăm (0–100%) mang tính tất định (cùng 2 đối tượng luôn cho ra cùng kết quả).
4. `ui/lovetest/horoscope/HoroscopeTestActivity.kt`: Màn hình bói cung hoàng đạo, hiển thị 2 cung hoàng đạo đã chọn, mở BottomSheet chọn cung và chuyển tiếp sang `ResultActivity`.
5. `ui/lovetest/horoscope/HoroscopeTestViewModel.kt`: `ViewModel` lưu trữ trạng thái 2 cung hoàng đạo (`HoroscopeUiState`), tính toán phần trăm tương thích qua `LoveTestCalculator`.
6. `ui/lovetest/horoscope/HoroscopePickerBottomSheet.kt`: BottomSheet hiển thị lưới danh sách 12 cung hoàng đạo để người dùng chọn, trả kết quả qua Fragment Result API.
7. `ui/lovetest/horoscope/ZodiacSign.kt`: Enum 12 cung hoàng đạo kèm tài nguyên chuỗi tên (`nameRes`) và icon đại diện (`iconRes`).
8. `ui/lovetest/horoscope/ZodiacAdapter.kt`: Adapter lưới 2 cột hiển thị các cung và đánh dấu cung đang được chọn.
9. `ui/lovetest/result/ResultActivity.kt`: Màn hình hiển thị kết quả kiểm tra (điểm %, thông điệp theo bậc điểm, icon cung hoàng đạo, các nút Thử lại, Chia sẻ và Quay về tab Love Test).

---

## b) Sơ đồ chuỗi gọi (Call Chain)

### Luồng 1: Bói tình yêu theo Tên (Name Test)
```text
Người dùng ở LoveTestFragment chạm vào thẻ "Name Test" (cardNameTest)
  → LoveTestFragment.setupCardClicks()
      → startActivity(Intent(requireContext(), NameTestActivity::class.java))
  → NameTestActivity.onCreate()
      → Người dùng nhập tên bạn vào etYourName, tên người ấy vào etPartnerName
      → Người dùng chạm vào nút "Love Test" (btnLoveTest)
          → NameTestActivity.binding.btnLoveTest.setOnClickListener()
              → Kiểm tra rỗng: yourName.isBlank() hoặc partnerName.isBlank()
              → LoveTestCalculator.calculateNameTest(yourName, partnerName)
                  → Chuẩn hóa chữ thường, sắp xếp thứ tự A-Z (đảm bảo tính giao hoán)
                  → Băm SHA-256 chuỗi "${len1}:${name1}|${len2}:${name2}"
                  → Lấy 2 byte đầu tiên chuyển thành số nguyên: (byte[0] shl 8 or byte[1]) % 101 -> kết quả 0..100
              → ResultActivity.newNameTestIntent(this, yourName, partnerName, percentage)
              → NameTestActivity.startActivity(intent)
  → ResultActivity.onCreate()
      → Đọc extras: EXTRA_PERSON_ONE, EXTRA_PERSON_TWO, EXTRA_PERCENTAGE
      → Nhận diện isHoroscope = false (do không có EXTRA_SIGN_ONE)
      → Lấy thông điệp qua getNameTestMessageRes(percentage) (4 bậc điểm)
      → Hiển thị tên 2 người, điểm số % và thông điệp tương ứng
```

### Luồng 2: Bói tình yêu theo Cung hoàng đạo (Horoscope Test)
```text
Người dùng ở LoveTestFragment chạm vào thẻ "Horoscope Test" (cardHoroscopeTest)
  → LoveTestFragment.setupCardClicks()
      → startActivity(Intent(requireContext(), HoroscopeTestActivity::class.java))
  → HoroscopeTestActivity.onCreate()
      → HoroscopeTestActivity.setupResultListeners() (đăng ký REQUEST_MY_SIGN, REQUEST_PARTNER_SIGN)
      → HoroscopeTestActivity.observeState() (lắng nghe viewModel.uiState qua collectOn)
  → Người dùng chạm vào khung cung của mình (layoutYourZodiac)
      → HoroscopeTestActivity.openPicker(REQUEST_MY_SIGN, viewModel.uiState.value.mySign)
          → supportFragmentManager.showOnce(HoroscopePickerBottomSheet.newInstance(...), TAG)
  → HoroscopePickerBottomSheet.onViewCreated()
      → Người dùng chọn 1 cung trong lưới 12 cung (ZodiacAdapter)
          → setFragmentResult(requestKey, bundleOf(RESULT_SIGN to sign.name))
          → HoroscopePickerBottomSheet.dismiss()
  → HoroscopeTestActivity nhận kết quả từ Fragment Result
      → viewModel.setMySign(sign)
      → HoroscopeTestViewModel cập nhật _uiState
      → HoroscopeTestActivity.observeState() nhận state mới → cập nhật icon và tên cung trên giao diện
  → Người dùng chạm vào nút "Love Test" (btnLoveTest)
      → HoroscopeTestActivity.binding.btnLoveTest.setOnClickListener()
          → viewModel.calculatePercent()
              → LoveTestCalculator.calculateNameTest("zodiac:${mySign.name}", "zodiac:${partnerSign.name}")
          → ResultActivity.newHoroscopeIntent(this, mySign, partnerSign, percentage)
          → HoroscopeTestActivity.startActivity(intent)
  → ResultActivity.onCreate()
      → Đọc extras: EXTRA_SIGN_ONE, EXTRA_SIGN_TWO, EXTRA_PERCENTAGE
      → Nhận diện isHoroscope = true
      → bindZodiacIcons(signOne, signTwo) (nạp icon 2 cung và đổi sang màu hồng R.color.primary)
      → Lấy thông điệp qua getHoroscopeMessageRes(percentage) (7 bậc điểm chi tiết)
      → Hiển thị tên 2 cung hoàng đạo, điểm số % và thông điệp tương ứng
```

### Luồng 3: Các thao tác trên màn hình kết quả ResultActivity (Share, Retry, Back)
```text
- Người dùng bấm "Share" (btnShare):
    → Chuẩn bị nội dung chuỗi: "$personOne ❤ $personTwo: $percentText\n\n$message"
    → Tạo Intent(Intent.ACTION_SEND) với kiểu "text/plain"
    → Mở hộp thoại chia sẻ của hệ thống Android: startActivity(Intent.createChooser(intent, ...))

- Người dùng bấm "Retry" (btnRetry):
    → ResultActivity.finish()
    → Đóng ResultActivity, màn hình tự động quay lại NameTestActivity hoặc HoroscopeTestActivity
    → Người dùng có thể chỉnh sửa tên hoặc đổi cung và thử lại ngay lập tức

- Người dùng bấm nút "Back" (btnBack trên giao diện hoặc phím Back hệ thống):
    → ResultActivity.navigateToTestMenu()
    → Khởi chạy HomeActivity với cờ FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP
      kèm Extra: EXTRA_TAB = TAB_LOVE_TEST
    → ResultActivity.finish()
    → HomeActivity nhận onNewIntent và tự động hiển thị tab Love Test
```

---

## c) Giải thích chi tiết từng bước

1. **Thuật toán tính điểm tất định tại `LoveTestCalculator`**:
   - `LoveTestCalculator` là một Singleton `object`, cung cấp hàm `calculateNameTest(firstName: String, secondName: String): Int`.
   - **Tính giao hoán (Commutative)**: Người dùng nhập `A` và `B` hay nhập `B` và `A` thì kết quả bắt buộc phải giống hệt nhau. Để đạt được điều này, danh sách 2 tên được chuyển thành chữ thường (`lowercase(Locale.ROOT)`) và sắp xếp theo thứ tự bảng chữ cái (`.sorted()`).
   - **Tính tất định (Deterministic)**: Dự án không sử dụng hàm sinh số ngẫu nhiên `Random.nextInt()`, vì nếu cùng một cặp tên mà mỗi lần bấm ra một số khác nhau thì ứng dụng sẽ mất độ tin cậy. Thay vào đó, chuỗi kết hợp `"${it.length}:$it"` được băm bằng thuật toán mật mã `SHA-256`. Hai byte đầu của mã băm được ghép thành một số nguyên 16-bit rồi lấy dư cho 101 (`% 101`). Điểm số sinh ra luôn nằm trong khoảng từ 0% đến 100% và cố định vĩnh viễn với cùng một cặp tên/cung.

2. **Luồng kiểm tra tên (`NameTestActivity`)**:
   - Giao diện có 2 ô nhập văn bản: `etYourName` và `etPartnerName`. Ô thứ hai được gắn extension `onImeDone()` để khi người dùng bấm nút Done trên bàn phím thì bàn phím sẽ tự động ẩn đi.
   - Khi bấm nút `btnLoveTest`:
     - Kiểm tra nếu một trong hai tên bị bỏ trống (`isBlank()`), gán thuộc tính `.error` trên ô tương ứng (`R.string.name_test_error_empty_name`) và dừng lại.
     - Nếu hợp lệ, gọi `LoveTestCalculator.calculateNameTest(yourName, partnerName)`.
     - Đóng gói dữ liệu vào Intent qua hàm tiện ích tĩnh `ResultActivity.newNameTestIntent(...)` và khởi chạy `ResultActivity`.

3. **Luồng kiểm tra cung hoàng đạo (`HoroscopeTestActivity` + `HoroscopeTestViewModel`)**:
   - Trạng thái 2 cung hoàng đạo được giữ trong `HoroscopeTestViewModel` với `MutableStateFlow<HoroscopeUiState>` (mặc định ban đầu cả hai đều là `ZodiacSign.ARIES` - Bạch Dương).
   - Hàm `observeState()` lắng nghe luồng `viewModel.uiState` qua `collectOn(this)` và tự động cập nhật icon và tên của cả 2 cung hoàng đạo lên giao diện.
   - Khi bấm vào khung chọn của một bên:
     - Gọi `openPicker()` mở `HoroscopePickerBottomSheet` kèm theo `requestKey` tương ứng (`REQUEST_MY_SIGN` hoặc `REQUEST_PARTNER_SIGN`).
     - Người dùng chọn 1 trong 12 cung trong `ZodiacAdapter`: BottomSheet gọi `setFragmentResult(requestKey, ...)` rồi đóng lại.
     - `HoroscopeTestActivity` nhận kết quả qua `supportFragmentManager.setFragmentResultListener` và cập nhật lại ViewModel (`setMySign` hoặc `setPartnerSign`).
   - Khi bấm nút `btnLoveTest`:
     - Hàm `viewModel.calculatePercent()` tái sử dụng thuật toán `LoveTestCalculator.calculateNameTest` bằng cách truyền vào 2 chuỗi định danh `"zodiac:${mySign.name}"` và `"zodiac:${partnerSign.name}"`.
     - Mở `ResultActivity` qua Intent `ResultActivity.newHoroscopeIntent(...)`.

4. **Trình bày kết quả thông minh trên `ResultActivity`**:
   - `ResultActivity` tự động nhận diện chế độ kiểm tra:
     ```kotlin
     val isHoroscope = signOne != null && signTwo != null
     ```
   - Nếu là Horoscope: Icon của 2 bên hiển thị hình cung hoàng đạo tương ứng (`ivPersonOne.setImageResource(signOne.iconRes)`) và được phủ màu hồng (`imageTintList = pink`).
   - Nếu là Name Test: Icon giữ mặc định của giao diện XML.
   - **Phân bậc thông điệp kết quả**:
     - *Name Test* có 4 bậc thông điệp (`getNameTestMessageRes`):
       - 0–20%: `name_test_message_0_20`
       - 21–50%: `name_test_message_21_50`
       - 51–80%: `name_test_message_51_80`
       - 81–100%: `name_test_message_81_100`
     - *Horoscope Test* có 7 bậc thông điệp chi tiết (`getHoroscopeMessageRes`):
       - 0–5%, 6–20%, 21–35%, 36–50%, 51–70%, 71–90%, 91–100%.

5. **Cơ chế các nút điều hướng (Retry, Share, Back)**:
   - **Nút Thử lại (`btnRetry`)**: Đơn giản chỉ gọi `finish()`. Thao tác này sẽ đóng `ResultActivity`, đưa người dùng quay lại Activity trước đó (`NameTestActivity` hoặc `HoroscopeTestActivity`) với dữ liệu cũ vẫn còn nguyên vẹn trên màn hình để họ tiện đổi tên hoặc đổi cung.
   - **Nút Chia sẻ (`btnShare`)**: Định dạng chuỗi gồm tên hai người, số điểm và lời nhắn, sau đó mở `Intent.createChooser` giúp người dùng chia sẻ lên Zalo, Messenger, Facebook, v.v.
   - **Nút Quay lại (`btnBack` và phím Back hệ thống)**: Gọi `navigateToTestMenu()`. Hàm này tạo `Intent` trỏ về `HomeActivity` kèm 2 cờ: `FLAG_ACTIVITY_CLEAR_TOP` (xoá sạch các màn hình test trung gian khỏi stack) và `FLAG_ACTIVITY_SINGLE_TOP` (gọi lại `HomeActivity` hiện có mà không tạo mới). Đồng thời truyền `EXTRA_TAB = TAB_LOVE_TEST` để Home tự động mở lại đúng tab bói tình yêu.

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`LoveTestFragment` / `NameTestActivity` / `HoroscopeTestActivity`**:
  - Đều có biến cờ `isNavigating` và được đặt lại `false` trong `onResume()`. Điều này triệt tiêu hoàn toàn lỗi người dùng click liên tiếp vào nút khiến app mở 2 màn hình cùng lúc.
- **`HoroscopeTestViewModel`**:
  - Lưu trữ `HoroscopeUiState` trên ViewModel độc lập với vòng đời của Activity. Khi người dùng xoay ngang điện thoại, `HoroscopeTestActivity` bị tạo lại nhưng 2 cung hoàng đạo đã chọn vẫn được bảo toàn nguyên vẹn từ ViewModel.
- **`ResultActivity`**:
  - Toàn bộ dữ liệu hiển thị (tên, cung, điểm số) đều nằm trong `Intent Extras`. Theo cơ chế của Android, khi xoay màn hình, `Intent` của Activity luôn được lưu giữ nguyên trạng. Do đó màn hình kết quả không cần dùng ViewModel hay Database mà vẫn hiển thị hoàn hảo sau khi xoay.

---

## e) Đoạn code then chốt

### Đoạn 1: Thuật toán tính điểm giao hoán và tất định tại `LoveTestCalculator.calculateNameTest()`
```kotlin
// LoveTestCalculator.kt
val names = listOf(firstName.trim().lowercase(Locale.ROOT), secondName.trim().lowercase(Locale.ROOT)).sorted()
val input = names.joinToString("|") { "${it.length}:$it" }
val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
val value = ((digest[0].toInt() and 0xFF) shl 8) or (digest[1].toInt() and 0xFF)
return value % 101
```
*Giải thích*: Sắp xếp 2 tên để đảm bảo tính giao hoán (A+B hay B+A đều cùng input), băm SHA-256 và lấy modulo 101 để sinh điểm số 0–100% cố định, không phụ thuộc vào thời gian hay tính ngẫu nhiên.

### Đoạn 2: Mở Picker và nhận kết quả qua Fragment Result API tại `HoroscopeTestActivity`
```kotlin
// HoroscopeTestActivity.kt
supportFragmentManager.setFragmentResultListener(REQUEST_MY_SIGN, this) { _, bundle ->
    bundle.toZodiacSign()?.let { viewModel.setMySign(it) }
}
private fun openPicker(requestKey: String, current: ZodiacSign) {
    supportFragmentManager.showOnce(HoroscopePickerBottomSheet.newInstance(requestKey, current), HoroscopePickerBottomSheet.TAG)
}
```
*Giải thích*: Tách biệt việc chọn cung hoàng đạo của mình và của đối phương bằng 2 `requestKey` độc lập, nhận kết quả an toàn không lo rò rỉ bộ nhớ.

### Đoạn 3: Quay về Tab Love Test sạch sẽ tại `ResultActivity.navigateToTestMenu()`
```kotlin
// ResultActivity.kt
private fun navigateToTestMenu() {
    startActivity(
        Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(HomeActivity.EXTRA_TAB, HomeActivity.TAB_LOVE_TEST)
        }
    )
    finish()
}
```
*Giải thích*: Kết hợp `CLEAR_TOP` và `SINGLE_TOP` để dọn sạch toàn bộ các Activity kiểm tra khỏi ngăn xếp và đưa người dùng quay lại instance `HomeActivity` duy nhất với tab Love Test được chọn tự động qua `onNewIntent`.

---

## f) Tự kiểm tra (Self-Check)

1. Tại sao trong `LoveTestCalculator.kt`, danh sách hai tên người dùng bắt buộc phải được gọi `.sorted()` trước khi đưa vào hàm băm SHA-256?
2. Khi người dùng bấm nút "Retry" trên `ResultActivity`, tại sao phương thức chỉ cần gọi `finish()` là đủ để đưa người dùng về lại màn hình nhập liệu trước đó với dữ liệu cũ vẫn còn?
3. Trong `HoroscopeTestActivity.kt`, hàm `viewModel.calculatePercent()` đã tận dụng lại thuật toán bói tên như thế nào mà không cần phải viết thêm một thuật toán riêng cho cung hoàng đạo?

---

# Phần 6: Memory (Room Database, Flow, MemoryViewModel, ConcatAdapter, AddMemoryActivity)

## a) Đọc file theo thứ tự
1. `data/local/database/MemoryEntity.kt`: Room Entity định nghĩa cấu trúc bảng `memories` (khoá chính `id`, tiêu đề `title`, ngày kỷ niệm `date`, đường dẫn ảnh `imagePath`).
2. `data/local/database/MemoryDao.kt`: Data Access Object cung cấp hàm truy vấn reactive `observeAll(): Flow<List<MemoryEntity>>`, hàm chèn `insert()`, tìm kiếm `findById()` và hàm xoá `delete()`.
3. `data/local/storage/MemoryImageStorage.kt`: Quản lý việc lưu ảnh kỷ niệm vào thư mục nội bộ `context.filesDir/memories` và xoá file an toàn khỏi ổ đĩa.
4. `data/local/repository/MemoryRepository.kt`: Repository trung gian điều phối giữa Room Database và `MemoryImageStorage` trên luồng `Dispatchers.IO`.
5. `ui/memory/MemoryViewModel.kt`: Hilt ViewModel quản lý trạng thái tải danh sách (`MemoryUiState.Loading / Success / Error`) qua `StateFlow` kèm cơ chế tải lại (`retry`), và trạng thái lưu (`MemorySaveState`).
6. `ui/memory/MemoryListFragment.kt`: Fragment nội dung của Tab 3, sử dụng `ConcatAdapter` kết hợp danh sách và chân trang, quản lý hiển thị các trạng thái UI và điều khiển vị trí nút thêm (+).
7. `ui/memory/MemoryAdapter.kt`: `ListAdapter` hiển thị danh sách các thẻ kỷ niệm, xử lý thao tác xoá qua nhấn giữ lâu (Long-press) và hỗ trợ hành động tiếp cận (Accessibility Action) cho TalkBack.
8. `ui/memory/MemoryFooterAdapter.kt`: Adapter chân trang chứa nút thêm kỷ niệm khi danh sách có từ 1 đến 4 mục.
9. `ui/memory/AddMemoryActivity.kt`: Màn hình tạo kỷ niệm mới (chọn ảnh, tiêu đề, ngày), hiển thị hộp thoại xác nhận huỷ bỏ khi bấm Back nếu có thay đổi chưa lưu.
10. `ui/memory/MemoryBitmap.kt`: Hàm tiện ích `decodeMemoryBitmap` giải mã ảnh xem trước nhanh chóng, thu nhỏ kích thước cạnh dài tối đa 1024px để chống tràn bộ nhớ (OOM).

---

## b) Sơ đồ chuỗi gọi (Call Chain)

### Luồng 1: Tải và hiển thị danh sách kỷ niệm từ Room Database
```text
Người dùng mở Tab Memory trên HomeActivity
  → MemoryListFragment.onViewCreated()
      → MemoryViewModel.uiState.collectOn(viewLifecycleOwner, action = ::render)
          → MemoryViewModel.retryTrigger.flatMapLatest { ... }
          → MemoryRepository.observeMemories()
          → MemoryDao.observeAll()                          [Room SQLite phát ra luồng Flow<List<MemoryEntity>>]
          → MemoryViewModel phát trạng thái:
              1. onStart { emit(MemoryUiState.Loading) }    [khi đang nạp]
              2. emit(MemoryUiState.Success(memories))      [khi đã có dữ liệu từ SQLite]
  → MemoryListFragment.render(state)
      → Nếu state là Loading: hiện ProgressBar progressMemories, ẩn danh sách và rỗng
      → Nếu state là Error: hiện layoutErrorState và nút Thử lại (btnRetryMemories)
      → Nếu state là Success:
          → Nếu danh sách rỗng (isEmpty):
              → Hiện layoutEmptyState (chứa nút + chính giữa)
              → Ẩn RecyclerView rvMemories
          → Nếu có dữ liệu:
              → Hiện RecyclerView rvMemories, ẩn layoutEmptyState
              → memoryAdapter.submitList(state.memories)
                  → Nếu count in 1..4: footerAdapter.setVisible(true), ẩn btnAddMemoryHeader
                  → Nếu count >= 5: footerAdapter.setVisible(false), hiện btnAddMemoryHeader (nút + trên góc)
              → MemoryAdapter.onBindViewHolder()
                  → Gán title, date
                  → Coil tải ảnh từ đĩa: binding.ivMemoryPhoto.load(File(memory.imagePath))
```

### Luồng 2: Thêm một kỷ niệm mới (AddMemoryActivity)
```text
Người dùng chạm vào nút "+" (btnAddMemory / btnAddMemoryHeader / btnAddMemoryFooter)
  → MemoryListFragment.openAddMemory()
      → startActivity(Intent(requireContext(), AddMemoryActivity::class.java))
  → AddMemoryActivity.onCreate()
      → Người dùng chạm vào khung chọn ảnh (cardMemoryPhoto)
          → photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
          → Người dùng chọn 1 bức ảnh từ bộ sưu tập
          → AddMemoryActivity.renderPhoto()
              → decodeMemoryBitmap(contentResolver, uri)   [thu nhỏ ảnh preview <= 1024px trên Dispatchers.IO]
              → binding.ivMemoryPreview.setImageBitmap(bitmap)
      → Người dùng chạm vào khung chọn ngày (layoutMemoryDate)
          → LoveDatePickerDialog.newInstance(selectedDate ?: LocalDate.now()).showNow(...)
          → Người dùng chọn ngày và bấm Save trên Dialog
          → AddMemoryActivity.observeDatePickerResult() nhận kết quả và hiển thị
      → Người dùng nhập tiêu đề kỷ niệm vào etMemoryTitle
      → Người dùng bấm nút "Save" (btnSaveMemory)
          → AddMemoryActivity.saveMemory()
              → Kiểm tra hợp lệ: tiêu đề không rỗng, <= 100 ký tự; ngày != null; ảnh != null
              → MemoryViewModel.saveMemory(title, date.toString(), image)
                  → MemoryViewModel._saveState.value = MemorySaveState.Saving
                  → MemoryRepository.addMemory(title, date, imageUri) [chạy trên Dispatchers.IO]
                      → MemoryImageStorage.saveImage(imageUri)
                          → Ghi file ảnh vào context.filesDir/memories/{UUID}.img
                      → MemoryDao.insert(MemoryEntity(title, date, imagePath))
                          → Chèn 1 dòng mới vào bảng memories của SQLite
                  → MemoryViewModel._saveState.value = MemorySaveState.Success
          → AddMemoryActivity lắng nghe viewModel.saveState
              → Thấy trạng thái là MemorySaveState.Success
              → AddMemoryActivity.closeForm() → finish()     [đóng form thêm kỷ niệm]
  → Dữ liệu trong bảng Room "memories" thay đổi
      → MemoryDao.observeAll() tự động phát ra danh sách mới
      → MemoryListFragment nhận danh sách mới và cập nhật giao diện ngay lập tức
```

### Luồng 3: Người dùng bấm Back khi đang nhập dở kỷ niệm
```text
Người dùng bấm nút Back trên thanh tiêu đề hoặc cử chỉ Back của Android
  → AddMemoryActivity.requestClose()
      → Kiểm tra hasUnsavedChanges(): (có tiêu đề HOẶC đã chọn ảnh HOẶC đã chọn ngày)
      → Nếu KHÔNG có thay đổi: closeForm() → finish() ngay lập tức
      → Nếu CÓ thay đổi dở dang:
          → Hiển thị AlertDialog (R.string.add_memory_discard_title, R.string.add_memory_discard_message)
          → Người dùng bấm "Discard": gọi closeForm() → finish() đóng màn hình
          → Người dùng bấm "Cancel": đóng Dialog, giữ nguyên dữ liệu đang nhập dở
```

### Luồng 4: Xoá một kỷ niệm (Long-press & Accessibility Action)
```text
Người dùng nhấn giữ lâu (Long-press) vào 1 thẻ kỷ niệm hoặc dùng TalkBack kích hoạt action xoá
  → MemoryAdapter.binding.root.setOnLongClickListener()
      → MemoryAdapter.onDeleteRequest(memory)
          → MemoryListFragment.confirmDelete(memory)
              → Hiển thị AlertDialog hỏi xác nhận: "Delete memory [title]?"
              → Người dùng chọn "Delete" (R.string.memory_delete_button)
                  → MemoryViewModel.delete(memory)
                      → MemoryRepository.deleteMemory(memory.id) [chạy trên Dispatchers.IO]
                          → MemoryDao.delete(memory)       [xoá dòng khỏi bảng SQLite]
                          → MemoryImageStorage.deleteImage(memory.imagePath) [xoá file ảnh vật lý trên đĩa]
  → Room Database phát ra danh sách mới không còn item vừa xoá
      → ListAdapter DiffUtil tính toán và chạy animation xoá mục đó khỏi RecyclerView
```

---

## c) Giải thích chi tiết từng bước

1. **Kiến trúc Reactive Stream từ Room Database đến UI**:
   - `MemoryDao.observeAll()` trả về một luồng `Flow<List<MemoryEntity>>`. Trong Room, mỗi khi có câu lệnh INSERT, UPDATE hoặc DELETE tác động lên bảng `memories`, Room sẽ tự động truy vấn lại và phát danh sách mới nhất vào luồng Flow này.
   - Trong `MemoryViewModel`:
     - Luồng được biến đổi qua toán tử `.map { MemoryUiState.Success(it) }`.
     - Toán tử `.onStart { emit(MemoryUiState.Loading) }` đảm bảo giao diện hiển thị vòng quay tải dữ liệu khi bắt đầu nạp.
     - Toán tử `.catch { emit(MemoryUiState.Error(...)) }` bắt các ngoại lệ cơ sở dữ liệu nếu có và chuyển sang trạng thái lỗi.
     - Sử dụng biến kích hoạt `retryTrigger = MutableStateFlow(0)` kết hợp `flatMapLatest`: khi gặp lỗi kết nối hoặc đọc đĩa, người dùng bấm nút "Retry", hàm `viewModel.retry()` sẽ tăng giá trị `retryTrigger.value++`, khiến toàn bộ chuỗi quan sát được khởi động lại một cách thanh lịch.
     - Hàm `.stateIn(...)` biến luồng thành `StateFlow` với chế độ `SharingStarted.WhileSubscribed(5000)`. Khi người dùng tạm thời xoay màn hình hoặc chuyển tab dưới 5 giây, kết nối với Room vẫn được duy trì, không bị ngắt quãng vô ích.

2. **Cơ chế chuyển đổi vị trí nút "+" linh hoạt (`HEADER_ADD_THRESHOLD = 5`)**:
   - Để mang lại trải nghiệm người dùng tốt nhất, ứng dụng áp dụng kỹ thuật hiển thị nút "+" thông minh dựa trên số lượng kỷ niệm hiện có:
     - **Trường hợp 0 kỷ niệm (`isEmpty`)**: Ẩn danh sách, hiện giao diện rỗng (`layoutEmptyState`) với một nút bấm lớn chính giữa mời gọi người dùng tạo kỷ niệm đầu tiên.
     - **Trường hợp từ 1 đến 4 kỷ niệm**: Nút "+" xuất hiện ở cuối danh sách dưới dạng một item Footer (`ItemMemoryFooterBinding`) nhờ lớp ghép nối `ConcatAdapter(memoryAdapter, footerAdapter)`. Ở số lượng ít, người dùng dễ dàng nhìn thấy nút bấm ở ngay đáy danh sách.
     - **Trường hợp từ 5 kỷ niệm trở lên**: Lúc này danh sách đã dài và người dùng phải cuộn trang. Việc để nút "+" ở tận đáy sẽ gây bất tiện. Ứng dụng tự động ẩn nút ở chân trang (`footerAdapter.setVisible(false)`) và làm xuất hiện nút "+" cố định trên thanh tiêu đề phía trên (`binding.btnAddMemoryHeader.isVisible = true`).

3. **Quy trình thêm kỷ niệm tại `AddMemoryActivity`**:
   - **Xử lý ảnh và tối ưu bộ nhớ**:
     - Khi người dùng chọn ảnh qua `ActivityResultContracts.PickVisualMedia()`, Activity yêu cầu cấp quyền đọc bền vững: `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)`.
     - Ảnh xem trước (Preview) được giải mã bất đồng bộ qua hàm `decodeMemoryBitmap(contentResolver, uri)`. Hàm này kiểm tra phiên bản Android (từ API 28 trở lên dùng `ImageDecoder`, cũ hơn dùng `BitmapFactory.Options.inSampleSize`) để thu nhỏ ảnh sao cho cạnh dài nhất không quá 1024px. Nhờ đó, ảnh chụp từ camera dung lượng hàng chục Megapixel không bao giờ gây đơ giao diện hay tràn bộ nhớ RAM (OOM).
   - **Lưu trữ nguyên tử (Atomic Persistence)**:
     - Khi người dùng bấm "Save", `MemoryRepository.addMemory` thực hiện tuần tự: (1) Sao chép file ảnh từ Uri vào thư mục nội bộ `filesDir/memories/{UUID}.img`, (2) Thêm bản ghi vào Room SQLite.
     - Nếu câu lệnh chèn vào database thất bại, khối `catch` sẽ lập tức gọi `imageStorage.deleteImage(imagePath)` để xoá file ảnh vừa sao chép, không bao giờ để lại file mồ côi (rác) trên máy người dùng.

4. **Xử lý phím Back và chống mất dữ liệu (Discard Dialog)**:
   - Trong `AddMemoryActivity`, hàm `hasUnsavedChanges()` kiểm tra:
     `binding.etMemoryTitle.text.isNotBlank() || selectedImageUri != null || selectedDate != null`
   - Nếu người dùng đã nhập bất kỳ thông tin nào mà vô tình bấm nút Back (của Activity hoặc cử chỉ vuốt của hệ thống), `onBackPressedDispatcher.addCallback` sẽ chặn lại và hiển thị một `AlertDialog` cảnh báo: *"Discard changes? Any unsaved changes will be lost."* Người dùng phải bấm xác nhận huỷ ("Discard") thì form mới đóng lại.

5. **Thao tác xoá an toàn và hỗ trợ người khiếm thị (Accessibility)**:
   - Trong `MemoryAdapter.kt`, mỗi item được gán sự kiện nhấn giữ lâu:
     `binding.root.setOnLongClickListener { onDeleteRequest(memory); true }`
   - **Hỗ trợ TalkBack**: Người dùng bật chế độ trợ năng TalkBack trên Android không thể thực hiện thao tác nhấn giữ bình thường. Do đó, adapter tích hợp thêm một hành động ngữ cảnh tùy chỉnh:
     ```kotlin
     ViewCompat.addAccessibilityAction(
         binding.root,
         binding.root.context.getString(R.string.memory_delete_button)
     ) { _, _ -> onDeleteRequest(memory); true }
     ```
   - Khi được kích hoạt, `MemoryListFragment.confirmDelete()` hiển thị hộp thoại xác nhận tên kỷ niệm cần xoá. Khi xác nhận, `viewModel.delete(memory)` chạy trên background thread, vừa xoá dữ liệu trong Room Database vừa xoá file ảnh tương ứng trong thư mục `filesDir/memories`.

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`MemoryViewModel`**:
  - Được khởi tạo ở phạm vi Activity bằng `activityViewModels()` bên trong `MemoryListFragment`. Nhờ vậy, khi người dùng chuyển qua lại giữa các tab (Home, Love Test, Memory), `MemoryViewModel` không bị tạo lại, danh sách kỷ niệm được giữ nguyên vẹn trong bộ nhớ đệm.
- **`MemoryListFragment`**:
  - `onResume()`: Luôn đặt lại `openingAddMemory = false` và bật lại khả năng bấm cho nút thêm (+), ngăn chặn việc mở đúp màn hình.
  - `onDestroyView()`: Huỷ liên kết `binding.rvMemories.adapter = null` để chống rò rỉ bộ nhớ View của RecyclerView.
- **`AddMemoryActivity`**:
  - `onSaveInstanceState()`: Lưu giữ chuỗi Uri của ảnh đã chọn (`KEY_IMAGE`) và ngày đã chọn (`KEY_DATE`). Nếu người dùng xoay ngang điện thoại khi đang nhập dữ liệu, ảnh preview và ngày vẫn được hiển thị nguyên vẹn.
  - `onDestroy()`: Huỷ coroutine `previewJob?.cancel()` và đóng hộp thoại `discardDialog?.dismiss()` để tránh crash lỗi rò rỉ cửa sổ (Activity window leak).

---

## e) Đoạn code then chốt

### Đoạn 1: Quản lý luồng dữ liệu Room với cơ chế Retry tại `MemoryViewModel`
```kotlin
// MemoryViewModel.kt
val uiState: StateFlow<MemoryUiState> = retryTrigger.flatMapLatest {
    repository.observeMemories()
        .map<List<MemoryEntity>, MemoryUiState> { memories -> MemoryUiState.Success(memories) }
        .onStart { emit(MemoryUiState.Loading) }
        .catch { exception ->
            Log.e("MemoryViewModel", "Load memories failed", exception)
            emit(MemoryUiState.Error(R.string.memory_error_load))
        }
}.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = MemoryUiState.Loading)
```
*Giải thích*: Biến đổi luồng dữ liệu reactive từ Room thành `StateFlow` chứa 3 trạng thái rõ ràng (Loading, Success, Error), duy trì 5 giây khi đổi cấu hình và cho phép thử lại dễ dàng bằng cách tăng `retryTrigger`.

### Đoạn 2: Điều khiển vị trí nút thêm (+) theo số lượng tại `MemoryListFragment.render()`
```kotlin
// MemoryListFragment.kt
is MemoryUiState.Success -> {
    val isEmpty = state.memories.isEmpty()
    val count = state.memories.size
    binding.layoutEmptyState.isVisible = isEmpty
    binding.rvMemories.isVisible = !isEmpty
    memoryAdapter.submitList(state.memories) {
        footerAdapter.setVisible(count in 1 until HEADER_ADD_THRESHOLD)
        binding.btnAddMemoryHeader.isVisible = count >= HEADER_ADD_THRESHOLD
    }
}
```
*Giải thích*: Tự động hiển thị nút thêm ở vị trí tối ưu: ở giữa màn hình khi rỗng, ở chân trang khi có 1–4 mục, và đưa lên thanh tiêu đề khi có từ 5 mục trở lên.

### Đoạn 3: Kết hợp Long-press và Trợ năng TalkBack tại `MemoryAdapter.MemoryViewHolder.bind()`
```kotlin
// MemoryAdapter.kt
binding.root.setOnLongClickListener {
    onDeleteRequest(memory)
    true
}
deleteActionId = ViewCompat.addAccessibilityAction(
    binding.root,
    binding.root.context.getString(R.string.memory_delete_button)
) { _, _ ->
    onDeleteRequest(memory)
    true
}
```
*Giải thích*: Cho phép người dùng bình thường nhấn giữ để xoá, đồng thời đăng ký hành động trợ năng riêng để người dùng khiếm thị dùng TalkBack vẫn có thể gọi lệnh xoá một cách thuận tiện.

---

## f) Tự kiểm tra (Self-Check)

1. Tại sao trong `MemoryListFragment`, `MemoryViewModel` lại được khởi tạo bằng `by activityViewModels()` thay vì `by viewModels()`?
2. Trong `MemoryRepository.addMemory()`, nếu quá trình chèn dữ liệu vào Room Database (`dao.insert`) bị lỗi thì khối `catch` thực hiện điều gì để bảo vệ bộ nhớ thiết bị?
3. Khi danh sách kỷ niệm đạt từ 5 mục trở lên, nút thêm kỷ niệm ở cuối danh sách (Footer) sẽ biến mất và thay thế bằng nút nào trên giao diện?

---

# Phần 7: Cài đặt và Đa ngôn ngữ (SettingsActivity, LanguageActivity, AppCompatDelegate)

## a) Đọc file theo thứ tự
1. `ui/setting/SettingsActivity.kt`: Màn hình Cài đặt, điều hướng người dùng sang `LanguageActivity` kèm cờ `fromSettings = true`.
2. `ui/language/LanguageActivity.kt`: Màn hình danh sách ngôn ngữ, phân nhánh xử lý giữa việc mở lần đầu (Onboarding) và mở từ màn hình Cài đặt.
3. `ui/language/LanguageViewModel.kt`: ViewModel lưu mã ngôn ngữ đang được người dùng bấm chọn (`selectedId: StateFlow<String>`), lưu giữ an toàn qua `SavedStateHandle`.
4. `ui/language/LanguageAdapter.kt`: `RecyclerView.Adapter` hiển thị danh sách 7 ngôn ngữ hỗ trợ kèm cờ quốc gia, cập nhật vị trí tick highlight.
5. `ui/language/Language.kt`: Data class biểu diễn thông tin một ngôn ngữ gồm mã định danh `id`, tài nguyên tên `nameRes` và cờ quốc gia `flagRes`.
6. `ui/language/SupportedLanguages.kt`: Singleton quản lý danh sách 7 ngôn ngữ hỗ trợ, phát hiện ngôn ngữ đang hiển thị (`current()`), và áp dụng ngôn ngữ toàn ứng dụng qua `AppCompatDelegate.setApplicationLocales()`.

---

## b) Sơ đồ chuỗi gọi (Call Chain)

### Luồng 1: Mở LanguageActivity từ SettingsActivity và đổi ngôn ngữ
```text
Người dùng ở SettingsActivity chạm vào mục "Language" (layoutLanguage)
  → SettingsActivity.binding.layoutLanguage.setOnClickListener()
      → startActivity(LanguageActivity.newIntent(this, fromSettings = true))
  → LanguageActivity.onCreate()
      → setupHeader()
          → fromSettings == true: nút btnBack hiển thị (binding.btnBack.isVisible = true)
      → setupLanguageList()
          → Lấy danh sách 7 ngôn ngữ từ SupportedLanguages.all
          → Nạp vào LanguageAdapter với selectedId lấy từ viewModel.selectedId.value
  → Người dùng chạm chọn một ngôn ngữ mới (ví dụ Tiếng Pháp "fr")
      → LanguageAdapter.onClick(language)
      → LanguageViewModel.select(language)
          → savedStateHandle[KEY_SELECTED_ID] = "fr"
          → viewModel.selectedId phát ra giá trị "fr"
          → LanguageAdapter nhận giá trị mới qua collectOn(this) → dời icon tick xanh sang dòng Tiếng Pháp
  → Người dùng chạm vào nút xác nhận tick ở góc trên bên phải (btnConfirmLanguage)
      → LanguageActivity.binding.btnConfirmLanguage.setOnClickListener()
          → Phát hiện fromSettings == true:
              → LanguageActivity.finish()                   [đóng LanguageActivity trước]
              → SupportedLanguages.apply(selectedLanguage)
                  → AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
  → Hệ thống Android ghi nhận thay đổi cấu hình ngôn ngữ (Configuration Change - LocaleList)
      → Mọi Activity đang mở trong stack (SettingsActivity, HomeActivity) tự động bị huỷ và tạo lại (Recreate)
      → Khi SettingsActivity được tạo lại:
          → Tự động nạp tài nguyên chuỗi từ thư mục res/values-fr/strings.xml
          → Giao diện hiển thị hoàn toàn bằng Tiếng Pháp
```

### Luồng 2: Mở LanguageActivity lần đầu tiên trong luồng Onboarding
```text
Người dùng mở app lần đầu, sau khi SplashActivity kết thúc
  → SplashActivity.navigateIfReady()
      → startActivity(Intent(SplashActivity, LanguageActivity))
  → LanguageActivity.onCreate()
      → fromSettings == false: nút btnBack bị ẩn (binding.btnBack.isVisible = false)
  → Người dùng chọn ngôn ngữ mong muốn và bấm nút tick xác nhận (btnConfirmLanguage)
      → LanguageActivity.binding.btnConfirmLanguage.setOnClickListener()
          → Phát hiện fromSettings == false:
              → LoveSettingsRepository.setOnboardingStep(OnboardingStep.INTRO) [lưu bước tiếp theo vào DataStore]
              → SupportedLanguages.apply(selectedLanguage)                    [áp dụng ngôn ngữ ngay trước khi rời đi]
              → startActivity(Intent(this, IntroActivity::class.java))        [mở màn hình Intro]
  → IntroActivity khởi tạo với Locale mới vừa được áp dụng
```

---

## c) Giải thích chi tiết từng bước

1. **Các mục trong `SettingsActivity`**:
   - `btnBack`: Gọi `finish()` để quay trở lại tab Home (`HomeFragment`).
   - `layoutLanguage`: Khởi chạy màn hình chọn ngôn ngữ thông qua `LanguageActivity.newIntent(this, fromSettings = true)`.
   - Các dòng khác trên giao diện (`layoutRate`, `layoutShare`, `layoutFeedback`, `layoutPrivacyPolicy`): Là các hàng giao diện chuẩn bị sẵn cho việc đánh giá ứng dụng trên Play Store, chia sẻ link app, gửi email góp ý và xem chính sách quyền riêng tư.

2. **Hai chế độ hoạt động của `LanguageActivity`**:
   - Ứng dụng tái sử dụng duy nhất một `LanguageActivity` cho cả 2 tình huống nhờ cờ `EXTRA_FROM_SETTINGS`:
     - **Chế độ Onboarding (`fromSettings = false`)**: Mở khi người dùng cài app lần đầu. Nút quay lại (`btnBack`) bị ẩn đi vì người dùng bắt buộc phải chọn một ngôn ngữ để tiếp tục. Khi xác nhận, ứng dụng lưu `OnboardingStep.INTRO` vào DataStore và khởi chạy `IntroActivity`.
     - **Chế độ Cài đặt (`fromSettings = true`)**: Mở từ màn hình Settings. Nút quay lại (`btnBack`) được hiển thị rõ ràng để người dùng có thể huỷ bỏ nếu không muốn đổi.

3. **Cơ chế lưu trữ và áp dụng ngôn ngữ qua `AppCompatDelegate`**:
   - Trong `SupportedLanguages.kt`, hàm `apply(language)` gọi:
     ```kotlin
     AppCompatDelegate.setApplicationLocales(
         LocaleListCompat.forLanguageTags(language.id)
     )
     ```
   - **Cơ chế hoạt động**:
     - Kể từ Android 13 (API 33), Google giới thiệu tính năng chọn ngôn ngữ riêng cho từng ứng dụng (**Per-app Language Preferences**). `AppCompatDelegate.setApplicationLocales` tự động uỷ quyền cho `android.app.LocaleManager` lưu trữ cấu hình này vào hệ thống của thiết bị (trong Settings của máy cũng sẽ thấy ngôn ngữ đã chọn cho ứng dụng này).
     - Trên các phiên bản Android cũ hơn (Android 12 trở xuống), thư viện `androidx.appcompat` tự động lưu trữ cấu hình này vào file lưu trữ nội bộ riêng của nó và tự động áp dụng lại mỗi khi ứng dụng khởi động.

4. **Tại sao Activity bị tạo lại (Activity Recreation)?**:
   - Việc thay đổi Locale toàn ứng dụng là một sự kiện thay đổi cấu hình môi trường (**Configuration Change**) lớn, tương tự như khi xoay màn hình điện thoại hoặc chuyển đổi giao diện Dark/Light Mode.
   - Để các văn bản trên giao diện chuyển sang ngôn ngữ mới, Android bắt buộc phải huỷ (`onDestroy`) và tạo lại (`onCreate`) các Activity trong ngăn xếp để các lời gọi `getString(R.string...)` và các thuộc tính `android:text="@string/..."` trong XML nạp lại tài nguyên từ thư mục tương ứng (ví dụ: `values-vi/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml`).
   - **Kỹ thuật đóng màn trước, áp dụng ngôn ngữ sau**:
     Trong `LanguageActivity.setupConfirmButton()`:
     ```kotlin
     if (fromSettings) {
         finish()
         SupportedLanguages.apply(selectedLanguage)
         return@setOnClickListener
     }
     ```
     Bằng cách gọi `finish()` trước, `LanguageActivity` sẽ đóng lại ngay lập tức mà không phải chịu ảnh hưởng của quá trình tái tạo. Khi lệnh `apply()` thực thi, chỉ có màn hình `SettingsActivity` bên dưới được tái tạo lại với ngôn ngữ mới, giúp giao diện chuyển đổi mượt mà, không bị hiện tượng giật màn hình (flicker).

---

## d) Vòng đời và Quản lý trạng thái (Lifecycle & State)

- **`LanguageViewModel`**:
  - `selectedId` được bọc bên trong `SavedStateHandle`:
    `val selectedId: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SELECTED_ID, SupportedLanguages.current().id)`
  - Khi người dùng chạm vào một ngôn ngữ nhưng chưa bấm tick xác nhận, nếu họ xoay ngang màn hình (Configuration Change), `SavedStateHandle` sẽ khôi phục lại chính xác mục vừa được chọn mà không bị trả về ngôn ngữ mặc định.
- **`LanguageActivity`**:
  - `setupHeader()`: Ẩn/hiện nút Back dựa trên `fromSettings`.
  - `setupLanguageList()`: Dùng extension `viewModel.selectedId.collectOn(this)` để lắng nghe và đổi vị trí icon tick xanh trên RecyclerView tức thì.

---

## e) Đoạn code then chốt

### Đoạn 1: Áp dụng ngôn ngữ cấp hệ thống tại `SupportedLanguages.apply()`
```kotlin
// SupportedLanguages.kt
fun apply(language: Language) {
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(language.id)
    )
}
```
*Giải thích*: Tận dụng API chuẩn của AndroidX để thiết lập Locale cho toàn ứng dụng, tự động tương thích với Per-app Language của Android 13+ và lưu trữ tự động trên Android cũ.

### Đoạn 2: Phân nhánh xử lý Onboarding và Settings tại `LanguageActivity.setupConfirmButton()`
```kotlin
// LanguageActivity.kt
if (fromSettings) {
    finish()
    SupportedLanguages.apply(selectedLanguage)
    return@setOnClickListener
}
lifecycleScope.launch {
    repository.setOnboardingStep(OnboardingStep.INTRO)
    SupportedLanguages.apply(selectedLanguage)
    startActivity(Intent(this@LanguageActivity, IntroActivity::class.java))
}
```
*Giải thích*: Nếu mở từ Settings thì đóng Activity trước rồi áp dụng ngôn ngữ để SettingsActivity tạo lại mượt mà; nếu là Onboarding thì lưu bước `INTRO` vào DataStore rồi mới chuyển sang `IntroActivity`.

### Đoạn 3: Lưu trạng thái highlight bằng `SavedStateHandle` tại `LanguageViewModel`
```kotlin
// LanguageViewModel.kt
class LanguageViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val selectedId: StateFlow<String> = savedStateHandle.getStateFlow(
        KEY_SELECTED_ID, SupportedLanguages.current().id
    )
    fun select(language: Language) {
        savedStateHandle[KEY_SELECTED_ID] = language.id
    }
}
```
*Giải thích*: Lưu trữ ID ngôn ngữ đang highlight vào `SavedStateHandle`, giúp giữ nguyên mục đã tick ngay cả khi người dùng xoay màn hình điện thoại.

---

## f) Tự kiểm tra (Self-Check)

1. Tại sao khi mở `LanguageActivity` từ `SettingsActivity`, trong sự kiện click của nút xác nhận lại gọi `finish()` trước khi gọi `SupportedLanguages.apply(selectedLanguage)`?
2. Tại sao khi gọi `AppCompatDelegate.setApplicationLocales()` thì các Activity đang hiển thị lại bị huỷ và khởi tạo lại (`recreate`)?
3. Trong `LanguageViewModel`, nhờ vào lớp nào mà giá trị `selectedId` có thể sống sót qua cả quá trình xoay màn hình lẫn trường hợp Activity bị hệ thống thu hồi bộ nhớ (Process Death)?







