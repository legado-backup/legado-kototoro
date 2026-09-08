# Guide: Integrating Tachiyomi and Mihon Extensions

This guide is intended for developers of other Android reader applications (including Kotatsu-derived readers) who wish to integrate Tachiyomi, Mihon, or Aniyomi extension ecosystems into their own applications. Kototoro uses this approach to successfully map Mihon sources into its native framework.

## 1. Extension Discovery and Metadata Reading

Mihon/Tachiyomi extensions are installed as separate Android APKs. They are not executable apps but rather library packages.

**How to find them:**
You need to query the Android `PackageManager` to find installed packages that contain specific signatures or metadata.
- **Feature Flag**: Look for packages declaring the `tachiyomi.extension` feature.
- **Prefixes**: Extension package names usually contain `.extension`, `eu.kanade.tachiyomi.`, or `org.keiyoushi.`.
- **Metadata**: You need to extract `<meta-data>` from the `AndroidManifest.xml`, specifically `tachiyomi.extension.class` or `tachiyomi.extension.factory` to know which Kotlin class implements the `Source` interface.

**Important Android 11+ Gotcha:**
When calling `PackageManager.getPackageInfo(pkgName, PackageManager.GET_META_DATA)`, Android 11+ (API 30+) requires strict package visibility configuration. If you encounter missing metadata or `IllegalArgumentException`, ensure you handle package visibility queries correctly in your `AndroidManifest.xml` using the `<queries>` block. Furthermore, your broadcast receivers dealing with package events must be explicitly exported.

## 2. ClassLoader Isolation

Executing code from another APK safely is crucial to avoid dependency collisions.

**The Problem:**
If your app uses `okhttp 4.12.0` and the extension was compiled against `okhttp 3.12.0` or uses different Kotlin standard library versions, a direct load might cause `NoSuchMethodError` crashes.

**The Solution:**
Implement a **ChildFirstPathClassLoader**. When loading the extension's Dex file:
1. Try loading from the extension's APK first.
2. If not found, fall back to your parent application's ClassLoader.
3. *Exceptions:* Always delegate core shared libraries (like `kotlin.*`, `android.*`, and the interfaces `eu.kanade.tachiyomi.*`) to the parent ClassLoader. This ensures both your app and the extension reference the exact same underlying interface classes in memory.

## 3. Dependency Injection Bridging

Mihon extensions expect an `Application` context and specific networking instances (like `OkHttpClient`, `CookieJar`, `NetworkHelper`) to be provided by an internal dependency injector (Injekt or generic interfaces).

**How to Bridge:**
Before instantiating the extension's `Source` class, you must manually populate the shared registry with your own application's network instances.
1. Create a compat layer (e.g., an Injekt bridge if mirroring older architectures, or injecting directly into base Source references).
2. Bridge your Application `Context`, your `Json` serializer, and your `OkHttpClient`.
3. Provide a stub or wrapper for `NetworkHelper` that maps Mihon's interceptors into your app's networking layer.

*Note on Interceptors:* Mihon extensions rely on standard HTTP behaviors. If your app utilizes a customized interceptor chain (e.g., custom `GZipInterceptor`), ensure it doesn't conflict or cause double-decompression errors when the extension attempts to process encoded streams.

## 4. Model Conversion

Mihon's data models (`SManga`, `SChapter`, `Page`) likely don't perfectly align with your app's internal representations. Create a mapping layer that:
- Maps reading states (Ongoing, Completed, etc.).
- Converts filter lists safely to your internal search logic schemas.
- Maps chapters accurately. *Watch out for chapter order:* Mihon sources sometimes rely on reverse indexing. Ensure you enforce proper ascending or descending order when passing the lists into your UI.
- Resolves absolute URLs gracefully from relative paths (`HttpSource.getMangaUrl`).

## 5. Handling Source Updates

To provide a seamless experience, you need to react to extensions being installed, updated, or uninstalled dynamically while your app is running.

**Implementation:**
Register a `BroadcastReceiver` listening to standard Android package actions. Ensure you apply `addDataScheme("package")` to the IntentFilter:
- `Intent.ACTION_PACKAGE_ADDED`
- `Intent.ACTION_PACKAGE_REPLACED`
- `Intent.ACTION_PACKAGE_REMOVED`
- `Intent.ACTION_PACKAGE_FULLY_REMOVED`

*Gotcha:* Use `ContextCompat.registerReceiver(..., ContextCompat.RECEIVER_EXPORTED)` for receivers initialized at runtime to comply with recent Android 14+ security constraints. When triggered, debounce the event and rescan installed extensions in the background, updating your internal source registry dynamically.

## 6. Cloudflare & WebView Fallbacks

Many Tachiyomi sources rely heavily on Cloudflare bypasses and custom headers.
- **Cookies**: Ensure that cookies successfully resolved in your headless WebView (the `cf_clearance` cookie) are injected correctly into the shared `CookieJar` used by the mapped `OkHttpClient`.
- **Headers**: Extensions define internal `Headers` which often include specifically tailored `User-Agent` strings or Referer URLs. Pass these headers faithfully back into the WebView during challenge resolution and keep OkHttp perfectly synchronized, otherwise Cloudflare captures will fail.

## Related Resources
- [Mihon Integration Reference](../reference/mihon-integration.md) - Kototoro's internal implementation flow.
