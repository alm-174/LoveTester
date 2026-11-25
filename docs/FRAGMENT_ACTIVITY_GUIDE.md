# Hướng dẫn chuyển màn hình giữa Fragment và Activity – LoveTester

Tài liệu dùng code thật của dự án. Code trong tài liệu đã rút gọn, phần `…` là giữ nguyên như cũ.

**Quy tắc công ty**
- Bấm nút để sang **một màn mới hoàn toàn** (thường có nút ←) → **Activity**.
- Nội dung đổi **ngay trong màn đang đứng** (tab, trang ViewPager, bottom sheet, dialog) → **Fragment**.

> Bảng phân loại ở mục 2 đang **chờ người hướng dẫn xác nhận**. Xác nhận xong rồi hãy bắt đầu.

---

## 1. Khái niệm

| | Activity | Fragment |
|---|---|---|
| Là gì | Một màn hình độc lập, hệ thống quản lý | Một phần giao diện nằm **trong** Activity |
| Khai báo | Bắt buộc có trong `AndroidManifest.xml` | Không khai báo |
| Vòng đời | `onCreate → onStart → onResume … onDestroy` | Có thêm vòng đời của **view**: `onViewCreated … onDestroyView`. Fragment có thể còn sống khi view đã bị huỷ |
| View/binding | `setContentView`, binding sống bằng Activity | Binding phải bỏ khi view huỷ (dự án dùng `by viewBinding(...)`) |
| Back stack | Back stack của hệ thống (task) | Back stack của `FragmentManager` / NavController |
| Mở màn mới | `startActivity(Intent)` | `navigate(...)` hoặc `beginTransaction().replace(...)` |
| Đóng màn | `finish()` | `navigateUp()` / `popBackStack()` |
| Truyền dữ liệu vào | `Intent` extras → `intent.getXxxExtra()` | `Bundle` arguments → `requireArguments()` |
| Trả kết quả | `registerForActivityResult`, hoặc ghi vào repository | `setFragmentResult` / `setFragmentResultListener` |
| ViewModel | `by viewModels()`, sống theo Activity | `by viewModels()` sống theo Fragment; `by activityViewModels()` **dùng chung** với Activity chứa nó |
| Context | Chính nó (`this`) | `requireContext()`, `requireActivity()` |
| Coroutine chạm UI | `lifecycleScope` | `viewLifecycleOwner.lifecycleScope` |
| Mở sheet/dialog | `supportFragmentManager` | `childFragmentManager` |
| Hilt | `@AndroidEntryPoint` | `@AndroidEntryPoint`, và Activity chứa nó **cũng phải có** |

**Điều quan trọng nhất khi chuyển Fragment → Activity:** hai Activity không dùng chung ViewModel được.
Dữ liệu dùng chung phải đi qua `Intent` (dữ liệu nhỏ) hoặc repository (Room/DataStore).

---

## 2. Hiện trạng → mục tiêu

| Màn | Hiện tại | Mục tiêu | Tạo | Xoá |
|---|---|---|---|---|
| Splash | `SplashActivity` | Activity | – | – |
| Language | `LanguageActivity` (lần đầu) + `LanguageFragment` (trong Settings) | **1** Activity, phân biệt bằng extra | extra `EXTRA_FROM_SETTINGS` | `LanguageFragment.kt`, `fragment_language.xml`, `settingsFragmentContainer` |
| Intro (3 trang) | `IntroActivity` + ViewPager2 | Giữ nguyên | – | – |
| Permission | `PermissionActivity` | Giữ nguyên | – | – |
| Home | `HomeActivity` | Activity chứa 3 tab | – | – |
| Tab Home | Nội dung nằm thẳng trong `activity_home.xml` | Giữ nguyên ở bước này | – | – |
| Tab Love Test | `LoveTestActivity` + `LoveTestFragment` | **Fragment tab** trong Home | – | `LoveTestActivity.kt`, `activity_love_test.xml`, `nav_love_test.xml` |
| Tab Memory | `MemoryListFragment` trong `homeMemoryContainer` | Giữ nguyên | – | – |
| Setting | `SettingsActivity` | Giữ nguyên | – | phần chứa `LanguageFragment` |
| NameTest | `NameTestFragment` (nav_love_test) | **Activity** | `NameTestActivity.kt` | `NameTestFragment.kt` |
| HoroscopeTest | `HoroscopeTestFragment` (nav_love_test) | **Activity** | `HoroscopeTestActivity.kt` | `HoroscopeTestFragment.kt` |
| Result | `LoveTestResultFragment` (nav_love_test) | **Activity** dùng chung cho 2 test | `ResultActivity.kt` | `LoveTestResultFragment.kt` |
| StartDate | `StartDateFragment` trong `startDateContainer` | **Activity** | `StartDateActivity.kt` | `StartDateFragment.kt`, `startDateContainer` |
| AddMemory | `AddMemoryFragment` thay vào `homeMemoryContainer` | **Activity** | `AddMemoryActivity.kt` | `AddMemoryFragment.kt` |
| Background, Information, AvatarOption, DatePicker, HoroscopePicker | BottomSheet/Dialog | Giữ nguyên | – | – |

