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

import okhttp3.internal.addIfAbsent
import okhttp3.internal.notify
import okhttp3.internal.threadFactory
import okhttp3.internal.waitNanos
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A set of worker threads that are shared among a set of task queues.
 *
 * The task runner is responsible for managing non-daemon threads. It keeps the process alive while
 * user-visible (ie. non-daemon) tasks are scheduled, and allows the process to exit when only
 * housekeeping (ie. daemon) tasks are scheduled.
 *
 * The task runner is also responsible for releasing held threads when the library is unloaded.
 * This is for the benefit of container environments that implement code unloading.
 *
 * Most applications should share a process-wide [TaskRunner] and use queues for per-client work.
 */
class TaskRunner(
  private val executor: Executor = ThreadPoolExecutor(
      0, // corePoolSize.
      Int.MAX_VALUE, // maximumPoolSize.
      60L, TimeUnit.SECONDS, // keepAliveTime.
      SynchronousQueue(),
      threadFactory("OkHttp", true)
  )
) {
  // All state in all tasks and queues is guarded by this.

  private var promoterRunning = false
  private val activeQueues = mutableListOf<TaskQueue>()
  private val promoter = Runnable { promote() }

  fun newQueue(owner: Any) = TaskQueue(this, owner)

  /**
   * Returns a snapshot of queues that currently have tasks scheduled. The task runner does not
   * necessarily track queues that have no tasks scheduled.
   */
  fun activeQueues(): List<TaskQueue> {
    synchronized(this) {
      return activeQueues.toList()
    }
  }

  internal fun kickPromoter(queue: TaskQueue) {
    check(Thread.holdsLock(this))

    if (queue.isActive()) {
      activeQueues.addIfAbsent(queue)
    } else {
      activeQueues.remove(queue)
    }

    if (promoterRunning) {
      notify()
    } else {
      promoterRunning = true
      executor.execute(promoter)
    }
  }

  private fun promote() {
    synchronized(this) {
      while (true) {
        val now = System.nanoTime()
        val delayNanos = promoteQueues(now)

        if (delayNanos == -1L) {
          promoterRunning = false
          return
        }

        try {
          waitNanos(delayNanos)
        } catch (_: InterruptedException) {
          // Will cause the thread to exit unless other connections are created!
          cancelAll()
        }
      }
    }
  }

  /**
   * Start executing the next available tasks for all queues.
   *
   * Returns the delay until the next call to this method, -1L for no further calls, or
   * [Long.MAX_VALUE] to wait indefinitely.
   */
  private fun promoteQueues(now: Long): Long {
    var result = -1L

    for (queue in activeQueues) {
      val delayNanos = queue.promote(now, executor)
      if (delayNanos == -1L) continue
      result = if (result == -1L) delayNanos else minOf(result, delayNanos)
    }

    return result
  }

  private fun cancelAll() {
    for (queue in activeQueues) {
      queue.cancelAll()
    }
  }
}
