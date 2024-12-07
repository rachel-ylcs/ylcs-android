package com.yinlin.rachel.tool

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.annotation.IOThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

@IOThread
suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO, block)

@IOThread
suspend fun <T> withTimeoutIO(timeMillis: Long, block: suspend CoroutineScope.() -> T): T? =
    withContext(Dispatchers.IO) { withTimeoutOrNull(timeMillis, block) }

@IOThread
fun LifecycleOwner.startIO(block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launch { withContext(Dispatchers.IO, block) }

@IOThread
fun <T> LifecycleOwner.startIOWithResult(ioBlock: suspend CoroutineScope.() -> T, mainBlock: suspend CoroutineScope.(T) -> Unit) =
    lifecycleScope.launch { mainBlock(withContext(Dispatchers.IO, ioBlock)) }