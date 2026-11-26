# Hướng dẫn chuyển màn hình giữa Fragment và Activity – LoveTester

Tài liệu dùng code thật của dự án. Code trong tài liệu đã rút gọn, phần `…` là giữ nguyên như cũ.

**Quy tắc công ty**
- Bấm nút để sang **một màn mới hoàn toàn** (thường có nút ←) → **Activity**.
- Nội dung đổi **ngay trong màn đang đứng** (tab, trang ViewPager, bottom sheet, dialog) → **Fragment**.
> Bảng phân loại ở mục 2 đang **chờ người hướng dẫn xác nhận**, kể cả điểm: *Home là "vỏ" chứa 3 tab Fragment*.
> Xác nhận xong rồi hãy bắt đầu.
 
---

## 0. Cấu trúc đích

```
📱 SplashActivity
📱 LanguageActivity            ← dùng cho lần đầu mở app VÀ khi mở từ Settings (EXTRA_FROM_SETTINGS)
📱 IntroActivity
   └── ViewPager2: 🧩 3 trang intro
📱 PermissionActivity
📱 HomeActivity                ← "vỏ": chỉ lo thanh tab dưới + chuyển tab + nút back
   ├── homeTabContainer
   │   ├── 🧩 HomeFragment          (tab Home)
   │   │   ├── 📄 BackgroundBottomSheet
   │   │   └── 📄 InformationBottomSheet ── 💬 AvatarOptionDialog, 💬 LoveDatePickerDialog
   │   ├── 🧩 LoveTestFragment      (tab Love test)
   │   └── 🧩 MemoryListFragment    (tab Memory)
   └── thanh tab: Home | Love test | Memory
📱 SettingsActivity
📱 NameTestActivity
📱 HoroscopeTestActivity  ── 📄 HoroscopePickerBottomSheet
📱 ResultActivity              ← dùng chung cho Name Test và Horoscope
📱 StartDateActivity      ── 💬 LoveDatePickerDialog
📱 AddMemoryActivity      ── 💬 LoveDatePickerDialog
```
📱 Activity · 🧩 Fragment · 📄 BottomSheet · 💬 Dialog

Không còn Navigation Component (`nav_love_test.xml`, `safeNavigate`).

**Luồng điều hướng**
```
[tab Home]      ── số ngày ─────→ StartDate
                ── ⚙️ ───────────→ Settings ── Language ──→ LanguageActivity (có nút ←)
[tab Love test] ── thẻ Name ────→ NameTest ──────→ Result
                ── thẻ Horoscope → HoroscopeTest ─→ Result
[tab Memory]    ── nút + ───────→ AddMemory
 
Result: Retry → finish() (về form)   |   ← → Home, mở tab Love test (đóng luôn form)
Home:   Back ở tab Love test/Memory → về tab Home   |   Back ở tab Home → thoát app
```
 
---

## 1. Khái niệm

| | Activity | Fragment |
|---|---|---|
| Là gì | Một màn hình độc lập, hệ thống quản lý | Một phần giao diện nằm **trong** Activity |
| Khai báo | Bắt buộc có trong `AndroidManifest.xml` | Không khai báo |
| Vòng đời | `onCreate → onStart → onResume … onDestroy` | Có thêm vòng đời của **view**: `onViewCreated … onDestroyView`. Fragment có thể còn sống khi view đã bị huỷ |
| View/binding | `setContentView`, binding sống bằng Activity | Binding phải bỏ khi view huỷ (dự án dùng `by viewBinding(...)`) |
| Back stack | Back stack của hệ thống (task) | Back stack của `FragmentManager` |
| Mở màn mới | `startActivity(Intent)` | `beginTransaction().add/replace/show(...)` |
| Đóng màn | `finish()` | `popBackStack()` / `hide(...)` |
| Truyền dữ liệu vào | `Intent` extras → `intent.getXxxExtra()` | `Bundle` arguments → `requireArguments()` |
| Trả kết quả | `registerForActivityResult`, hoặc ghi vào repository | `setFragmentResult` / `setFragmentResultListener` |
| ViewModel | `by viewModels()`, sống theo Activity | `by viewModels()` sống theo Fragment; `by activityViewModels()` **dùng chung** với Activity chứa nó |
| Context | Chính nó (`this`) | `requireContext()`, `requireActivity()` |
| Coroutine chạm UI | `lifecycleScope` / `collectOn(this)` | `viewLifecycleOwner.lifecycleScope` / `collectOn(viewLifecycleOwner)` |
| Mở sheet/dialog | `supportFragmentManager` | `childFragmentManager` |
| Hilt | `@AndroidEntryPoint` | `@AndroidEntryPoint`, và Activity chứa nó **cũng phải có** |

