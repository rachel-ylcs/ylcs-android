package com.yinlin.rachel.tool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

suspend fun <T> withTimeoutIO(timeMillis: Long, block: suspend CoroutineScope.() -> T): T?
    = withContext(Dispatchers.IO) { withTimeoutOrNull(timeMillis, block) }