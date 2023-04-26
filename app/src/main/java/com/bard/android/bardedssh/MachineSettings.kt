package com.bard.android.bardedssh

import java.util.UUID

data class MachineSettings(
        var hostname: String,
        var username: String,
        var password: String,
        var port: Int,
        )