**Điều quan trọng nhất khi chuyển Fragment → Activity:** hai Activity không dùng chung ViewModel được.
Dữ liệu dùng chung phải đi qua `Intent` (dữ liệu nhỏ) hoặc repository (Room/DataStore).
 
---

## 2. Hiện trạng → mục tiêu

| Màn | Hiện tại | Mục tiêu | Tạo | Xoá |
|---|---|---|---|---|
| Splash | `SplashActivity` | Giữ nguyên | – | – |
| Language | `LanguageActivity` (lần đầu) + `LanguageFragment` (trong Settings) | **1** Activity, phân biệt bằng extra | extra `EXTRA_FROM_SETTINGS` | `LanguageFragment.kt`, `fragment_language.xml`, `settingsFragmentContainer` |
| Intro (3 trang) | `IntroActivity` + ViewPager2 | Giữ nguyên | – | – |
| Permission | `PermissionActivity` | Giữ nguyên | – | – |
| Home | `HomeActivity` chứa cả nội dung tab Home lẫn các container đè lên | **Vỏ** chỉ chứa `homeTabContainer` + thanh tab | – | `homeMemoryContainer`, `startDateContainer`, back stack `"home_memory"`, `renderMemoryNavigation()` |
| Tab Home | Nội dung nằm thẳng trong `activity_home.xml` | **`HomeFragment`** | `HomeFragment.kt`, `fragment_home.xml` | phần nội dung trong `activity_home.xml` |
| Tab Love Test | `LoveTestActivity` + `LoveTestFragment` | **Fragment tab** trong Home | – | `LoveTestActivity.kt`, `activity_love_test.xml`, `nav_love_test.xml` |
| Tab Memory | `MemoryListFragment` đè lên Home qua `homeMemoryContainer` | **Fragment tab** trong Home (ngang hàng 2 tab kia) | – | – |
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

Back ở Result phải đóng **cả** Result **và** NameTest để về màn chọn test. Viết thành **một** hàm để sau này chỉ sửa một chỗ:
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
- `SINGLE_TOP`: dùng lại Home đang có (Home nhận Intent qua `onNewIntent`), không tạo Home mới.
  Home đọc `EXTRA_TAB` trong `onNewIntent` rồi chọn tab Love test (xem Bước B4).
- **Tạm thời** (cho đến khi làm xong Hướng dẫn B, tức là khi tab Love Test vẫn còn là `LoveTestActivity`):
  thay `HomeActivity::class.java` bằng `LoveTestActivity::class.java` và bỏ dòng `putExtra`.
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
- `by activityViewModels()` (dùng chung với Activity chứa Fragment) **không còn ý nghĩa** khi màn đó thành Activity riêng, xem AddMemory ở mục 5.
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

## 4. Hướng dẫn B: Home thành "vỏ" quản lý 3 tab (Activity → Fragment)

Làm **sau** khi NameTest, HoroscopeTest, Result, StartDate và AddMemory đã là Activity (bước 1–4 ở mục 6).
Khi đó Home không còn phải chứa màn nào đè lên, chỉ còn chuyện tab, nên tách ra rất gọn.

### Bước B1. Hiểu hiện trạng và mục tiêu
**Hiện tại:**
```
HomeActivity
├── activity_home.xml chứa TRỰC TIẾP nội dung tab Home (ảnh nền, avatar, số ngày...)
├── homeMemoryContainer: Memory đặt ĐÈ lên nội dung Home, dùng back stack "home_memory"
└── tab Love test: mở LoveTestActivity riêng, Activity này tự vẽ lại một thanh tab giống hệt
```
**Mục tiêu:** `HomeActivity` chỉ là vỏ. Ba tab là 3 Fragment **ngang hàng** trong **một** container.

