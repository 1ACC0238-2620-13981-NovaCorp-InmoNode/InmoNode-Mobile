package com.novacorp.inmonode_app.features.fieldsales.application
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ProspectRepository
import javax.inject.Inject
class CompleteProspectUseCase @Inject constructor(private val repository:ProspectRepository) {
 suspend operator fun invoke(id:String,maritalStatus:String)=repository.completeMaritalStatus(id,maritalStatus)
}
