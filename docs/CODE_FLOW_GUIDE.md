# Hướng Dẫn Toàn Diện Luồng Code LoveTester (Code Flow Guide)

Tài liệu này được biên soạn dành cho lập trình viên mới học Android, bám sát 100% mã nguồn thực tế của dự án LoveTester (sử dụng Kotlin, ViewBinding, Hilt, Room, DataStore Preferences, Coroutines/Flow, Coil).

---

## Tiến độ biên soạn

- [x] **Phần 0**: Tổng quan kiến trúc, Hilt DI, cây màn hình, bản đồ thư mục và thứ tự đọc
- [ ] **Phần 1**: Khởi động và Onboarding (Splash → Language → Intro → Permission → Home)
- [ ] **Phần 2**: Home – Vỏ 3 tab (HomeActivity, FragmentManager, show/hide, xoay màn hình)
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
