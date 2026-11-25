# Báo cáo tối ưu code – LoveTester

Tài liệu tổng hợp các thay đổi tối ưu đã làm, để đọc lại và học. Code ví dụ đã rút gọn.

> Ghi chú về tên đợt: trong git không có commit tên "Giai đoạn 1 / 1b / 2". Các thay đổi lớn trước
> khi tối ưu nằm trong `62ff695 implement update` (ViewModel cho Language/Horoscope, testType ở
> Result…). Các đợt tối ưu thật sự là `Đợt 1` → `Sửa lint` dưới đây.

## 1. Tổng quan

| Nhóm | Commit | Tóm tắt |
|---|---|---|
| Đợt 1 | `83ed320` | Theme/styles, `safeNavigate`, chuỗi còn viết cứng, giới tính, lint đầu tiên |
| Đợt 2 | `bc0cf29` | Edge-to-edge cho Language/Intro/Permission/Settings, nút back 48dp, ripple |
| Đợt 3 | `58ae7cf` | Coil + thu nhỏ ảnh, `showOnce`, Memory loading/error/retry, bàn phím, TalkBack |
| Đợt 4 | `46f3548` | Style dùng chung cho nút/tiêu đề/sheet, thang chữ, màu tương phản |
| Sửa nhỏ | `5bf5076`, `d784e3f` | Log lỗi khi lưu avatar, chỉnh `ImageDownscaler` |
| Dịch | `001cdb2` | 7 câu horoscope đầy đủ ở 7 ngôn ngữ |
| Đợt 3–4 hoàn tất | `2014a11` | Hilt cho mọi repository/storage, `dimens.xml` theo vai trò, bottom sheet không cao cứng |
| Đợt 5 + Đợt riêng | `ed1f9fd` | Xoá mã chết, viewBinding delegate, `collectOn`/`toast`, anim, Back/Retry, xác nhận huỷ |
| Lint | `14cefa1` | ContentDescription, RTL, Autofill, toml, xoá tài nguyên thừa (84 → 61 warning) |

---

## 2. Các nhóm thay đổi

### 2.1 Chuỗi & đa ngôn ngữ
- **Trước:** chữ viết cứng trong Kotlin, ghi đè cả tiêu đề đã dịch; 7 câu horoscope chỉ có "…".
  ```kotlin
  binding.btnSaveMemory.text = "Saving…"
  title.setSpan(ForegroundColorSpan(pink), 5, title.length, 0) // vị trí 5 chỉ đúng với tiếng Anh
  ```
- **Sau:** mọi chữ nằm trong `strings.xml` (7 ngôn ngữ); tô màu từ cuối cùng theo chuỗi đã dịch.
  ```kotlin
  binding.btnSaveMemory.setText(if (saving) R.string.common_saving else R.string.common_save)
  val accentStart = title.lastIndexOf(' ') + 1   // đúng với mọi ngôn ngữ
  ```
- **Tại sao:** đổi ngôn ngữ là đổi hết chữ, không phải sửa code. Vị trí cố định như `5` sẽ tô sai
  khi tiêu đề là "Liebestest" hay "Test d'amour".
- **File:** `values*/strings.xml`, `LoveTestFragment.kt`, `AddMemoryFragment.kt`, `GenderLabel.kt`.

### 2.2 Màu, style & dimens
- **Trước:** mỗi layout tự ghi `16sp`, `8dp`, `#FF0845`; Kotlin dùng `Color.parseColor("#454954")`.
- **Sau:** thang trong `dimens.xml` đặt tên theo vai trò; style dùng chung; màu lấy từ resource.
  ```xml
  <dimen name="text_screen_title">20sp</dimen>
  <style name="Widget.LoveTester.Button.Filled"> … <item name="cornerRadius">@dimen/radius_button</item>
  ```
  ```kotlin
  val unselected = getColor(R.color.tab_inactive)
  ```
- **Tại sao:** đổi một chỗ là cả app đổi theo, và không thể lệch giữa các màn. Theme ép sáng
  (`Theme.Material3.Light.NoActionBar` + `forceDarkAllowed=false`) vì thiết kế chỉ có giao diện sáng.
