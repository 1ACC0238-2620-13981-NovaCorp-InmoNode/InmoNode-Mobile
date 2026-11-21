package com.novacorp.inmonode_app.core.format

import java.math.BigDecimal

/** Rates as the simulation and the preliminary contract show them: "12.5 %", "20 %". */
fun formatPercent(value: BigDecimal): String = "${value.stripTrailingZeros().toPlainString()} %"