---

## 3. Hướng dẫn A: Fragment → Activity

Ví dụ chính: `NameTestFragment` → `NameTestActivity` và `LoveTestResultFragment` → `ResultActivity`.
Làm cả hai trong **cùng một lần**, vì NameTest mở Result.

### Bước A1. Đổi tên layout
- **Làm gì:** trong Android Studio, chuột phải `fragment_name_test.xml` → *Refactor > Rename* → `activity_name_test.xml`.
  Làm tương tự `fragment_love_test_result.xml` → `activity_result.xml`.
- **Tại sao:** theo quy ước, tên layout cho biết nó thuộc loại màn nào. Refactor tự đổi mọi chỗ tham chiếu,
  **và** tên lớp binding cũng đổi: `FragmentNameTestBinding` → `ActivityNameTestBinding`.
- Giữ tên cũ cũng chạy được, nhưng dễ nhầm về sau.

### Bước A2. Tạo Activity và ViewBinding
- **File mới:** `ui/lovetest/name/NameTestActivity.kt`
- **Trước** (Fragment):
  ```kotlin
  class NameTestFragment : Fragment(R.layout.fragment_name_test) {
      private val binding by viewBinding(FragmentNameTestBinding::bind)
      override fun onViewCreated(view: View, savedInstanceState: Bundle?) { … }
  ```
- **Sau** (Activity):
  ```kotlin
  class NameTestActivity : AppCompatActivity() {
      private lateinit var binding: ActivityNameTestBinding

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          enableLightEdgeToEdge()
          binding = ActivityNameTestBinding.inflate(layoutInflater)
          setContentView(binding.root)
          binding.root.applySystemBarsPadding()
          // dán nội dung onViewCreated cũ vào đây
      }
  }
  ```
- **Tại sao:** binding của Activity sống bằng Activity, nên dùng `lateinit` là đủ, không cần delegate.
  Mọi code trong `onViewCreated` chuyển vào `onCreate`, đặt **sau** `setContentView`.
- **Đổi tên trong code:** `requireContext()` → `this`; `getString(...)` giữ nguyên;
  `viewLifecycleOwner` → `this`.

### Bước A3. Khai báo trong AndroidManifest
```xml
<activity
    android:name=".ui.lovetest.name.NameTestActivity"
    android:exported="false"
    android:windowSoftInputMode="adjustResize" />
<activity
    android:name=".ui.lovetest.result.ResultActivity"
    android:exported="false" />
```
- `exported="false"`: chỉ app này được mở màn đó (an toàn hơn).
- `theme`: không cần ghi, vì kế thừa `Theme.LoveTester` từ thẻ `<application>`.
- `adjustResize`: NameTest có ô nhập, nên bàn phím không được che ô.

### Bước A4. Truyền dữ liệu: Bundle → Intent extras
- **Trước:** NameTest tự tạo Bundle với key viết tay, nên gõ sai key cũng không bị báo lỗi.
  ```kotlin
  val args = Bundle().apply { putString("personOne", yourName); putInt("percentage", percentage) }
  findNavController().safeNavigate(R.id.action_nameTest_to_result, args)
  ```
