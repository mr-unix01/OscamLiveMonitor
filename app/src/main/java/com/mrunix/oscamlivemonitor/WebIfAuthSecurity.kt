package com.mrunix.oscamlivemonitor

internal fun webIfAuthHostAutorizzato(
    hostAtteso: String?,
    hostRichiesto: String?
): Boolean =
    !hostAtteso.isNullOrBlank() &&
    hostRichiesto?.equals(hostAtteso, ignoreCase = true) == true
