# Lumen PDF — premium offline PDF reader & smart library

A fully offline Android PDF reader with a Liquid Glass interface, book-cover style library,
nested folders, custom covers and a content-aware auto-arrange engine.

Kotlin · Jetpack Compose · Material 3 · Room · Storage Access Framework · PdfBox.
No server, no account, no API key, no ads — every byte stays on the device.

## Features

**Library**
- Large first-page cover previews rendered with `PdfRenderer` and cached as PNG
- Editable display title, three-dot menu: open / rename / move / favorite / cover / info / remove
- Layouts: 2 columns, 3 columns, list, horizontal strip — remembered across launches
- Multi-select (long press) with move, favorite and remove
- Long-press drag to reorder covers, order persisted in Room
- Recently opened strip, favorites, search over titles, keywords and extracted PDF text
- Sort by title, recent, size or pages; filter for recent and favorites

**Folders**
- Unlimited nested folders, rename, delete (contents kept and moved one level up)
- Custom folder cover from gallery, otherwise the first PDF cover inside is used
- PDF counts and small cover previews per folder
- Move PDFs between folders and whole folders into other folders (cycle safe)

**Auto arrange**
- Content aware: filename similarity, common keywords, PDF title metadata, first-page text,
  chapter names, sequence numbers and content similarity (tf-idf + cosine clustering)
- Confidence score per proposed folder, full preview before anything changes
- Detects part / chapter / volume numbers for reading order, else uses content similarity
- Unknown documents land in an `Unsorted` folder. Files are never moved or deleted on disk.

**Reader**
- Vertical page rendering through a single-thread `PdfRenderer` session with a bitmap cache
- Pinch to zoom, zoom buttons, pan while zoomed, full screen mode
- Page thumbnails, page indicator, last reading position, bookmarks per page
- Search inside the PDF (PdfBox), jump to a hit, share, open with another app

**Settings**
- System / light / dark theme, layout selector, recent strip, automatic covers, manual order
- Auto arrange sensitivity, storage summary, clear cache, rescan metadata
- Backup and restore of settings as a small JSON file

## Build

```bash
# needs JDK 17 + Android SDK 34
gradle :app:assembleDebug      # or ./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew` is not committed; run `gradle wrapper` once or open the project in Android Studio,
which generates it. CI builds a debug APK on every push to `main`
(`.github/workflows/build.yml`, artifact `lumen-pdf-debug-apk`).

## Notes

- The debug APK is signed with the debug keystore; use your own release keystore for the Play Store.
- PDFs are referenced through SAF URIs and never duplicated. Uninstalling the app leaves them untouched.
- Removing a PDF from Lumen only removes the library entry and its cached cover.
