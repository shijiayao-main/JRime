# Third-party notices

JRime is licensed under **GPL-3.0-or-later**. It is built on the open-source Rime
ecosystem and on many application libraries. This file is generated from the
build metadata (`aboutlibraries` + `app/licenses/libraries`) so the list reflects
what is actually packaged.

Regenerate with:

```sh
python3 script/gen-third-party-notices.py
```

In-app: **设置 → 关于 → 开源说明** (or About → Open source licenses) lists every
library with its license and links.

## Rime engine and native components

| Component | Version | License | Website |
|---|---|---|---|
| boostorg/boost | 1.89.0 | BSL-1.0 | https://www.boost.org/ |
| BYVoid/OpenCC | 1.4.2 | Apache-2.0 | https://opencc.byvoid.com/ |
| fmtlib/fmt | 12.1.0 | MIT | https://github.com/fmtlib/fmt |
| gabime/spdlog | 1.17.0 | MIT | https://github.com/gabime/spdlog |
| google/leveldb | 1.23.0 | BSD-3-Clause | https://github.com/google/leveldb |
| jbeder/yaml-cpp | 0.8.0 | MIT | https://github.com/jbeder/yaml-cpp |
| rime/librime | 1.17.0 | BSD-3-Clause | https://rime.im/ |
| s-yata/marisa-trie | 0.3.1 | BSD-2-Clause, LGPL-2.1-or-later | https://github.com/s-yata/marisa-trie |

