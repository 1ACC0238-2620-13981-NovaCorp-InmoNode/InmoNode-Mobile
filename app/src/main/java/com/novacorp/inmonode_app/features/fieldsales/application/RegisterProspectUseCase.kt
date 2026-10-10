package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ProspectRepository
import javax.inject.Inject

class RegisterProspectUseCase @Inject constructor(private val repository: ProspectRepository) {
    suspend operator fun invoke(document: String, fullName: String, phone: String = "", maritalStatus: String? = null) =
        repository.register(document, fullName, phone, maritalStatus)
}
