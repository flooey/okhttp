/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.internal.concurrent

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

open class RecordingExecutor : AbstractExecutorService() {
  var shutdown: Boolean = false
  val calls = mutableListOf<Runnable>()

  override fun execute(command: Runnable) {
    if (shutdown) throw RejectedExecutionException()
    calls.add(command)
  }

  override fun shutdown() {
    shutdown = true
  }

  override fun shutdownNow(): List<Runnable> {
    throw UnsupportedOperationException()
  }

  override fun isShutdown(): Boolean {
    throw UnsupportedOperationException()
  }

  override fun isTerminated(): Boolean {
    throw UnsupportedOperationException()
  }

  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
    throw UnsupportedOperationException()
  }
}