- **Sau:** key là `private` trong `ResultActivity`; bên ngoài chỉ gọi hàm tạo Intent.
  ```kotlin
  class ResultActivity : AppCompatActivity() {
      companion object {
          private const val EXTRA_PERSON_ONE = "com.example.lovetester.extra.PERSON_ONE"
          private const val EXTRA_PERSON_TWO = "com.example.lovetester.extra.PERSON_TWO"
          private const val EXTRA_PERCENTAGE = "com.example.lovetester.extra.PERCENTAGE"
          private const val EXTRA_SIGN_ONE = "com.example.lovetester.extra.SIGN_ONE"
          private const val EXTRA_SIGN_TWO = "com.example.lovetester.extra.SIGN_TWO"

          fun newNameTestIntent(context: Context, personOne: String, personTwo: String, percentage: Int) =
              Intent(context, ResultActivity::class.java)
                  .putExtra(EXTRA_PERSON_ONE, personOne)
                  .putExtra(EXTRA_PERSON_TWO, personTwo)
                  .putExtra(EXTRA_PERCENTAGE, percentage)

          fun newHoroscopeIntent(context: Context, mySign: ZodiacSign, partnerSign: ZodiacSign, percentage: Int) =
              Intent(context, ResultActivity::class.java)
                  .putExtra(EXTRA_SIGN_ONE, mySign.name)   // enum truyền bằng tên
                  .putExtra(EXTRA_SIGN_TWO, partnerSign.name)
                  .putExtra(EXTRA_PERCENTAGE, percentage)
      }
  ```
- **Tại sao:**
  - Có hai hàm riêng thì không thể truyền thiếu dữ liệu: Horoscope bắt buộc có 2 cung, Name Test bắt buộc có 2 tên.
  - Không cần `ARG_TEST_TYPE` nữa: có `EXTRA_SIGN_ONE` thì là Horoscope.
  - Key có tiền tố package để tránh trùng với key của thư viện khác.
- Ở NameTest gọi: `startActivity(ResultActivity.newNameTestIntent(this, yourName, partnerName, percentage))`.

### Bước A5. Nhận dữ liệu và xử lý khi thiếu
- **Trước:** `requireArguments().getInt(ARG_PERCENTAGE)` và `requireNotNull(args.getString(ARG_PERSON_ONE))`. Thiếu dữ liệu là crash.
- **Sau:**
  ```kotlin
  val percentage = intent.getIntExtra(EXTRA_PERCENTAGE, -1)
  val signOne = intent.getStringExtra(EXTRA_SIGN_ONE)?.let { runCatching { ZodiacSign.valueOf(it) }.getOrNull() }
  val signTwo = intent.getStringExtra(EXTRA_SIGN_TWO)?.let { runCatching { ZodiacSign.valueOf(it) }.getOrNull() }
  val isHoroscope = signOne != null && signTwo != null
  val personOne = if (isHoroscope) getString(signOne!!.nameRes) else intent.getStringExtra(EXTRA_PERSON_ONE)
  val personTwo = if (isHoroscope) getString(signTwo!!.nameRes) else intent.getStringExtra(EXTRA_PERSON_TWO)

  if (percentage !in 0..100 || personOne == null || personTwo == null) {
      finish()        // dữ liệu hỏng: đóng màn thay vì crash
      return
  }
  ```
- **Tại sao:** `getIntExtra` bắt buộc có giá trị mặc định; chọn `-1` để nhận ra được là thiếu.
  `getStringExtra` trả `null` khi thiếu. `return` ngay sau `finish()` vì `finish()` không dừng hàm `onCreate`.
- Hàm `getHoroscopeMessageRes`, `getNameTestMessageRes`, `bindZodiacIcons` giữ nguyên, chỉ chép sang.

### Bước A6. Điều hướng: safeNavigate / navigateUp → startActivity / finish
| Chỗ | Trước | Sau |
|---|---|---|
| NameTest: nút ← | `findNavController().navigateUp()` | `finish()` |
| NameTest: Love Test | `safeNavigate(R.id.action_nameTest_to_result, args)` | `startActivity(ResultActivity.newNameTestIntent(…))` |
| Result: Retry | `navigateUp()` | `finish()` (quay lại form vừa nhập) |
| Result: ← Back | `popBackStack(R.id.loveTestFragment, false)` | `navigateToTestMenu()` (xem dưới) |

Back ở Result phải đóng **cả** Result **và** NameTest để về màn chọn test. Dự án đã có sẵn mẫu này trong
`LoveTestActivity.returnToHomeTab`:
```kotlin
private fun navigateToTestMenu() {
    startActivity(Intent(this, HomeActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(HomeActivity.EXTRA_TAB, HomeActivity.TAB_LOVE_TEST)
    })
    finish()
}
```
- `CLEAR_TOP`: đóng mọi Activity nằm trên Home (NameTest, Result).
- `SINGLE_TOP`: dùng lại Home đang có (gọi `onNewIntent`), không tạo Home mới.
- `HomeActivity.onPostResume` đã đọc `EXTRA_TAB`; chỉ cần thêm `TAB_LOVE_TEST` ở Hướng dẫn B.
- **Tạm thời** (khi tab Love Test vẫn còn là `LoveTestActivity`): thay `HomeActivity::class.java` bằng
  `LoveTestActivity::class.java` và bỏ dòng `putExtra`. Viết thành **một** hàm để sau này chỉ sửa một chỗ.

