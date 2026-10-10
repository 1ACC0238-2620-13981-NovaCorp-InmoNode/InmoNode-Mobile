package com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories

import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.database.atomic
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.core.network.requireBody
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Portfolio
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote.FieldSyncService
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.novacorp.inmonode_app.core.time.utcTimestamp
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioRepositoryImpl @Inject constructor(
    private val database: InmoNodeDatabase,
    private val service: FieldSyncService,
    private val session: FieldSession,
) : PortfolioRepository {
    private val downloadMutex = Mutex()
    private val projects = database.portfolioDao()
    private val lots = database.lotDao()

    override fun observePortfolio(): Flow<Portfolio> = session.ownerIds.flatMapLatest { owner ->
        if (owner == null) flowOf(Portfolio(emptyList(), null, null)) else observe(owner)
    }

    private fun observe(owner: Long): Flow<Portfolio> = combine(projects.observeProjects(owner),
        lots.observeLots(owner), projects.observeMetadata(owner), database.reservationDao().observeReservations(owner)) {
            projectRows, lotRows, metadata, reservations ->
        val claims = reservations.filter { it.holdsLot() }.associateBy { it.lotId }
        val byProject = lotRows.map { it.toDomain(claims[it.id]) }.groupBy { it.projectId }
        Portfolio(projectRows.map { it.toDomain(byProject[it.id].orEmpty()) }, metadata?.downloadedAt, metadata?.etag)
    }

    override suspend fun download(): Result<Portfolio> = operationResult {
        downloadMutex.withLock {
            val owner = session.requireOwner()
            val metadata = projects.metadata(owner)
            val response = service.portfolio(metadata?.etag.takeIf { metadata?.downloadedAt != null }, AuthenticatedRequestOwner(owner))
            if (response.code() == 304) {
                check(metadata?.downloadedAt != null) { "El servidor indicó 304 sin un portafolio local." }
            } else {
                val portfolio = response.requireBody()
                portfolio.validate()
                val projectRows = portfolio.projects.map { it.toEntity(owner) }
                val lotRows = portfolio.projects.flatMap { project -> project.lots.features.map { it.toEntity(owner, project.id) } }
                session.requireUnchanged(owner)
                database.atomic {
                    lots.deletePortfolioLots(owner)
                    projects.deleteProjects(owner)
                    projects.upsert(projectRows)
                    lots.upsert(lotRows)
                    projects.upsert((metadata ?: SyncMetadataEntity(owner)).copy(
                        etag = response.headers()["ETag"], downloadedAt = utcTimestamp()))
                }
            }
            session.requireUnchanged(owner)
            observe(owner).first()
        }
    }
}
