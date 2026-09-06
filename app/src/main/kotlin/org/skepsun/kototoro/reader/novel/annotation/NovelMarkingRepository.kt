package org.skepsun.kototoro.reader.novel.annotation

import kotlinx.coroutines.flow.Flow
import org.skepsun.kototoro.core.db.MangaDatabase
import javax.inject.Inject

class NovelMarkingRepository @Inject constructor(
    private val database: MangaDatabase,
) {

    fun observe(mangaId: Long): Flow<List<NovelMarkingEntity>> = database.getNovelMarkingDao().observe(mangaId)

    suspend fun delete(marking: NovelMarkingEntity) {
        database.getNovelMarkingDao().deleteById(marking.id)
    }

    suspend fun updateNote(marking: NovelMarkingEntity, note: String?) {
        database.getNovelMarkingDao().updateNote(
            id = marking.id,
            note = note,
            updatedAt = System.currentTimeMillis(),
        )
    }

    /** Returns true when a marking was added, false when an identical marking was removed. */
    suspend fun toggle(
        mangaId: Long,
        chapterId: Long,
        chapterIndex: Int,
        startOffset: Int,
        endOffset: Int,
        selectedText: String,
        note: String? = null,
    ): Boolean {
        val dao = database.getNovelMarkingDao()
        val existing = dao.find(mangaId, chapterId, startOffset, endOffset)
        if (existing != null) {
            dao.delete(existing)
            return false
        }
        val now = System.currentTimeMillis()
        dao.insert(
            NovelMarkingEntity(
                mangaId = mangaId,
                chapterId = chapterId,
                chapterIndex = chapterIndex,
                startOffset = startOffset,
                endOffset = endOffset,
                selectedText = selectedText,
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return true
    }

    suspend fun addNote(
        mangaId: Long,
        chapterId: Long,
        chapterIndex: Int,
        startOffset: Int,
        endOffset: Int,
        selectedText: String,
        note: String,
    ) {
        val dao = database.getNovelMarkingDao()
        val existing = dao.find(mangaId, chapterId, startOffset, endOffset)
        val now = System.currentTimeMillis()
        if (existing != null) {
            dao.updateNote(existing.id, note, now)
        } else {
            dao.insert(
                NovelMarkingEntity(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    chapterIndex = chapterIndex,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    selectedText = selectedText,
                    note = note,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}
