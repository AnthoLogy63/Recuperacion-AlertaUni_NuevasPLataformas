package com.erns.alertauni.data.repository

import com.erns.alertauni.data.local.DraftDao
import com.erns.alertauni.data.model.DraftEntity
import com.erns.alertauni.domain.manager.DataStoreHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Borradores locales (Room). Cada borrador pertenece a la cuenta que lo creo (correo + rol),
 * de modo que al iniciar sesion con otra cuenta no se muestran los de la anterior.
 */
@Singleton
class DraftRepository @Inject constructor(
    private val dao: DraftDao,
    private val dataStore: DataStoreHelper
) {
    private suspend fun owner(): String = "${dataStore.getEmail()}|${dataStore.getUserType().orEmpty()}"

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<List<DraftEntity>> = flow { emit(owner()) }.flatMapLatest { dao.observe(it) }

    suspend fun save(draft: DraftEntity) { dao.upsert(draft.copy(ownerKey = owner(), updatedAt = System.currentTimeMillis())) }

    suspend fun delete(id: Long) = dao.delete(id, owner())
}
