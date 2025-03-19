/*
 * Copyright (C) 2024 Shubham Panchal
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

package io.shubham0204.smollmandroid

import android.app.Application
import com.smith.smith_rag.di.SmithRagModule
import io.shubham0204.smollmandroid.data.ObjectBoxStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class SmolChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SmolChatApplication)
            modules(
                listOf(
                    KoinAppModule().module,
                    SmithRagModule().module // Changed to access the generated module
                ))
        }
        ObjectBoxStore.init(this)
        com.smith.smith_rag.data.ObjectBoxStore.init(this)
    }
}
