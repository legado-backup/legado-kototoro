package org.skepsun.kototoro.settings.sources.unified

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.skepsun.kototoro.extensions.install.ExtensionInstallMode

class UnifiedSourcePackageInstallModeTest : FunSpec({

    test("system-installed package update uses the system APK installer") {
        packageItem(UnifiedSourcePackageInstallLocation.SYSTEM).preferredInstallMode() shouldBe ExtensionInstallMode.SYSTEM
    }

    test("sideload-installed package update stays in the app-private APK store") {
        packageItem(UnifiedSourcePackageInstallLocation.LOCAL_APK).preferredInstallMode() shouldBe ExtensionInstallMode.LOCAL_APK
    }

    test("new package without an installation history keeps the sideload default") {
        packageItem(null).preferredInstallMode() shouldBe ExtensionInstallMode.LOCAL_APK
    }
})

private fun packageItem(
    installLocation: UnifiedSourcePackageInstallLocation?,
): UnifiedSourcePackageItem {
    return UnifiedSourcePackageItem(
        id = "package:MIHON:org.example.extension",
        kind = UnifiedSourceKind.MIHON,
        name = "Example",
        packageName = "org.example.extension",
        repositoryId = "repo:MIHON:https://example.org/index.json",
        repositoryName = "Example repo",
        versionName = "2.0.0",
        versionCode = 2L,
        language = "en",
        isInstalled = true,
        isNsfw = false,
        sourceCount = 1,
        sourceNames = listOf("Example source"),
        state = UnifiedSourcePackageState.UPDATE_AVAILABLE,
        installedVersionName = "1.0.0",
        installLocation = installLocation,
    )
}