| | Trước | Sau |
|---|---|---|
| Nội dung tab Home | Nằm trong `activity_home.xml` | `HomeFragment` |
| Memory, Love Test | Đè lên Home / Activity riêng | Thay chỗ nhau trong `homeTabContainer` |
| `HomeActivity` | Dài, nhiều nhánh ẩn/hiện | Chỉ chuyển tab, tô màu tab, back |
| Back khi đang ở tab khác | Dựa vào back stack | `OnBackPressedCallback` về tab Home |

Chia làm 3 bước nhỏ, **mỗi bước chạy thử được**:
- **B2 (5a):** tách nội dung Home ra `HomeFragment`. App chạy y như cũ.
- **B3–B6 (5b):** bộ chuyển tab mới, chuyển Memory sang cơ chế này.
- **B7–B8 (5c):** thêm Love Test làm tab thứ 3, xoá `LoveTestActivity` và Navigation.
### Bước B2. Tách nội dung tab Home ra `HomeFragment`
- **File mới:** `ui/home/HomeFragment.kt`, `res/layout/fragment_home.xml`.
- **Layout:** cắt toàn bộ phần **nội dung** trong `activity_home.xml` (ảnh nền, nút ⚙️ và 🖼️, avatar, tên, tuổi,
  trái tim số ngày…) dán sang `fragment_home.xml`. `activity_home.xml` chỉ còn:
```xml
<androidx.constraintlayout.widget.ConstraintLayout …>
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/homeTabContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    <!-- thanh tab dưới giữ nguyên: layoutBottomNav, ivNavHome, tvNavHome, ivNavLoveTest… -->
</androidx.constraintlayout.widget.ConstraintLayout>
```
- **Code:** chuyển sang `HomeFragment` mọi thứ liên quan đến nội dung tab Home: collect `repository` (tên, avatar,
  ngày bắt đầu, hình nền), mở sheet Background/Information, mở StartDate.
```kotlin
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    @Inject lateinit var repository: LoveSettingsRepository
 
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // dán phần logic nội dung Home từ HomeActivity vào đây
    }
}
```
- **Đổi khi chép từ Activity sang Fragment** (xem bảng B9):
    - `collectOn(this)` → `collectOn(viewLifecycleOwner)`
    - `this` (Context) → `requireContext()`
    - `startActivity(Intent(this, StartDateActivity::class.java))` → `startActivity(Intent(requireContext(), StartDateActivity::class.java))`
    - **Sheet Background/Information:** `supportFragmentManager` → `childFragmentManager`, cho **cả** chỗ mở sheet
      **lẫn** chỗ `setFragmentResultListener` (nếu có). Mở một nơi, nghe một nơi khác thì kết quả không bao giờ tới.
- **Tạm thời** để kiểm tra: trong `HomeActivity.onCreate`, nếu `savedInstanceState == null` thì
  `add(R.id.homeTabContainer, HomeFragment(), TAG_HOME)`. Tab Memory vẫn chạy theo cách cũ ở bước này.
- **Chạy thử:** Home trông và hoạt động **y như trước**: đổi hình nền, sửa thông tin, mở StartDate, số ngày cập nhật.
### Bước B3. Khai báo các tab
Trong `HomeActivity`:
```kotlin
private enum class Tab(val tag: String) {
    HOME("tab_home"), LOVE_TEST("tab_love_test"), MEMORY("tab_memory");
 
    fun createFragment(): Fragment = when (this) {
        HOME -> HomeFragment()
        LOVE_TEST -> LoveTestFragment()
        MEMORY -> MemoryListFragment()
    }
}
 
private var currentTab = Tab.HOME
 
companion object {
    const val EXTRA_TAB = "com.example.lovetester.extra.TAB"
    const val TAB_HOME = "home"
    const val TAB_LOVE_TEST = "love_test"
    const val TAB_MEMORY = "memory"
    private const val KEY_CURRENT_TAB = "current_tab"
}
```
- **Tại sao dùng enum + tag:** mỗi tab có một tag cố định, nên lúc nào cũng tìm lại được Fragment của tab đó
  bằng `findFragmentByTag`, kể cả sau khi xoay màn hình.
