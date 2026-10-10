package com.novacorp.inmonode_app.features.fieldsales.presentation.prospects
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Prospect
data class ProspectsUiState(val prospects: List<Prospect> = emptyList(), val loaded:Boolean=false, val query: String = "", val offline: Boolean = false,
 val name: String = "", val document: String = "", val phone: String = "", val maritalStatus: String = "",
 val saving: Boolean = false, val savedId: String? = null, val errorMessage: String? = null) {
 val filtered get() = prospects.filter { it.fullName.contains(query,true) || it.document.contains(query,true) }
}