### Bước A7. Chống mở 2 lần khi bấm nhanh (thay safeNavigate)
`startActivity` không bị crash khi gọi 2 lần, nhưng sẽ mở **2 màn Result chồng nhau**. Dùng cờ như
`IntroActivity` (`isNavigating`):
```kotlin
private var isNavigating = false

binding.btnLoveTest.setOnClickListener {
    if (isNavigating) return@setOnClickListener
    …
    isNavigating = true
    startActivity(ResultActivity.newNameTestIntent(this, yourName, partnerName, percentage))
}

override fun onResume() {
    super.onResume()
    isNavigating = false   // quay lại màn này thì cho bấm lại
}
```

### Bước A8. ViewModel – phạm vi thay đổi
- NameTest không có ViewModel nên không cần làm gì.
- Khi có ViewModel: `by viewModels()` trong Activity thì ViewModel sống đến khi Activity `finish()`, và vẫn sống qua xoay màn hình.
- `by activityViewModels()` (dùng chung với Activity chứa Fragment) **không còn ý nghĩa**, xem AddMemory ở mục 5.

### Bước A9. Hilt
- Thêm `@AndroidEntryPoint` khi Activity có `@Inject`, dùng `@HiltViewModel`, **hoặc** mở một Fragment/Sheet có `@AndroidEntryPoint`.
- NameTest và Result không cần. StartDate, AddMemory, Language thì **cần**.
- Thiếu annotation thì crash ngay khi mở màn (xem mục 8).

### Bước A10. Edge-to-edge, insets, bàn phím
- Dùng sẵn `enableLightEdgeToEdge()` và `binding.root.applySystemBarsPadding()` trong `InsetsExtensions.kt`.
- **Chú ý bàn phím:** khi bật edge-to-edge, `adjustResize` **không tự đẩy** nội dung lên nữa.
  Trước đây `LoveTestActivity.applyWindowInsets` tự xử lý IME:
  ```kotlin
  val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
  … maxOf(safe.bottom, ime.bottom)
  ```
  NameTestActivity cần làm như vậy. Cách gọn nhất là viết thêm một extension giống `applySystemBarsPadding`
  nhưng lấy `WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()`.
  Bỏ `ime()` thì nút "Love Test" sẽ bị bàn phím che.

### Bước A11. Animation và nút back hệ thống
- **Animation:** Activity giữ animation mặc định của hệ thống (quyết định của dự án), nên không cần làm gì.
  Bộ `R.anim.screen_*` chỉ dùng cho Fragment.
- **Back hệ thống:** mặc định gọi `finish()`. Muốn back hệ thống ở Result giống nút ← thì:
  ```kotlin
  onBackPressedDispatcher.addCallback(this) { navigateToTestMenu() }
  ```
  Không thêm thì back hệ thống ở Result sẽ giống Retry (về form). Cách nào cũng được, nhưng cần chọn và ghi lại.

### Bước A12. Bottom sheet mở từ Activity
Không áp dụng cho NameTest/Result (xem HoroscopeTest ở mục 5). Quy tắc chung:
| | Trong Fragment | Trong Activity |
|---|---|---|
| Mở sheet | `childFragmentManager.showOnce(sheet, TAG)` | `supportFragmentManager.showOnce(sheet, TAG)` |
| Nghe kết quả | `childFragmentManager.setFragmentResultListener(KEY, viewLifecycleOwner) { … }` | `supportFragmentManager.setFragmentResultListener(KEY, this) { … }` |

Sheet gửi kết quả bằng `setFragmentResult(...)`, tức là gửi vào FragmentManager **đã mở nó**. Mở bằng
FragmentManager nào thì phải nghe trên đúng FragmentManager đó.

### Bước A13. Xoá phần cũ
1. `nav_love_test.xml`: xoá `<fragment android:id="@+id/nameTestFragment">`, `loveTestResultFragment`
   và action `action_loveTest_to_nameTest`.
2. `LoveTestFragment.setupCardClicks`: đổi `safeNavigate(R.id.action_loveTest_to_nameTest)` thành
   `startActivity(Intent(requireContext(), NameTestActivity::class.java))`.