- `EXTRA_TAB` và `TAB_*` là `public` vì `ResultActivity` cần dùng (Bước A6).
  Nếu dự án đã có `EXTRA_TAB`, `TAB_HOME`, `TAB_MEMORY` thì giữ tên cũ, chỉ thêm `TAB_LOVE_TEST`.
### Bước B4. Hàm chuyển tab bằng show/hide
```kotlin
private fun selectTab(tab: Tab) {
    val fm = supportFragmentManager
    if (fm.isStateSaved) return
 
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
 
    currentTab = tab
    backToHomeCallback.isEnabled = tab != Tab.HOME
    renderTabs()
}
```
- **Tại sao `show/hide` mà không `replace`:** `replace` huỷ view của tab cũ, quay lại thì tab bị dựng lại từ đầu
  (mất vị trí cuộn của Memory, chạy lại animation…). `hide` chỉ ẩn đi, quay lại thấy y nguyên.
- **Tab chỉ được tạo lần đầu** người dùng bấm vào (`add` khi chưa có). Tab Memory không bị tải nếu người dùng không mở.
- **Không `addToBackStack`:** chuyển tab không phải là "đi sâu vào màn mới", nên không đưa vào back stack.
  Back được xử lý riêng ở B5.
- Có thể thêm `.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)` cho mượt.
  **Nối vào các chỗ gọi:**
- Nút tab dưới: `layoutNavHome` → `selectTab(Tab.HOME)`, `layoutNavLoveTest` → `selectTab(Tab.LOVE_TEST)`,
  `layoutNavMemory` → `selectTab(Tab.MEMORY)`.
- Mở Home từ Result (`SINGLE_TOP`, Bước A6):
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    tabFromIntent(intent)?.let(::selectTab)
}
 
private fun tabFromIntent(intent: Intent): Tab? = when (intent.getStringExtra(EXTRA_TAB)) {
    TAB_HOME -> Tab.HOME
    TAB_LOVE_TEST -> Tab.LOVE_TEST
    TAB_MEMORY -> Tab.MEMORY
    else -> null
}
```
`setIntent(intent)` để các chỗ khác đọc `intent` thì thấy Intent **mới**, không phải Intent lúc mở Home lần đầu.
Nếu trước đây Home đọc `EXTRA_TAB` trong `onPostResume`, chuyển hết sang đây và xoá chỗ cũ.

### Bước B5. Nút back: tab khác → tab Home, tab Home → thoát
```kotlin
private val backToHomeCallback = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() = selectTab(Tab.HOME)
}
 
// trong onCreate:
onBackPressedDispatcher.addCallback(this, backToHomeCallback)
```
- Callback chỉ **bật** khi đang ở tab khác Home (`isEnabled = tab != Tab.HOME` trong `selectTab`).
- Ở tab Home, callback **tắt**, nên back hệ thống làm việc mặc định: đóng Home, thoát app.
### Bước B6. Giữ tab khi xoay màn hình
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    …
    currentTab = savedInstanceState?.getString(KEY_CURRENT_TAB)?.let(Tab::valueOf)
        ?: tabFromIntent(intent)
        ?: Tab.HOME
    selectTab(currentTab)
}
 
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(KEY_CURRENT_TAB, currentTab.name)
}
```
- Sau khi xoay, `FragmentManager` **tự khôi phục** các Fragment đã add (kể cả trạng thái ẩn/hiện).
  `selectTab` tìm theo tag nên **không tạo bản thứ hai**, chỉ show/hide cho đúng.
- Bỏ đoạn `add(HomeFragment)` tạm thời ở Bước B2: giờ `selectTab(Tab.HOME)` đã làm việc đó.
  **Chuyển Memory sang cơ chế mới (hết bước 5b):**
- Xoá `showMemoryTab()`, back stack `"home_memory"`, `homeMemoryContainer`, listener back stack và
  `renderMemoryNavigation()` cũ (thay bằng `renderTabs()` ở B7).
- `MemoryListFragment` không cần sửa gì, trừ khi nó tự gọi `popBackStack()` để "đóng tab": đổi thành không làm gì,
  vì tab giờ không nằm trong back stack.
