package org.skepsun.kototoro.reader.novel.annotation

import androidx.compose.ui.graphics.Color

enum class NovelMarkingColor(
    val id: Int,
    val displayName: String,
    val lineHex: Long,
    val bgHex: Long,
) {
    YELLOW(0, "经典黄", 0xFFF59E0B, 0x59FDE68A),
    GREEN(1, "清新绿", 0xFF10B981, 0x59A7F3D0),
    BLUE(2, "雅致蓝", 0xFF3B82F6, 0x59BFDBFE),
    PINK(3, "淡雅粉", 0xFFEC4899, 0x59FBCFE8),
    ORANGE(4, "暖阳橙", 0xFFF97316, 0x59FED7AA);

    val lineColor: Color get() = Color(lineHex)
    val bgColor: Color get() = Color(bgHex)

    companion object {
        fun fromId(id: Int): NovelMarkingColor = entries.find { it.id == id } ?: YELLOW
    }
}

enum class NovelMarkingStyle(
    val id: Int,
    val displayName: String,
) {
    UNDERLINE(0, "直线"),
    WAVY(1, "波浪线"),
    HIGHLIGHT(2, "马克笔");

    companion object {
        fun fromId(id: Int): NovelMarkingStyle = entries.find { it.id == id } ?: UNDERLINE
    }
}