3. Xoá `NameTestFragment.kt`, `LoveTestResultFragment.kt`.
4. *Code > Optimize Imports* trên các file đã sửa.
5. Build, chạy, commit.

---

## 4. Hướng dẫn B: Activity → Fragment

Ví dụ chính: `LoveTestActivity` + `LoveTestFragment` → **tab Love Test** trong `HomeActivity`.
Làm **sau** khi NameTest, HoroscopeTest và Result đã là Activity (khi đó `nav_love_test.xml` đã trống).

### Bước B1. Hiểu hiện trạng
- `HomeActivity`: tab Love Test gọi `startActivity(Intent(this, LoveTestActivity::class.java))`.
- `LoveTestActivity`: tự vẽ thanh tab (`layoutBottomNav`), tự tô màu tab, tự xử lý insets, và chứa NavHost `loveTestNavHost`.
- `LoveTestFragment` đã là Fragment sẵn. Phần cần chuyển thật ra là **logic của LoveTestActivity** sang Home.

### Bước B2. Sửa LoveTestFragment để không còn cần NavController
- **Trước:**
  ```kotlin
  binding.cardNameTest.setOnClickListener { findNavController().safeNavigate(R.id.action_loveTest_to_nameTest) }
  ```
- **Sau:**
  ```kotlin
  binding.cardNameTest.setOnClickListener {
      startActivity(Intent(requireContext(), NameTestActivity::class.java))
  }
  binding.cardHoroscopeTest.setOnClickListener {
      startActivity(Intent(requireContext(), HoroscopeTestActivity::class.java))
  }
  ```
- **Tại sao:** trong Home không có NavHost; gọi `findNavController()` sẽ crash
  (`IllegalStateException: … does not have a NavController set`).
- Binding giữ `by viewBinding(FragmentLoveTestBinding::bind)`; `setupTitle()` giữ nguyên.
- Fragment dùng `requireContext()` thay cho `this` của Activity. Nếu cần chặn bấm 2 lần, dùng cờ `isNavigating` như Bước A7, reset trong `onResume`.

### Bước B2b. Nếu phải chuyển logic từ một Activity vào Fragment (quy tắc chung)
| Trong Activity | Trong Fragment |
|---|---|
| `onCreate` + `setContentView` | `Fragment(R.layout.x)` + `onViewCreated` |
| `lateinit var binding` | `private val binding by viewBinding(X::bind)` |
| `this` (Context) | `requireContext()`; cần Activity thì `requireActivity()` |
| `lifecycleScope` / `collectOn(this)` | `viewLifecycleOwner.lifecycleScope` / `collectOn(viewLifecycleOwner)` |
| `supportFragmentManager` | `childFragmentManager` (sheet của riêng Fragment) |
| `finish()` | `parentFragmentManager.popBackStack()` |
| `onBackPressedDispatcher.addCallback(this)` | `requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner)` |

### Bước B3. Gắn Fragment vào Home và chuyển tab
1. **Container:** dùng lại `homeMemoryContainer` (nên *Rename* thành `homeTabContainer`, vì giờ nó chứa 2 tab).
2. **Thêm hằng số:** `const val TAB_LOVE_TEST = "love_test"` cạnh `TAB_HOME`, `TAB_MEMORY` trong `HomeActivity`.
3. **Thêm hàm**, chép theo mẫu `showMemoryTab()`:
   ```kotlin
   private fun showLoveTestTab() {
       val manager = supportFragmentManager
       if (manager.isStateSaved || manager.findFragmentById(R.id.homeMemoryContainer) is LoveTestFragment) return
       manager.popBackStack("home_tab", FragmentManager.POP_BACK_STACK_INCLUSIVE) // bỏ tab cũ (nếu có)
       manager.beginTransaction()
           .setReorderingAllowed(true)
           .setScreenAnimations()
           .replace(R.id.homeMemoryContainer, LoveTestFragment())
           .addToBackStack("home_tab")
           .commit()
   }
   ```
   Đổi `"home_memory"` của tab Memory thành cùng tên `"home_tab"`, để chuyển qua lại giữa 2 tab mà back vẫn chỉ cần 1 lần là về Home.