- **File:** `values/dimens.xml`, `styles.xml`, `themes.xml`, `colors.xml`.

### 2.3 Hilt (DI)
- **Trước:** mỗi màn tự tạo đối tượng, nên có nhiều bản sao cùng lúc.
  ```kotlin
  val repository = LoveSettingsRepository(applicationContext)
  ```
- **Sau:** Hilt cấp một bản duy nhất (`@Singleton`).
  ```kotlin
  @AndroidEntryPoint class HomeActivity : AppCompatActivity() {
      @Inject lateinit var repository: LoveSettingsRepository
  ```
- **Tại sao:** DataStore chỉ được có **một** instance cho mỗi file. Sau này viết test có thể thay
  repository giả dễ dàng. Lưu ý: Fragment có `@AndroidEntryPoint` thì Activity chứa nó cũng phải có.
- **File:** `AvatarStorage`, `BackgroundImageStorage`, `MemoryImageStorage`, Home/Intro/Permission/Splash,
  `StartDateFragment`, `InformationBottomSheet`, `BackgroundBottomSheet`.

### 2.4 Điều hướng & nút back
- **Trước:** `navigate()` gọi 2 lần (bấm nhanh) làm crash; back ở Horoscope dùng dispatcher của Activity.
  ```kotlin
  requireActivity().onBackPressedDispatcher.onBackPressed()
  ```
- **Sau:**
  ```kotlin
  fun NavController.safeNavigate(@IdRes actionId: Int, args: Bundle? = null) // bỏ qua nếu action không thuộc màn hiện tại
  binding.btnBack.setOnClickListener { findNavController().navigateUp() }
  // Result: Back về menu Love Test, Retry về form
  findNavController().popBackStack(R.id.loveTestFragment, false)
  ```
- **Tại sao:** bấm 2 lần thì lần thứ hai chạy khi đã sang màn khác, action không còn và app crash.
  `navigateUp()` chỉ lùi trong NavGraph, không đóng nhầm Activity.
- **File:** `NavExtensions.kt`, `HoroscopeTestFragment`, `NameTestFragment`, `LoveTestResultFragment`.

### 2.5 Edge-to-edge & insets
- **Trước:** từ targetSdk 35 app vẽ tràn dưới thanh trạng thái, nên tiêu đề và nút bị che.
- **Sau:**
  ```kotlin
  enableLightEdgeToEdge()               // thanh hệ thống trong suốt, icon tối
  binding.root.applySystemBarsPadding() // cộng insets vào padding XML
  ```
- **Tại sao:** hai extension thay cho đoạn code insets copy ở từng màn, và icon trạng thái vẫn đọc được trên nền sáng.
- **File:** `InsetsExtensions.kt`, Language/Intro/Permission/Settings.

### 2.6 Bottom sheet & dialog
- **Trước:** bấm nhanh 2 lần mở 2 sheet; sheet có `minHeight="570dp"` cứng nên tràn trên màn nhỏ.
- **Sau:**
  ```kotlin
  supportFragmentManager.showOnce(BackgroundBottomSheet(), SHEET_TAG) // showNow + kiểm tra tag
  expandWithMaxHeight(R.dimen.sheet_max_height_horoscope)             // BottomSheetBehavior.maxHeight
  ```
- **Tại sao:** `showNow` đăng ký fragment ngay lập tức nên lần bấm thứ hai thấy sheet đã có. Layout
  `wrap_content`, chiều cao tối đa do Behavior quyết định, nên lưới cuộn bên trong thay vì bị cắt.
- **File:** `FragmentExtensions.kt`, 3 layout `bottom_sheet_*.xml`, `HoroscopePickerBottomSheet`.

### 2.7 Bàn phím
- **Trước:** bàn phím che ô nhập; bấm "Done" không đóng bàn phím.
- **Sau:** `imeOptions` (Next/Done) + `NestedScrollView` + `adjustResize` + extension:
  ```kotlin
  binding.etPartnerName.onImeDone()   // Done → ẩn bàn phím
  it.hideKeyboard()
  ```
