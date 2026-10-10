package com.novacorp.inmonode_app.features.fieldsales
import com.novacorp.inmonode_app.features.fieldsales.application.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Portfolio
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
import com.novacorp.inmonode_app.features.iam.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioCoordinatorTest {
 private class Auth:AuthRepository {
  val user=MutableStateFlow<User?>(User(1,"agent@example.test",UserRole.FIELD_AGENT))
  override val currentUser:Flow<User?> = user
  override suspend fun signIn(email:String,password:String):Result<User> = Result.failure(IllegalStateException())
  override suspend fun signOut() { user.value=null }
 }
 private class Repository:PortfolioRepository {
  var calls=0;var cancelled=false
  val gate=CompletableDeferred<Unit>()
  override fun observePortfolio()=flowOf(Portfolio(emptyList(),null,null))
  override suspend fun download():Result<Portfolio> {
   calls++
   try { gate.await();return Result.success(Portfolio(emptyList(),"2026-10-01",null)) }
   catch(e:CancellationException) { cancelled=true;throw e }
  }
 }
 @Test fun leavingScreenDoesNotStartDuplicateDownload()=runTest {
  val repository=Repository();val auth=Auth()
  val coordinator=PortfolioDownloadCoordinator(DownloadPortfolioUseCase(repository),auth,backgroundScope)
  coordinator.start();runCurrent();coordinator.start();runCurrent()
  assertEquals(1,repository.calls);assertTrue(coordinator.observe().first().loading)
  repository.gate.complete(Unit);runCurrent()
  assertTrue(coordinator.observe().first().completed)
 }
 @Test fun changingAgentCancelsAndHidesOldTransfer()=runTest {
  val repository=Repository();val auth=Auth()
  val coordinator=PortfolioDownloadCoordinator(DownloadPortfolioUseCase(repository),auth,backgroundScope)
  coordinator.start();runCurrent()
  auth.user.value=User(2,"another@example.test",UserRole.FIELD_AGENT);runCurrent()
  assertTrue(repository.cancelled);assertFalse(coordinator.observe().first().loading)
  assertFalse(coordinator.observe().first().completed)
 }
}
