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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.LinkedBlockingDeque

class TaskRunnerTest {
  @Test fun execute() {
    val log = LinkedBlockingDeque<String>()
    val taskRunner = TaskRunner()

    val queueA = taskRunner.newQueue("a")
    val queueB = taskRunner.newQueue("b")

    queueA.schedule(object : Task("task A", false) {
      override fun runOnce(): Long {
        log.put("1")
        return -1L
      }
    }, 0L)

    queueB.schedule(object : Task("task B", false) {
      override fun runOnce(): Long {
        log.put("2")
        return -1L
      }
    }, 100L)

    assertThat(log.take()).isEqualTo("1")
    assertThat(log.take()).isEqualTo("2")
  }
}