4. **Nút tab:** thay `startActivity(LoveTestActivity…)` bằng `showLoveTestTab()`.
5. **`onPostResume`:** thêm nhánh `TAB_LOVE_TEST -> showLoveTestTab()` (Result dùng nhánh này ở Bước A6).
6. **`renderMemoryNavigation()`** (nên đổi tên thành `renderTabs()`): tính thêm `val onLoveTest = current is LoveTestFragment`, rồi
   - tô màu và `isSelected` cho `ivNavLoveTest`/`tvNavLoveTest`, như `LoveTestActivity.onCreate` đang làm;
   - nền tab bar trắng khi `onMemory || onLoveTest`;
   - các biến `showingMemory` vẫn đúng, vì chúng dựa trên `current != null`.

### Bước B4. Giữ tab khi xoay màn hình
- Không cần code thêm: `FragmentManager` tự khôi phục Fragment trong container sau khi xoay.
- **Điều cần tránh:** đừng gọi `showLoveTestTab()` hay `replace(...)` vô điều kiện trong `onCreate`,
  vì sẽ tạo thêm một bản Fragment mới đè lên bản vừa được khôi phục. Nếu cần chọn tab mặc định thì chỉ làm khi `savedInstanceState == null`.
- `renderMemoryNavigation()` được gọi trong `onCreate`. Hàm này đọc Fragment hiện có, nên màu tab đúng ngay sau khi xoay.

### Bước B5. Insets khi nằm trong Home
- Home đã cộng `statusInsets.top` vào padding của `homeMemoryContainer` (trong `applyWindowInsets`), nên LoveTestFragment **không** cần tự xử lý insets.
- Thanh tab của Home nằm đè lên đáy container, nên nội dung cuối `fragment_love_test.xml` cần có khoảng trống
  bằng chiều cao thanh tab. Kiểm tra cách `fragment_memory_list.xml` làm rồi làm giống vậy.

### Bước B6. Xoá phần cũ
1. `LoveTestActivity.kt`, `activity_love_test.xml`, khai báo `<activity …LoveTestActivity>` trong manifest.
2. `nav_love_test.xml` (lúc này đã trống).
3. Nếu không còn NavHost nào: xoá `NavExtensions.kt` (`safeNavigate`) và 2 thư viện
   `androidx-navigation-fragment-ktx`, `androidx-navigation-ui-ktx` trong `build.gradle.kts` + `libs.versions.toml`.
4. Grep `LoveTestActivity`: phải còn **0** kết quả. Build, chạy, commit.

---

## 5. Các màn còn lại – chỉ ghi điểm KHÁC

### HoroscopeTest → `HoroscopeTestActivity`
- `HoroscopeTestViewModel` giữ nguyên, khai báo `private val viewModel: HoroscopeTestViewModel by viewModels()` trong Activity.
- **Sheet chọn cung:**
  ```kotlin
  // Trước
  childFragmentManager.setFragmentResultListener(REQUEST_MY_SIGN, viewLifecycleOwner) { … }
  childFragmentManager.showOnce(HoroscopePickerBottomSheet.newInstance(…), HoroscopePickerBottomSheet.TAG)
  // Sau
  supportFragmentManager.setFragmentResultListener(REQUEST_MY_SIGN, this) { … }
  supportFragmentManager.showOnce(HoroscopePickerBottomSheet.newInstance(…), HoroscopePickerBottomSheet.TAG)
  ```
  Đăng ký listener trong `onCreate`, để sau khi xoay vẫn nhận được kết quả.
- `collectOn(viewLifecycleOwner)` → `collectOn(this)`.
- Nút Love Test: `startActivity(ResultActivity.newHoroscopeIntent(this, state.mySign, state.partnerSign, percentage))`.
- Không có ô nhập, nên không cần `adjustResize` và IME insets.

### StartDate → `StartDateActivity` (`@AndroidEntryPoint`, cần `@Inject repository`)
- **Không cần trả kết quả về Home:** màn này đã lưu bằng `repository.setLoveStartDate(date)`, và Home đang
  `collectOn` `repository.loveStartDate`, nên tự cập nhật. Sau khi lưu chỉ cần `finish()` (thay `popBackStack()`).
- Bỏ `applyWindowInsets()` và `setStatusBarDarkIcons()` tự viết, dùng `enableLightEdgeToEdge()` + `applySystemBarsPadding()`.
- Date picker: `childFragmentManager` → `supportFragmentManager` (cả `showOnce` lẫn `setFragmentResultListener`).
- `onSaveInstanceState(outState)` giữ `pending_date` y như cũ (Activity cũng có hàm này). Đọc lại trong `onCreate(savedInstanceState)`.
- **Home:** `openStartDate()` → `startActivity(Intent(this, StartDateActivity::class.java))`, kèm cờ chống bấm 2 lần.
  Xoá `startDateContainer` trong `activity_home.xml` và 2 dòng `startDateContainer.isVisible` trong `renderMemoryNavigation()`.

