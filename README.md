# GlassPDF — Premium offline PDF reader & smart library

Kotlin · Jetpack Compose · Material 3 · Room · Navigation Compose · Storage Access Framework · PdfRenderer + PdfBox-Android (text).

## Build the APK with GitHub (no Android Studio needed)

1. Create a new **empty repository** on GitHub.
2. Upload **everything inside this folder** (keep the `.github` folder!) — or `git push` it.
3. Open the repo → **Actions** tab → **Build APK** → it runs automatically on every push
   (or press **Run workflow**).
4. When it turns green, open the run → **Artifacts**:
   * `GlassPDF-debug-apk`   → `app-debug.apk`  (always built)
   * `GlassPDF-release-apk` → `app-release.apk` (optimized, signed with the debug key)
5. Download the zip, extract, install the APK on your phone (allow “install unknown apps”).

First build takes ~5–8 minutes (Gradle downloads dependencies). If a step fails, open it in the
Actions log and send me the red error lines — I’ll fix it.

## Build locally (optional)
Android Studio Koala+ → *Open* this folder → Run. Or with Gradle 8.9 + JDK 17 + Android SDK 34: `gradle assembleDebug`.

## Using the app
* **Home → “+”** adds PDF files or a whole folder (sub-folders included). Files are read in place; nothing is copied.
* **Auto Arrange** (floating pill) analyzes names, embedded titles, first-page text, part/chapter numbers and
  content similarity, shows a **preview with confidence scores**, lets you correct/reorder, and only changes the
  *library organization* after you confirm. Real files are never moved or deleted.
* **Folders**: nested folders, custom covers, long-press + drag a PDF/folder onto a folder or breadcrumb,
  multi-select → Move, reorder PDFs by dragging handles.
* **Reader**: vertical scroll or horizontal pages, pinch/double-tap zoom, thumbnails, search, bookmarks,
  remembers last page, full screen, share, open with another app.