- **File:** `KeyboardExtensions.kt`, `fragment_name_test.xml`, `fragment_add_memory.xml`, manifest.

### 2.8 Ảnh (Coil, thu nhỏ)
- **Trước:** decode bitmap trên main thread; lưu ảnh gốc vài MB.
- **Sau:**
  ```kotlin
  binding.ivMemoryPhoto.load(File(memory.imagePath))          // Coil: luồng nền, đúng kích thước view
  ImageDownscaler.saveDownscaled(context, uri, destination)   // lưu ≤ 1440px
  ```
- **Tại sao:** tránh giật/ANR và OutOfMemory. Coil tự huỷ request khi item RecyclerView bị tái sử dụng.
- **File:** `ImageDownscaler.kt`, `AvatarStorage`, `BackgroundImageStorage`, `MemoryAdapter`, `BackgroundAdapter`.

### 2.9 Extension dùng chung
| Extension | Thay cho |
|---|---|
| `by viewBinding(X::bind)` | `_binding` + `_binding = null` trong `onDestroyView` (13 Fragment/Sheet/Dialog) |
| `flow.collectOn(viewLifecycleOwner) { … }` | `lifecycleScope.launch { repeatOnLifecycle(STARTED) { collect } }` |
| `context.toast(R.string.x)` | `Toast.makeText(...).show()` lặp ở 7 chỗ |
| `GridSpacingDecoration(spacing, spanCount)` | `BackgroundGridSpacing` chỉ dùng được cho 2 cột |
| `setScreenAnimations()` | ghi lại 4 anim ở mỗi transaction |
- **Tại sao:** ít code lặp thì ít chỗ quên. Ví dụ, quên `_binding = null` gây rò rỉ bộ nhớ.
- **File:** `ui/common/*.kt`.

### 2.10 Giao diện & animation
- Một bộ anim nhẹ (mờ dần + trượt dọc 3%) dùng chung cho `nav_love_test.xml` và transaction ở Home.
  Chọn trượt **dọc** để không bị ngược chiều trong ngôn ngữ RTL. Activity giữ anim mặc định.
- `BackgroundAdapter` theo mẫu `LanguageAdapter`: `root.isSelected` + selector, chỉ vẽ lại ô cũ và ô mới.
- AddMemory: bấm back khi còn tiêu đề/ảnh/ngày chưa lưu thì hỏi "Discard this memory?".
- **File:** `res/anim/screen_*.xml`, `BackgroundAdapter`, `AddMemoryFragment`.

### 2.11 Accessibility
- Nút back 48dp có ripple; ảnh trang trí `contentDescription="@null"`.
- Memory: long-press để xoá, và thêm **custom action "Delete"** cho TalkBack (người dùng TalkBack không long-press được):
  ```kotlin
  ViewCompat.addAccessibilityAction(binding.root, getString(R.string.memory_delete_button)) { _, _ -> onDeleteRequest(memory); true }
  ```
- **File:** `MemoryAdapter.kt`, `activity_home.xml`, các layout có nút back.

### 2.12 Lint
| Loại | Cách sửa |
|---|---|
| NotifyDataSetChanged | 2 adapter lịch → `ListAdapter` + `DiffUtil` |
| SetTextI18n | `String.format(Locale.getDefault(), "%d", n)` |
| RtlHardcoded | `marginLeft` → `marginStart` |
| Autofill | `importantForAutofill="no"` (ô tên còn dùng cho tên người ấy) |
| UseTomlInstead | datastore chuyển vào `libs.versions.toml` |
| UnusedResources | xoá 3 drawable và 2 màu không dùng |

Kết quả: **84 → 61 warning, 0 error**.

### 2.13 Mã chết đã xoá
`MainActivity`, `activity_main.xml`, `nav_graph.xml` (trỏ tới Fragment không tồn tại), `MemoryActivity`,
`activity_memory.xml`, `LovePreferencesDataSource` (class rỗng), `ui/test.kt`, `ui/common/test.kt`,
`values-night/themes.xml`, `BackgroundGridSpacing`, `bg_date_cancel/save`, `bg_information_sheet`.

