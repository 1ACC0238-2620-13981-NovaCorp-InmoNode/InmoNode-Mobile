package com.novacorp.inmonode_app.features.fieldsales.domain.model

import java.math.BigDecimal

/** [front] and [depth] are null when the plan does not give them, as for an irregular lot; [area] always comes. */
data class LotDimensions(val front: BigDecimal?, val depth: BigDecimal?, val area: BigDecimal)