- **Chạy thử:** chuyển Home ↔ Memory nhiều lần, cuộn Memory rồi sang Home và quay lại (vẫn ở chỗ cũ),
  back ở Memory về Home, back ở Home thoát app, xoay màn hình ở cả 2 tab.
### Bước B7. Thêm Love Test làm tab thứ 3 (bước 5c)
1. **`LoveTestFragment`:** bỏ mọi `findNavController()`; 2 thẻ mở Activity:
```kotlin
binding.cardNameTest.setOnClickListener {
    startActivity(Intent(requireContext(), NameTestActivity::class.java))
}
binding.cardHoroscopeTest.setOnClickListener {
    startActivity(Intent(requireContext(), HoroscopeTestActivity::class.java))
}
```
Chống bấm 2 lần bằng cờ `isNavigating` như Bước A7, reset trong `onResume`.
2. **Nút tab Love test** trong Home: thay `startActivity(LoveTestActivity…)` bằng `selectTab(Tab.LOVE_TEST)`.
3. **`renderTabs()`:** tô màu và `isSelected` cho cả 3 nút tab theo `currentTab`:
```kotlin
private fun renderTabs() {
    binding.ivNavHome.isSelected = currentTab == Tab.HOME
    binding.tvNavHome.isSelected = currentTab == Tab.HOME
    binding.ivNavLoveTest.isSelected = currentTab == Tab.LOVE_TEST
    binding.tvNavLoveTest.isSelected = currentTab == Tab.LOVE_TEST
    binding.ivNavMemory.isSelected = currentTab == Tab.MEMORY
    binding.tvNavMemory.isSelected = currentTab == Tab.MEMORY
    // nền thanh tab: theo Figma (trong suốt ở tab Home, trắng ở 2 tab còn lại)
}
```
Màu chữ/icon nên dùng selector (`state_selected`) trong XML thay vì tô bằng code, giống cách làm ô cung hoàng đạo.
4. **`ResultActivity.navigateToTestMenu()`:** bỏ phần tạm thời ở Bước A6, giờ trỏ về `HomeActivity` + `TAB_LOVE_TEST`.

### Bước B8. Insets trong vỏ Home
- **Thanh tab dưới** thuộc `HomeActivity`: Activity cộng inset của thanh điều hướng hệ thống vào padding dưới của thanh tab.
- **Mỗi tab tự lo phần trên:** `HomeFragment` để ảnh nền tràn lên dưới status bar, chỉ đẩy nút ⚙️/🖼️ xuống;
  `LoveTestFragment` và `MemoryListFragment` đẩy tiêu đề xuống bằng inset trên.
- **Phần dưới của tab:** thanh tab nằm đè lên đáy container, nên danh sách/nội dung cuối của `fragment_love_test.xml`
  và `fragment_memory_list.xml` cần `paddingBottom` bằng chiều cao thanh tab (đưa vào `dimens.xml`, ví dụ
  `tab_bar_height`) và `clipToPadding="false"` để cuộn được hết. Đối chiếu Figma để chọn đúng nền phía sau thanh tab.
### Bước B9. Bảng chuyển logic Activity → Fragment (quy tắc chung)
| Trong Activity | Trong Fragment |
|---|---|
| `onCreate` + `setContentView` | `Fragment(R.layout.x)` + `onViewCreated` |
| `lateinit var binding` | `private val binding by viewBinding(X::bind)` |
| `this` (Context) | `requireContext()`; cần Activity thì `requireActivity()` |
| `lifecycleScope` / `collectOn(this)` | `viewLifecycleOwner.lifecycleScope` / `collectOn(viewLifecycleOwner)` |
| `supportFragmentManager` | `childFragmentManager` (sheet của riêng Fragment) |
| `finish()` | không còn: tab không tự đóng, Activity vỏ quyết định |
| `onBackPressedDispatcher.addCallback(this)` | `requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner)` |
| `@AndroidEntryPoint` trên Activity | `@AndroidEntryPoint` trên Fragment **và** Activity chứa nó |

### Bước B10. Xoá phần cũ (hết bước 5c)
1. `LoveTestActivity.kt`, `activity_love_test.xml`, khai báo `<activity …LoveTestActivity>` trong manifest.
2. `nav_love_test.xml`.
3. `NavExtensions.kt` (`safeNavigate`) và 2 thư viện `androidx-navigation-fragment-ktx`,
   `androidx-navigation-ui-ktx` trong `build.gradle.kts` + `libs.versions.toml`.