### AddMemory → `AddMemoryActivity` (`@AndroidEntryPoint`)
- **ViewModel (điểm khác lớn nhất):** đang dùng `by activityViewModels()` để **dùng chung** `MemoryViewModel` với `MemoryListFragment`.
  Hai Activity không dùng chung được, nên AddMemoryActivity dùng `by viewModels()` và có **bản riêng**.
  Danh sách vẫn tự cập nhật, vì MemoryList đọc Room qua Flow: lưu xong thì Flow phát danh sách mới.
- `viewModel.clearSaveResult()` vẫn gọi trước khi đóng (để lần mở sau bắt đầu từ `Idle`).
- **Back:** `requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { requestClose() }` → `onBackPressedDispatcher.addCallback(this) { requestClose() }`.
- **`closeForm()`:** `manager.popBackStackImmediate()` → `finish()`. Phần thu hồi quyền ảnh (`releasePersistableUriPermission`) **giữ nguyên**.
- `this@AddMemoryFragment.view != null` → không cần kiểm tra nữa (Activity còn sống thì binding còn).
- Date picker: `childFragmentManager` → `supportFragmentManager` (như StartDate).
- Cần `adjustResize` + IME insets (có ô tiêu đề), xem Bước A10.
- **MemoryListFragment:** `openAddMemory()` → `startActivity(Intent(requireContext(), AddMemoryActivity::class.java))`;
  cờ `openingAddMemory` reset trong `onResume`.
- **Home:** xoá nhánh `onAdd` trong `renderMemoryNavigation()` (ẩn tab bar) và trong `applyWindowInsets()` (padding IME).

### Language: Settings dùng lại `LanguageActivity`
- Thêm extra và hàm tạo Intent:
  ```kotlin
  companion object {
      private const val EXTRA_FROM_SETTINGS = "com.example.lovetester.extra.FROM_SETTINGS"
      fun newIntent(context: Context, fromSettings: Boolean) =
          Intent(context, LanguageActivity::class.java).putExtra(EXTRA_FROM_SETTINGS, fromSettings)
  }
  private val fromSettings get() = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
  ```
- Nút xác nhận:
  - `fromSettings == true`: **không** ghi `OnboardingStep`, không mở Intro, mà `finish()` rồi `SupportedLanguages.apply(selected)`.
    Thứ tự này lấy từ comment trong `LanguageFragment`: rời màn trước, áp dụng sau, vì áp dụng ngôn ngữ sẽ tạo lại các Activity.
  - `fromSettings == false`: giữ nguyên luồng hiện tại.
- `activity_language.xml` cần nút ← (đang có trong `fragment_language.xml`): thêm vào, chỉ hiện khi `fromSettings`.
- `LanguageViewModel` dùng `SavedStateHandle`, chạy được trong Activity y hệt như trong Fragment.
- **Splash và Intro** vẫn mở `LanguageActivity` như cũ (không có extra, nên `fromSettings == false`).
- **SettingsActivity:** `layoutLanguage` → `startActivity(LanguageActivity.newIntent(this, fromSettings = true))`.
  Xoá `openLanguageFragment()`, `updateScreen()`, listener back stack, `settingsFragmentContainer`.
  Xoá `LanguageFragment.kt`, `fragment_language.xml`.

---

## 6. Thứ tự làm khuyến nghị

Mỗi bước build và chạy được. **Commit sau mỗi bước.**

1. **NameTest + Result → Activity** (Hướng dẫn A). Result Back tạm về `LoveTestActivity`.
   Horoscope tạm thời vẫn là Fragment, nhưng nút Love Test của nó chuyển sang mở `ResultActivity.newHoroscopeIntent(...)` luôn.
2. **HoroscopeTest → Activity.** Lúc này `nav_love_test.xml` chỉ còn `loveTestFragment`.
3. **LoveTest → tab Fragment trong Home** (Hướng dẫn B). Đổi Result Back sang `HomeActivity` + `TAB_LOVE_TEST`.
   Xoá `LoveTestActivity`, nav graph, Navigation.
