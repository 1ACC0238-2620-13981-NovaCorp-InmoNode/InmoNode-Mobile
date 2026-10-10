package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

data class PortfolioTransferState(val ownerId:Long?=null,val loading:Boolean=false,val completed:Boolean=false,val errorMessage:String?=null)

/** A screen can leave a transfer running; changing agent cancels it. */
@Singleton
class PortfolioDownloadCoordinator @Inject constructor(private val download:DownloadPortfolioUseCase,private val auth:AuthRepository,
    @com.novacorp.inmonode_app.core.di.ApplicationScope private val scope:CoroutineScope) {
    private val state=MutableStateFlow(PortfolioTransferState())
    private val lock=Any()
    private var transfer:Job?=null
    init {
        scope.launch {
            auth.currentUser.map { it?.id }.distinctUntilChanged().collect { owner ->
                synchronized(lock) {
                    if(state.value.ownerId!=null&&state.value.ownerId!=owner) {
                        transfer?.cancel();state.value=PortfolioTransferState()
                    }
                }
            }
        }
    }
    fun observe():Flow<PortfolioTransferState> = combine(auth.currentUser,state) { user,transfer ->
        if(user?.id==transfer.ownerId)transfer else PortfolioTransferState()
    }
    fun start() = synchronized(lock) {
        if(transfer?.isActive==true)return@synchronized
        transfer=scope.launch {
            val owner=auth.currentUser.first()?.takeIf { it.isFieldAgent }?.id?:return@launch
            state.value=PortfolioTransferState(ownerId=owner,loading=true)
            val result=download()
            synchronized(lock) {
                if(isActive&&state.value.ownerId==owner) {
                    state.value=result.fold({ PortfolioTransferState(ownerId=owner,completed=true) },
                        { PortfolioTransferState(ownerId=owner,errorMessage=it.message) })
                }
            }
        }
    }
}
