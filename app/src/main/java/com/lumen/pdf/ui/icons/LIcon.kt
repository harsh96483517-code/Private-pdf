package com.lumen.pdf.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * Hand drawn 24x24 icon set so the app does not pull in the huge
 * material-icons-extended artifact. Every icon is one lightweight vector.
 */
object LIcon {

    private fun outline(name: String, width: Float = 1.7f, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathBuilder().apply(block).nodes,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()

    private fun solid(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathBuilder().apply(block).nodes,
            fill = SolidColor(Color.Black)
        ).build()

    val Add: ImageVector by lazy {
        outline("Add") {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }

    val Close: ImageVector by lazy {
        outline("Close") {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }

    val Check: ImageVector by lazy {
        outline("Check") {
            moveTo(4.5f, 12.5f); lineTo(9.5f, 17.5f); lineTo(19.5f, 6.5f)
        }
    }

    val ArrowBack: ImageVector by lazy {
        outline("ArrowBack") {
            moveTo(15f, 4f); lineTo(7f, 12f); lineTo(15f, 20f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        outline("ChevronRight") {
            moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
        }
    }

    val MoreVert: ImageVector by lazy {
        solid("MoreVert") {
            moveTo(10.6f, 5f)
            arcTo(1.4f, 1.4f, 0f, true, true, 13.4f, 5f)
            arcTo(1.4f, 1.4f, 0f, true, true, 10.6f, 5f)
            close()
            moveTo(10.6f, 12f)
            arcTo(1.4f, 1.4f, 0f, true, true, 13.4f, 12f)
            arcTo(1.4f, 1.4f, 0f, true, true, 10.6f, 12f)
            close()
            moveTo(10.6f, 19f)
            arcTo(1.4f, 1.4f, 0f, true, true, 13.4f, 19f)
            arcTo(1.4f, 1.4f, 0f, true, true, 10.6f, 19f)
            close()
        }
    }

    val Search: ImageVector by lazy {
        outline("Search") {
            moveTo(10.5f, 4.5f)
            arcTo(6f, 6f, 0f, true, true, 10.5f, 16.5f)
            arcTo(6f, 6f, 0f, true, true, 10.5f, 4.5f)
            close()
            moveTo(15.2f, 15.2f); lineTo(20f, 20f)
        }
    }

    val ZoomIn: ImageVector by lazy {
        outline("ZoomIn") {
            moveTo(10.5f, 4.5f)
            arcTo(6f, 6f, 0f, true, true, 10.5f, 16.5f)
            arcTo(6f, 6f, 0f, true, true, 10.5f, 4.5f)
            close()
            moveTo(15.2f, 15.2f); lineTo(20f, 20f)
            moveTo(7.8f, 10.5f); lineTo(13.2f, 10.5f)
            moveTo(10.5f, 7.8f); lineTo(10.5f, 13.2f)
        }
    }

    val ZoomOut: ImageVector by lazy {
        outline("ZoomOut") {
            moveTo(10.5f, 4.5f)
            arcTo(6f, 6f, 0f, true, true, 10.5f, 16.5f)
            arcTo(6f, 6f, 0f, true, true, 10.5f, 4.5f)
            close()
            moveTo(15.2f, 15.2f); lineTo(20f, 20f)
            moveTo(7.8f, 10.5f); lineTo(13.2f, 10.5f)
        }
    }

    val Folder: ImageVector by lazy {
        outline("Folder") {
            moveTo(3f, 8.5f)
            quadTo(3f, 6.5f, 5f, 6.5f)
            lineTo(9f, 6.5f)
            lineTo(11f, 8.5f)
            lineTo(19f, 8.5f)
            quadTo(21f, 8.5f, 21f, 10.5f)
            lineTo(21f, 17.5f)
            quadTo(21f, 19.5f, 19f, 19.5f)
            lineTo(5f, 19.5f)
            quadTo(3f, 19.5f, 3f, 17.5f)
            close()
        }
    }

    val FolderOpen: ImageVector by lazy {
        outline("FolderOpen") {
            moveTo(3f, 8.5f)
            quadTo(3f, 6.5f, 5f, 6.5f)
            lineTo(9f, 6.5f)
            lineTo(11f, 8.5f)
            lineTo(19f, 8.5f)
            quadTo(21f, 8.5f, 21f, 10.5f)
            lineTo(21f, 17.5f)
            quadTo(21f, 19.5f, 19f, 19.5f)
            lineTo(5f, 19.5f)
            quadTo(3f, 19.5f, 3f, 17.5f)
            close()
            moveTo(3f, 12.5f); lineTo(21f, 12.5f)
        }
    }

    val CreateNewFolder: ImageVector by lazy {
        outline("CreateNewFolder") {
            moveTo(3f, 8.5f)
            quadTo(3f, 6.5f, 5f, 6.5f)
            lineTo(9f, 6.5f)
            lineTo(11f, 8.5f)
            lineTo(18f, 8.5f)
            quadTo(20f, 8.5f, 20f, 10.5f)
            lineTo(20f, 17.5f)
            quadTo(20f, 19.5f, 18f, 19.5f)
            lineTo(5f, 19.5f)
            quadTo(3f, 19.5f, 3f, 17.5f)
            close()
            moveTo(15.5f, 14f); lineTo(15.5f, 19f)
            moveTo(13f, 16.5f); lineTo(18f, 16.5f)
        }
    }

    val PictureAsPdf: ImageVector by lazy {
        outline("PictureAsPdf") {
            moveTo(7f, 3.5f)
            lineTo(14f, 3.5f)
            lineTo(19f, 8.5f)
            lineTo(19f, 20.5f)
            lineTo(7f, 20.5f)
            close()
            moveTo(14f, 3.5f); lineTo(14f, 8.5f); lineTo(19f, 8.5f)
            moveTo(10f, 12.5f); lineTo(16f, 12.5f)
            moveTo(10f, 16.5f); lineTo(16f, 16.5f)
        }
    }

    val Star: ImageVector by lazy {
        solid("Star") {
            moveTo(12f, 3f)
            lineTo(14.6f, 9.2f)
            lineTo(21f, 9.8f)
            lineTo(16.2f, 14f)
            lineTo(17.6f, 20.3f)
            lineTo(12f, 17f)
            lineTo(6.4f, 20.3f)
            lineTo(7.8f, 14f)
            lineTo(3f, 9.8f)
            lineTo(9.4f, 9.2f)
            close()
        }
    }

    val StarBorder: ImageVector by lazy {
        outline("StarBorder", 1.6f) {
            moveTo(12f, 3f)
            lineTo(14.6f, 9.2f)
            lineTo(21f, 9.8f)
            lineTo(16.2f, 14f)
            lineTo(17.6f, 20.3f)
            lineTo(12f, 17f)
            lineTo(6.4f, 20.3f)
            lineTo(7.8f, 14f)
            lineTo(3f, 9.8f)
            lineTo(9.4f, 9.2f)
            close()
        }
    }

    val Favorite: ImageVector by lazy {
        solid("Favorite") {
            moveTo(12f, 20.5f)
            curveTo(12f, 20.5f, 3f, 14.6f, 3f, 8.9f)
            curveTo(3f, 5.7f, 5.4f, 3.5f, 8f, 3.5f)
            curveTo(10f, 3.5f, 11.4f, 4.8f, 12f, 6f)
            curveTo(12.6f, 4.8f, 14f, 3.5f, 16f, 3.5f)
            curveTo(18.6f, 3.5f, 21f, 5.7f, 21f, 8.9f)
            curveTo(21f, 14.6f, 12f, 20.5f, 12f, 20.5f)
            close()
        }
    }

    val Bookmark: ImageVector by lazy {
        solid("Bookmark") {
            moveTo(7f, 3.5f)
            lineTo(17f, 3.5f)
            lineTo(17f, 20.5f)
            lineTo(12f, 16f)
            lineTo(7f, 20.5f)
            close()
        }
    }

    val BookmarkBorder: ImageVector by lazy {
        outline("BookmarkBorder", 1.6f) {
            moveTo(7f, 3.5f)
            lineTo(17f, 3.5f)
            lineTo(17f, 20.5f)
            lineTo(12f, 16f)
            lineTo(7f, 20.5f)
            close()
        }
    }

    val Delete: ImageVector by lazy {
        outline("Delete") {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(9.5f, 7f); lineTo(9.5f, 4.5f); lineTo(14.5f, 4.5f); lineTo(14.5f, 7f)
            moveTo(6f, 7f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 7f)
            moveTo(10f, 11f); lineTo(10f, 16.5f)
            moveTo(14f, 11f); lineTo(14f, 16.5f)
        }
    }

    val Rename: ImageVector by lazy {
        outline("Rename") {
            moveTo(4f, 20f)
            lineTo(4.6f, 15.8f)
            lineTo(15.6f, 4.8f)
            lineTo(19.2f, 8.4f)
            lineTo(8.2f, 19.4f)
            close()
            moveTo(13.4f, 7f); lineTo(17f, 10.6f)
        }
    }

    val Image: ImageVector by lazy {
        outline("Image") {
            moveTo(3.5f, 5f); lineTo(20.5f, 5f); lineTo(20.5f, 19f); lineTo(3.5f, 19f); close()
            moveTo(7.4f, 10f)
            arcTo(1.6f, 1.6f, 0f, true, true, 10.6f, 10f)
            arcTo(1.6f, 1.6f, 0f, true, true, 7.4f, 10f)
            close()
            moveTo(5f, 17f); lineTo(10f, 12f); lineTo(14f, 16f); lineTo(17f, 13f); lineTo(20f, 16f)
        }
    }

    val Info: ImageVector by lazy {
        outline("Info") {
            moveTo(12f, 3.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 12f, 20.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 12f, 3.5f)
            close()
            moveTo(12f, 11f); lineTo(12f, 16.5f)
            moveTo(12f, 7.6f); lineTo(12f, 8.4f)
        }
    }

    val Fullscreen: ImageVector by lazy {
        outline("Fullscreen") {
            moveTo(4f, 9f); lineTo(4f, 4f); lineTo(9f, 4f)
            moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f)
            moveTo(20f, 15f); lineTo(20f, 20f); lineTo(15f, 20f)
            moveTo(9f, 20f); lineTo(4f, 20f); lineTo(4f, 15f)
        }
    }

    val FullscreenExit: ImageVector by lazy {
        outline("FullscreenExit") {
            moveTo(9f, 4f); lineTo(9f, 9f); lineTo(4f, 9f)
            moveTo(15f, 4f); lineTo(15f, 9f); lineTo(20f, 9f)
            moveTo(15f, 20f); lineTo(15f, 15f); lineTo(20f, 15f)
            moveTo(9f, 20f); lineTo(9f, 15f); lineTo(4f, 15f)
        }
    }

    val GridView: ImageVector by lazy {
        outline("GridView", 1.6f) {
            moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
            moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
            moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
            moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
        }
    }

    val History: ImageVector by lazy {
        outline("History") {
            moveTo(12f, 3.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 12f, 20.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 12f, 3.5f)
            close()
            moveTo(12f, 7.5f); lineTo(12f, 12.2f); lineTo(16f, 14.4f)
        }
    }

    val Home: ImageVector by lazy {
        outline("Home") {
            moveTo(3.5f, 11f); lineTo(12f, 3.8f); lineTo(20.5f, 11f)
            moveTo(6.2f, 10f); lineTo(6.2f, 20f); lineTo(17.8f, 20f); lineTo(17.8f, 10f)
            moveTo(10.2f, 20f); lineTo(10.2f, 14f); lineTo(13.8f, 14f); lineTo(13.8f, 20f)
        }
    }

    val Settings: ImageVector by lazy {
        outline("Settings") {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 17f); lineTo(20f, 17f)
            moveTo(7.3f, 7f)
            arcTo(2.3f, 2.3f, 0f, true, true, 11.9f, 7f)
            arcTo(2.3f, 2.3f, 0f, true, true, 7.3f, 7f)
            close()
            moveTo(13.6f, 12f)
            arcTo(2.3f, 2.3f, 0f, true, true, 18.2f, 12f)
            arcTo(2.3f, 2.3f, 0f, true, true, 13.6f, 12f)
            close()
            moveTo(8.4f, 17f)
            arcTo(2.3f, 2.3f, 0f, true, true, 13f, 17f)
            arcTo(2.3f, 2.3f, 0f, true, true, 8.4f, 17f)
            close()
        }
    }

    val Share: ImageVector by lazy {
        outline("Share") {
            moveTo(7.4f, 11.2f); lineTo(16.2f, 6.4f)
            moveTo(7.4f, 12.8f); lineTo(16.2f, 17.6f)
            moveTo(15.4f, 4.4f)
            arcTo(2.4f, 2.4f, 0f, true, true, 20.2f, 4.4f)
            arcTo(2.4f, 2.4f, 0f, true, true, 15.4f, 4.4f)
            close()
            moveTo(15.4f, 19.6f)
            arcTo(2.4f, 2.4f, 0f, true, true, 20.2f, 19.6f)
            arcTo(2.4f, 2.4f, 0f, true, true, 15.4f, 19.6f)
            close()
            moveTo(3.8f, 12f)
            arcTo(2.4f, 2.4f, 0f, true, true, 8.6f, 12f)
            arcTo(2.4f, 2.4f, 0f, true, true, 3.8f, 12f)
            close()
        }
    }

    val OpenInNew: ImageVector by lazy {
        outline("OpenInNew") {
            moveTo(13f, 4f); lineTo(20f, 4f); lineTo(20f, 11f)
            moveTo(20f, 4f); lineTo(11.5f, 12.5f)
            moveTo(17f, 14f); lineTo(17f, 19.5f); lineTo(4.5f, 19.5f); lineTo(4.5f, 7f); lineTo(10f, 7f)
        }
    }

    val Storage: ImageVector by lazy {
        outline("Storage") {
            moveTo(4f, 7.5f); lineTo(20f, 7.5f); lineTo(20f, 19.5f); lineTo(4f, 19.5f); close()
            moveTo(4f, 7.5f); lineTo(6.2f, 4f); lineTo(17.8f, 4f); lineTo(20f, 7.5f)
            moveTo(9.5f, 12f); lineTo(14.5f, 12f)
        }
    }

    val Palette: ImageVector by lazy {
        outline("Palette") {
            moveTo(12f, 3.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 12f, 20.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 12f, 3.5f)
            close()
            moveTo(8.6f, 8.6f); lineTo(8.6f, 9.4f)
            moveTo(15.4f, 8.6f); lineTo(15.4f, 9.4f)
            moveTo(8f, 14f); lineTo(8f, 14.8f)
        }
    }

    val Backup: ImageVector by lazy {
        outline("Backup") {
            moveTo(12f, 16f); lineTo(12f, 6f)
            moveTo(8f, 10f); lineTo(12f, 6f); lineTo(16f, 10f)
            moveTo(4f, 15.5f); lineTo(4f, 19.5f); lineTo(20f, 19.5f); lineTo(20f, 15.5f)
        }
    }

    val Refresh: ImageVector by lazy {
        outline("Refresh") {
            moveTo(19.6f, 8.4f)
            arcTo(8.2f, 8.2f, 0f, true, false, 5.6f, 18.4f)
            moveTo(19.6f, 8.4f); lineTo(19.9f, 3.6f)
            moveTo(19.6f, 8.4f); lineTo(14.8f, 8.1f)
        }
    }

    val AutoAwesome: ImageVector by lazy {
        solid("AutoAwesome") {
            moveTo(11f, 3f)
            lineTo(12.8f, 9.2f)
            lineTo(19f, 11f)
            lineTo(12.8f, 12.8f)
            lineTo(11f, 19f)
            lineTo(9.2f, 12.8f)
            lineTo(3f, 11f)
            lineTo(9.2f, 9.2f)
            close()
            moveTo(18.6f, 3.2f)
            lineTo(19.4f, 5.6f)
            lineTo(21.8f, 6.4f)
            lineTo(19.4f, 7.2f)
            lineTo(18.6f, 9.6f)
            lineTo(17.8f, 7.2f)
            lineTo(15.4f, 6.4f)
            lineTo(17.8f, 5.6f)
            close()
        }
    }
}