4. **StartDate → Activity.**
5. **AddMemory → Activity.**
6. **Language trong Settings → LanguageActivity.**
7. Chạy lint, grep tên các lớp đã xoá, cập nhật `docs/OPTIMIZATION_REPORT.md`.

Lý do làm 1 trước 3: Fragment trong NavHost mở Activity được, nhưng muốn tab Love Test rời NavHost thì nó không được trỏ tới đích nào trong nav graph nữa.

---

## 7. Checklist kiểm thử (làm sau **mỗi** màn)

1. **Back:** nút ← và back hệ thống về đúng màn mong muốn.
   - NameTest ← về tab Love Test.
   - Result Retry về form, giữ nguyên chữ đã nhập.
   - Result ← về tab Love Test, **không** còn NameTest phía sau (bấm back thêm lần nữa phải về Home/thoát, không quay lại form).
2. **Xoay màn hình** ở mọi màn:
   - Chữ đã nhập, cung đã chọn, ngày đang chọn vẫn còn.
   - Tab đang đứng vẫn giữ.
   - Sheet đang mở vẫn mở và vẫn trả được kết quả.
3. **Bấm nhanh 2 lần:** nút Love Test, thẻ Name/Horoscope, ô ngày, nút Add Memory, tab. Chỉ được mở **1** màn.
4. **Đổi ngôn ngữ** (Settings → Language):
   - Quay về Settings và chữ đã đổi.
   - Không bị đưa về Intro.
   - Mở lại app thì giữ ngôn ngữ mới.
5. **Dữ liệu truyền đúng:**
   - Result hiện đúng 2 tên/2 cung và đúng %. Cùng đầu vào thì ra cùng %.
   - Thông điệp đúng loại test.
   - Lưu StartDate thì Home cập nhật số ngày.
   - Lưu AddMemory thì danh sách có memory mới.
6. **Bàn phím** không che ô nhập và nút ở NameTest, AddMemory.
7. **Thanh trạng thái:** icon đọc được, nội dung không nằm dưới thanh trạng thái hay thanh điều hướng.

---

## 8. Lỗi hay gặp và cách sửa

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `ActivityNotFoundException: Unable to find explicit activity class` | Quên khai báo trong manifest | Thêm `<activity android:name=".ui…XxxActivity" android:exported="false"/>` |
| `IllegalStateException: Hilt Fragments must be attached to an @AndroidEntryPoint Activity` | Activity mở sheet có `@AndroidEntryPoint` (Information, Background) nhưng chính nó thiếu | Thêm `@AndroidEntryPoint` cho Activity |
| `UninitializedPropertyAccessException: lateinit property repository` | Có `@Inject` nhưng thiếu `@AndroidEntryPoint` | Như trên |
| Chọn cung xong nhưng màn không đổi | Mở sheet bằng `supportFragmentManager` nhưng vẫn nghe bằng `childFragmentManager` (hoặc ngược lại) | Mở và nghe trên **cùng** FragmentManager (Bước A12) |
| Kết quả sheet mất sau khi xoay | Đăng ký listener trong hàm click | Đăng ký trong `onCreate` |
| `getStringExtra` trả `null` / % = 0 | Key gửi và key nhận khác nhau, hoặc gọi `Intent(...)` trực tiếp mà quên `putExtra` | Key `private` + hàm `newIntent` trong companion (Bước A4) |
| `does not have a NavController set` | Fragment không còn nằm trong NavHost mà vẫn gọi `findNavController()` | Đổi sang `startActivity` / `finish()` |
| Hai Fragment chồng nhau sau khi xoay | `replace(...)` trong `onCreate` mà không kiểm tra `savedInstanceState` | Chỉ thêm Fragment khi `savedInstanceState == null` |
| Bàn phím che nút | Edge-to-edge làm `adjustResize` không tự đẩy | Cộng `WindowInsetsCompat.Type.ime()` vào padding (Bước A10) |
| Result ← quay về form chứ không về menu | Chỉ gọi `finish()` | `CLEAR_TOP or SINGLE_TOP` về Home + `finish()` (Bước A6) |
| Back ở Home thoát app thay vì đóng tab | Transaction tab thiếu `addToBackStack` | Thêm `addToBackStack("home_tab")` |
| Memory mới không xuất hiện | Tưởng 2 Activity dùng chung ViewModel | Danh sách phải đọc từ Room (Flow), không đọc từ ViewModel của màn khác |
| Unresolved reference `FragmentNameTestBinding` | Đã đổi tên layout | Binding mới là `ActivityNameTestBinding` |