4. Bộ anim `R.anim.screen_*` nếu không còn chỗ nào dùng (hoặc dùng lại cho `selectTab`).
5. Grep `LoveTestActivity`, `homeMemoryContainer`, `startDateContainer`, `findNavController`: phải còn **0** kết quả.
   Build, chạy, commit.
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
  Xoá `startDateContainer` trong `activity_home.xml` và các dòng `startDateContainer.isVisible`.
  (Ở Bước B2, `openStartDate()` sẽ chuyển sang `HomeFragment` và đổi `this` thành `requireContext()`.)
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
  Sau bước này Home không còn màn nào đè lên, nên Hướng dẫn B làm rất gọn.
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
- **Sau khi đổi ngôn ngữ**, Home được tạo lại: tab đang chọn vẫn giữ nhờ `onSaveInstanceState` (Bước B6).
---

## 6. Thứ tự làm khuyến nghị

Mỗi bước build và chạy được. **Commit sau mỗi bước.**

1. **NameTest + Result → Activity** (Hướng dẫn A). Result ← tạm về `LoveTestActivity`.
   Horoscope tạm thời vẫn là Fragment, nhưng nút Love Test của nó chuyển sang mở `ResultActivity.newHoroscopeIntent(...)` luôn.
2. **HoroscopeTest → Activity.** Lúc này `nav_love_test.xml` chỉ còn `loveTestFragment`.
3. **StartDate → Activity.**
4. **AddMemory → Activity.**
5. **Home thành vỏ 3 tab** (Hướng dẫn B):
    - **5a.** Tách `HomeFragment` (B2).
    - **5b.** Bộ chuyển tab show/hide, back về tab Home, giữ tab khi xoay; chuyển Memory sang (B3–B6).
    - **5c.** Thêm tab Love Test, đổi Result ← về `HomeActivity` + `TAB_LOVE_TEST`, xoá `LoveTestActivity` và Navigation (B7–B10).
6. **Language trong Settings → LanguageActivity.**
7. Chạy lint, grep tên các lớp đã xoá, cập nhật `docs/OPTIMIZATION_REPORT.md`.
   **Vì sao StartDate và AddMemory làm trước Home:** hiện tại cả hai đang là Fragment **đè lên** Home
   (`startDateContainer`, `homeMemoryContainer`). Chuyển chúng thành Activity trước thì khi tái cấu trúc Home,
   Home chỉ còn đúng chuyện tab, không phải vừa tách tab vừa giữ các màn đè lên.

**Vì sao NameTest/Horoscope làm trước tab Love Test:** tab Love Test chỉ rời NavHost được khi nó không còn
trỏ tới đích nào trong nav graph nữa.
 
---

## 7. Checklist kiểm thử (làm sau **mỗi** bước)

1. **Back:** nút ← và back hệ thống về đúng màn mong muốn.
    - NameTest ← về tab Love Test.
    - Result Retry về form, giữ nguyên chữ đã nhập.
    - Result ← về tab Love Test, **không** còn NameTest phía sau (bấm back thêm lần nữa về tab Home, lần nữa thì thoát).
    - Ở tab Love Test / Memory bấm back → về tab Home. Ở tab Home bấm back → thoát app.
2. **Chuyển tab:**
    - Cuộn Memory, sang tab khác rồi quay lại: vẫn ở vị trí cũ.
    - Màu tab đúng tab đang đứng.
    - Bấm tab đang đứng không làm gì lạ (không nháy, không tạo lại).
3. **Xoay màn hình** ở mọi màn:
    - Chữ đã nhập, cung đã chọn, ngày đang chọn vẫn còn.
    - Tab đang đứng vẫn giữ, không có 2 tab chồng lên nhau.
    - Sheet đang mở vẫn mở và vẫn trả được kết quả.
4. **Bấm nhanh 2 lần:** nút Love Test, thẻ Name/Horoscope, ô ngày, nút Add Memory, nút tab. Chỉ được mở **1** màn.
5. **Đổi ngôn ngữ** (Settings → Language):
    - Quay về Settings và chữ đã đổi.
    - Không bị đưa về Intro.
    - Quay lại Home vẫn đúng tab.
    - Mở lại app thì giữ ngôn ngữ mới.