The Rime engine stack is credited to the [Rime community](https://rime.im/)
(librime, OpenCC and their bundled libraries). JRime's Android frontend derives
from [Trime](https://github.com/osfans/trime); thank you to its authors and
contributors.

## Application, UI and tooling libraries

| Library | Version | License | Website |
|---|---|---|---|
| Activity | 1.13.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/activity#1.13.0 |
| Activity Compose | 1.13.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/activity#1.13.0 |
| Activity Kotlin Extensions | 1.13.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/activity#1.13.0 |
| Annotation | 1.10.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/annotation#1.10.0 |
| Experimental annotation | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/annotation#1.5.0 |
| AppCompat | 1.8.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/appcompat#1.8.0 |
| AppCompat Resources | 1.8.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/appcompat#1.8.0 |
| Android Arch-Common | 2.2.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/arch-core#2.2.0 |
| Android Arch-Runtime | 2.2.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/arch-core#2.2.0 |
| Autofill | 1.3.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/autofill#1.3.0 |
| Support CardView v7 | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| collections | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/collection#1.5.0 |
| Collections Kotlin Extensions | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/collection#1.5.0 |
| Compose Animation | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-animation#1.12.1 |
| Compose Animation Core | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-animation#1.12.1 |
| Compose Foundation | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.12.1 |
| Compose Layouts | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.12.1 |
| Compose Material3 Components | 1.4.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0 |
| Compose Material Icons Core | 1.7.8 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.8 |
| Compose Material Icons Extended | 1.7.8 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-material#1.7.8 |
| Compose Material Ripple | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-material#1.12.1 |
| Compose Runtime | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.12.1 |
| Compose Runtime Annotation | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.12.1 |
| Compose Runtime Retain | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.12.1 |
| Compose Saveable | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.12.1 |
| Compose UI | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-ui#1.12.1 |
| Compose Geometry | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-ui#1.12.1 |
| Compose Graphics | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-ui#1.12.1 |
| Compose UI Text | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-ui#1.12.1 |
| Compose Unit | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-ui#1.12.1 |
| Compose Util | 1.12.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose-ui#1.12.1 |
| AndroidX Futures | 1.0.0 | Apache-2.0 | https://developer.android.com/topic/libraries/architecture/index.html |
| ConstraintLayout | 2.2.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/constraintlayout#2.2.2 |
| ConstraintLayout Core | 1.1.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/constraintlayout#1.1.2 |
| Support Coordinator Layout | 1.1.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| Core | 1.19.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/core#1.19.1 |
| Core Kotlin Extensions | 1.19.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/core#1.19.1 |
| androidx.core:core-viewtree | 1.0.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/core#1.0.0 |
| Support Cursor Adapter | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Support Custom View | 1.1.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| androidx.customview:poolingcontainer | 1.0.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/customview#1.0.0 |
| androidx.databinding:viewbinding | 9.5.1 | Apache-2.0 |  |
| Support Document File | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Support Drawer Layout | 1.1.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| Support DynamicAnimation | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Emoji2 | 1.4.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/emoji2#1.4.0 |
| Emoji2 Views Helper | 1.4.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/emoji2#1.4.0 |
| Support fragment | 1.6.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/fragment#1.6.2 |
| Fragment Kotlin Extensions | 1.6.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/fragment#1.6.2 |
| Android Graphics Path | 1.0.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/graphics#1.0.1 |
| Support Interpolators | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Support core utils | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Lifecycle-Common | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle-Common for Java 8 | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle LiveData | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle LiveData Core | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| LiveData Core Kotlin Extensions | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| LiveData Kotlin Extensions | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle Process | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle Runtime | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle Runtime Compose | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle Kotlin Extensions | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle ViewModel | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle ViewModel Kotlin Extensions | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Lifecycle ViewModel with SavedState | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.0 |
| Support loader | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Support Local Broadcast Manager | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| Navigation Common | 2.10.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigation#2.10.2 |
| Navigation Fragment | 2.10.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigation#2.10.2 |
| Navigation Fragment Kotlin Extensions | 2.10.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigation#2.10.2 |
| Navigation Runtime | 2.10.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigation#2.10.2 |
| Navigation UI | 2.10.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigation#2.10.2 |
| Navigation UI Kotlin Extensions | 2.10.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigation#2.10.2 |
| Navigation Event | 1.0.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigationevent#1.0.0 |
| NavigationEvent Compose | 1.0.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/navigationevent#1.0.0 |
| Paging-Common | 3.5.3 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/paging#3.5.3 |
| Paging-Common Kotlin Extensions | 3.5.3 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/paging#3.5.3 |
| Paging-Runtime | 3.5.3 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/paging#3.5.3 |
| Paging-Runtime Kotlin Extensions | 3.5.3 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/paging#3.5.3 |
| AndroidX Preference | 1.2.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/preference#1.2.1 |
| Android Preferences KTX | 1.2.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/preference#1.2.1 |
| Support Print | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| RecyclerView | 1.4.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/recyclerview#1.4.0 |
| Android Resource Inspection - Annotations | 1.0.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/resourceinspection#1.0.1 |
| Room-Common | 2.8.5 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/room#2.8.5 |
| Room Kotlin Extensions | 2.8.5 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/room#2.8.5 |
| Room Paging | 2.8.5 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/room#2.8.5 |
| Room-Runtime | 2.8.5 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/room#2.8.5 |
| Saved State | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/savedstate#1.5.0 |
| Saved State Compose | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/savedstate#1.5.0 |
| SavedState Kotlin Extensions | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/savedstate#1.5.0 |
| Support Sliding Pane Layout | 1.2.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/slidingpanelayout#1.2.0 |
| SQLite | 2.6.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/sqlite#2.6.2 |
| SQLite Framework Integration | 2.6.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/sqlite#2.6.2 |
| Startup Runtime | 1.2.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/startup#1.2.0 |
| Android Tracing | 1.2.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/tracing#1.2.0 |
| Android Transition Support Library | 1.4.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/transition#1.4.1 |
| Support VectorDrawable | 1.1.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| Support AnimatedVectorDrawable | 1.1.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| VersionedParcelable | 1.1.1 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| ViewPager2 | 1.1.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/viewpager2#1.1.0 |
| Support View Pager | 1.0.0 | Apache-2.0 | http://developer.android.com/tools/extras/support-library.html |
| WindowManager | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/window#1.5.0 |
| WindowManager Core | 1.5.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/window#1.5.0 |
| kaml | 0.104.0 | Apache-2.0 | https://github.com/charleskorn/kaml |
| com.github.getActivity:DeviceCompat | 2.6 | unknown |  |
| getActivity/XXPermissions | 28.3 | Apache-2.0 | https://github.com/getActivity/XXPermissions |
| com.github.WhiredPlanck:StorageAccess | 2.0.0 | unknown |  |
| flexbox-layout | 3.0.0 | Apache-2.0 |  |
| Material Components for Android | 1.4.0 | Apache-2.0 | https://github.com/material-components/material-components-android |
| Guava ListenableFuture only | 1.0 | Apache-2.0 | https://github.com/google/guava/listenablefuture |
| Timber | 5.0.1 | Apache-2.0 | https://github.com/JakeWharton/timber |
| splitties-bitflags | 3.0.0 | Apache-2.0 | https://github.com/LouisCAD/Splitties |
| splitties-collections | 3.0.0 | Apache-2.0 | https://github.com/LouisCAD/Splitties |
| splitties-experimental | 3.0.0 | Apache-2.0 | https://github.com/LouisCAD/Splitties |
| AboutLibraries Core Library | 15.3.0 | Apache-2.0 | https://github.com/mikepenz/AboutLibraries |
| Android-Iconics Community Material Icons Typeface Library | 7.0.96.2-kotlin | Apache-2.0 | https://github.com/mikepenz/Android-Iconics |
| Android-Iconics Library | 5.6.0 | Apache-2.0 | https://github.com/mikepenz/Android-Iconics |
| Android-Iconics Typeface API Library | 5.6.0 | Apache-2.0 | https://github.com/mikepenz/Android-Iconics |
| okio | 3.16.4 | Apache-2.0 | https://github.com/square/okio/ |
| BaseRecyclerViewAdapterHelper | 4.4.2 | MIT | https://github.com/CymChad/BaseRecyclerViewAdapterHelper |
| SnakeYAML Engine KMP | 4.0.1 | Apache-2.0 | https://github.com/krzema12/snakeyaml-engine-kmp/ |
| UrlEncoder for Kotlin Multiplatform | 1.6.0 | Apache-2.0 | https://github.com/ethauvin/urlencoder |
| Lifecycle-Common | 2.9.6 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Lifecycle Runtime | 2.9.6 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Lifecycle Runtime Compose | 2.9.6 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Saved State | 1.3.6 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Saved State Compose | 1.3.6 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Compose Animation | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Animation Core | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Annotation | 1.9.1 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| collections | 1.9.1 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Compose Foundation | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Layouts | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Material3 Components | 1.9.0 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Compose Material Icons Core | 1.7.3 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Compose Material Icons Extended | 1.7.3 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Compose Material Ripple | 1.9.1 | Apache-2.0 | https://github.com/JetBrains/compose-jb |
| Compose Runtime | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Saveable | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose UI | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose BackHandler | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Geometry | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Graphics | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose UI Text | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Unit | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Compose Util | 1.12.2 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform |
| Parcelize Runtime | 2.4.21 | Apache-2.0 | https://kotlinlang.org/ |
| Kotlin Stdlib | 2.4.21 | Apache-2.0 | https://kotlinlang.org/ |
| Kotlin Stdlib Common | 2.4.21 | Apache-2.0 | https://kotlinlang.org/ |
| Kotlin Stdlib Jdk7 | 2.2.21 | Apache-2.0 | https://kotlinlang.org/ |
| Kotlin Stdlib Jdk8 | 2.2.21 | Apache-2.0 | https://kotlinlang.org/ |
| kotlinx-coroutines-android | 1.11.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| kotlinx-coroutines-core | 1.11.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| kotlinx-serialization-core | 1.13.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.serialization |
| kotlinx-serialization-json | 1.13.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.serialization |
| JetBrains Java Annotations | 23.0.0 | Apache-2.0 | https://github.com/JetBrains/java-annotations |
| JSpecify annotations | 1.0.0 | Apache-2.0 | http://jspecify.org/ |
| Kodein | 7.33.0 | MIT | http://kodein.org |
| Kaverit | 2.14.0 | MIT | http://kodein.org |

> Versions are captured from the last `assembleDebug`/`assembleRelease` metadata.
> If a table is empty, build the app once and rerun the generator.