---

## 3. Lỗi thật đã được sửa

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| Bấm 2 lần nút chuyển màn gây crash | `navigate()` lần 2 chạy khi đã sang màn khác | `safeNavigate` |
| Bấm 2 lần mở 2 bottom sheet / 2 StartDate | `show()` bất đồng bộ | `showOnce` (`showNow`), kiểm tra `findFragmentById` |
| Nút back ở Horoscope đóng nhầm Activity | gọi dispatcher của Activity | `navigateUp()` |
| Giới tính bị reset khi xoay màn | giá trị không được lưu, menu dựa vào text | `selectedGender` nullable + `onSaveInstanceState`, menu dùng `itemId` |
| Tiêu đề Love Test mất bản dịch | Kotlin ghi đè bằng chữ tiếng Anh | Lấy từ `strings.xml` |
| 7 câu horoscope chỉ hiện "…" | placeholder chưa thay | Điền đủ 7 ngôn ngữ |
| Kiểm tra `view != null` luôn đúng | tham số `view` của `onViewCreated` che `Fragment.view` | `this@AddMemoryFragment.view` |

> Lỗi `when(ARG_TEST_TYPE)` luôn rơi vào nhánh Name Test: **không thấy trong lịch sử git**. Từ commit
> đầu tiên (`62ff695`) code đã dùng `testType == TEST_TYPE_HOROSCOPE`, nên lỗi được sửa trước khi commit.

---

## 4. Chưa làm / để sau

- ViewModel cho Intro, Permission, Splash, StartDate, Information (hiện gọi repository trực tiếp).
- 61 warning lint còn lại: Icon*, VectorRaster, Overdraw, UseKtx, UseCompoundDrawables, phiên bản thư viện mới…
- Bản dịch (đặc biệt 7 câu horoscope có `<!-- TODO -->`) cần người bản xứ kiểm tra.
- 7 câu horoscope tiếng Anh cần đối chiếu lại với Figma.
- Bo góc lệch thang (nút avatar 16dp, `bg_rectangle_white` 20dp, hộp kết quả 10dp so với 8dp trong Figma…) chờ đối chiếu Figma.
- 51 chỗ lề `16dp` chưa đổi sang `@dimen/margin_screen_horizontal`.
- Hộp thoại huỷ ở AddMemory mất khi xoay màn (ghi chú `shortcut:` trong code).
- Chưa có test tự động; chưa chạy thử trên màn hình nhỏ.

---

## 5. Bài học rút ra

| Nguyên tắc | Ví dụ trong dự án |
|---|---|
| Chữ để hiển thị, mã để xử lý logic | Lưu `Gender.MALE`, hiển thị `getString(gender.labelRes())`; menu dùng `itemId`, không so sánh text |
| Tầng dữ liệu báo *lỗi gì*, UI chọn *câu chữ* | `MemoryUiState.Error(messageRes)`; Storage ném exception, Fragment chọn `R.string.add_memory_error_*` |
| Coroutine chạm binding thì dùng `viewLifecycleOwner` | `flow.collectOn(viewLifecycleOwner)`, không dùng `lifecycleScope` của Fragment |
| Binding sống theo view, không theo Fragment | delegate `viewBinding` tự xoá khi view bị huỷ |
| Thao tác người dùng có thể đến 2 lần | `safeNavigate`, `showOnce`, cờ `isNavigating` |
| Một nguồn sự thật cho số đo và màu | `dimens.xml`, `colors.xml`, style dùng chung |
| Một instance cho tài nguyên dùng chung | DataStore qua Hilt `@Singleton` |
| Việc nặng không làm trên main thread | Coil, `ImageDownscaler` trên `Dispatchers.IO` |
| Chạy được trên mọi kích thước màn | `wrap_content` + `maxHeight`, không `minHeight` cứng |
| Mọi người dùng đều thao tác được | custom action TalkBack, vùng chạm 48dp |