6. **Dữ liệu truyền đúng:**
    - Result hiện đúng 2 tên/2 cung và đúng %. Cùng đầu vào thì ra cùng %.
    - Thông điệp đúng loại test.
    - Lưu StartDate thì tab Home cập nhật số ngày.
    - Lưu AddMemory thì tab Memory có memory mới.
    - Đổi hình nền / thông tin trong sheet thì tab Home cập nhật.
7. **Bàn phím** không che ô nhập và nút ở NameTest, AddMemory.
8. **Thanh trạng thái / thanh tab:** icon đọc được, nội dung không nằm dưới thanh trạng thái, thanh điều hướng
   hay thanh tab; cuộn được tới phần tử cuối cùng.
---

## 8. Lỗi hay gặp và cách sửa

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `ActivityNotFoundException: Unable to find explicit activity class` | Quên khai báo trong manifest | Thêm `<activity android:name=".ui…XxxActivity" android:exported="false"/>` |
| `IllegalStateException: Hilt Fragments must be attached to an @AndroidEntryPoint Activity` | Activity/Fragment mở sheet có `@AndroidEntryPoint` nhưng chính nó (hoặc Activity chứa nó) thiếu | Thêm `@AndroidEntryPoint` cho Activity, và cho `HomeFragment` |
| `UninitializedPropertyAccessException: lateinit property repository` | Có `@Inject` nhưng thiếu `@AndroidEntryPoint` | Như trên |
| Chọn cung / đổi hình nền xong nhưng màn không đổi | Mở sheet bằng FragmentManager này nhưng nghe bằng FragmentManager khác | Mở và nghe trên **cùng** FragmentManager (Bước A12). Khi chuyển nội dung Home sang `HomeFragment`, đổi **cả hai** sang `childFragmentManager` |
| Kết quả sheet mất sau khi xoay | Đăng ký listener trong hàm click | Đăng ký trong `onCreate` / `onViewCreated` |
| `getStringExtra` trả `null` / % = 0 | Key gửi và key nhận khác nhau, hoặc gọi `Intent(...)` trực tiếp mà quên `putExtra` | Key `private` + hàm `newIntent` trong companion (Bước A4) |
| `does not have a NavController set` | Fragment không còn nằm trong NavHost mà vẫn gọi `findNavController()` | Đổi sang `startActivity` / `finish()` |
| Hai tab chồng lên nhau sau khi xoay | `add(...)` / `replace(...)` trong `onCreate` mà không tìm Fragment cũ | Dùng `selectTab` tìm theo tag (Bước B4), chỉ `add` khi `findFragmentByTag` trả `null` |
| Quay lại tab Memory thì danh sách cuộn về đầu | Chuyển tab bằng `replace` | Dùng `show/hide` (Bước B4) |
| Back ở tab Memory thoát app thay vì về tab Home | Thiếu `OnBackPressedCallback` hoặc quên bật `isEnabled` | Bước B5: `isEnabled = tab != Tab.HOME` trong `selectTab` |
| Back ở tab Home không thoát app | Callback vẫn bật khi đang ở tab Home | Như trên |
| Result ← về Home nhưng vẫn ở tab cũ | Home không xử lý `onNewIntent` | Bước B4: `onNewIntent` → `setIntent` → `selectTab` |
| Result ← quay về form chứ không về menu | Chỉ gọi `finish()` | `CLEAR_TOP or SINGLE_TOP` về Home + `finish()` (Bước A6) |
| Bàn phím che nút | Edge-to-edge làm `adjustResize` không tự đẩy | Cộng `WindowInsetsCompat.Type.ime()` vào padding (Bước A10) |
| Phần cuối danh sách bị thanh tab che | Thiếu `paddingBottom` + `clipToPadding="false"` | Bước B8 |
| Memory mới không xuất hiện | Tưởng 2 Activity dùng chung ViewModel | Danh sách phải đọc từ Room (Flow), không đọc từ ViewModel của màn khác |
| Unresolved reference `FragmentNameTestBinding` | Đã đổi tên layout | Binding mới là `ActivityNameTestBinding` |
 