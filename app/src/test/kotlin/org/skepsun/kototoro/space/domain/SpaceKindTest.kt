package org.skepsun.kototoro.space.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.parsers.model.ContentType

class SpaceKindTest {

    @Test
    fun `empty content types defaults to ALL`() {
        emptySet<ContentType>().primarySpaceKind() shouldBe SpaceKind.ALL
    }

    @Test
    fun `pure novel types classify as NOVEL`() {
        setOf(ContentType.NOVEL).primarySpaceKind() shouldBe SpaceKind.NOVEL
        setOf(ContentType.NOVEL, ContentType.HENTAI_NOVEL).primarySpaceKind() shouldBe SpaceKind.NOVEL
        setOf(ContentType.HENTAI_NOVEL).primarySpaceKind() shouldBe SpaceKind.NOVEL
    }

    @Test
    fun `pure anime types classify as ANIME`() {
        setOf(ContentType.VIDEO).primarySpaceKind() shouldBe SpaceKind.ANIME
        setOf(ContentType.VIDEO, ContentType.HENTAI_VIDEO).primarySpaceKind() shouldBe SpaceKind.ANIME
        setOf(ContentType.HENTAI_VIDEO).primarySpaceKind() shouldBe SpaceKind.ANIME
    }

    @Test
    fun `pure manga and image types classify as MANGA`() {
        setOf(ContentType.MANGA).primarySpaceKind() shouldBe SpaceKind.MANGA
        setOf(
            ContentType.MANGA,
            ContentType.MANHWA,
            ContentType.MANHUA,
            ContentType.COMICS,
            ContentType.DOUJINSHI,
            ContentType.IMAGE_SET,
        ).primarySpaceKind() shouldBe SpaceKind.MANGA
    }

    @Test
    fun `mixed content types classify as ALL`() {
        setOf(ContentType.MANGA, ContentType.NOVEL).primarySpaceKind() shouldBe SpaceKind.ALL
        setOf(ContentType.MANGA, ContentType.VIDEO).primarySpaceKind() shouldBe SpaceKind.ALL
        setOf(ContentType.NOVEL, ContentType.VIDEO).primarySpaceKind() shouldBe SpaceKind.ALL
        setOf(
            ContentType.MANGA,
            ContentType.NOVEL,
            ContentType.VIDEO,
        ).primarySpaceKind() shouldBe SpaceKind.ALL
    }

    @Test
    fun `all standard content types classify as ALL`() {
        val allTypes = ContentType.entries.filterNot { it == ContentType.OTHER }.toSet()
        allTypes.primarySpaceKind() shouldBe SpaceKind.ALL
    }
}
