package org.skepsun.kototoro.core.dictionary

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.skepsun.kototoro.core.db.MangaDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationDictionaryRepository @Inject constructor(
    private val database: MangaDatabase,
    private val dao: TranslationDictionaryDao,
    private val json: Json,
) {

    suspend fun get(bookKey: String): BookDictionary = withContext(Dispatchers.IO) {
        val entity = dao.get(bookKey) ?: return@withContext BookDictionary(bookKey)
        BookDictionary(
            bookKey = entity.bookKey,
            pairs = runCatching {
                json.decodeFromString(ListSerializer(DictPair.serializer()), entity.pairsJson)
            }.getOrDefault(emptyList()),
            updatedAt = entity.updatedAt,
        )
    }

    suspend fun mergeDiscoveredPairs(bookKey: String, discovered: List<DictPair>): BookDictionary =
        withContext(Dispatchers.IO) {
            val current = get(bookKey)
            val merged = TranslationDictionaryPolicy.mergeDiscovered(current.pairs, discovered)
            val result = current.copy(pairs = merged, updatedAt = System.currentTimeMillis())
            database.withTransaction {
                dao.upsert(
                    TranslationDictionaryEntity(
                        bookKey = result.bookKey,
                        pairsJson = json.encodeToString(ListSerializer(DictPair.serializer()), result.pairs),
                        updatedAt = result.updatedAt,
                    ),
                )
            }
            result
        }

    suspend fun replace(bookKey: String, pairs: List<DictPair>): BookDictionary =
        withContext(Dispatchers.IO) {
            val normalized = TranslationDictionaryPolicy.mergeDiscovered(emptyList(), pairs)
            val result = BookDictionary(bookKey, normalized, System.currentTimeMillis())
            dao.upsert(
                TranslationDictionaryEntity(
                    bookKey = result.bookKey,
                    pairsJson = json.encodeToString(ListSerializer(DictPair.serializer()), result.pairs),
                    updatedAt = result.updatedAt,
                ),
            )
            result
        }

    suspend fun delete(bookKey: String) = withContext(Dispatchers.IO) {
        dao.delete(bookKey)
    }
}
