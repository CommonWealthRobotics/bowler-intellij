package com.commonwealthrobotics.bowlerintellij.plugin

import com.commonwealthrobotics.bowlerintellij.testutil.KoinTestFixture
import com.commonwealthrobotics.bowlerkernel.kerneldiscovery.NameClient
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Timeout(value = 1, unit = TimeUnit.MINUTES)
internal class DefaultBowlerKernelFacadeTest : KoinTestFixture() {

    private lateinit var kernelServerFacade: KernelServerFacade

    @BeforeEach
    fun beforeEach() {
        check(NameClient.scan().isEmpty())
        kernelServerFacade = KernelServerFacade(
            "adhjkshkjda",
            Paths.get(
                "/home/salmon/Documents/bowler-kernel/cli/build/install/cli/bin/bowler-kernel"
            )
        )

        kernelServerFacade.ensureStarted()
        Thread.sleep(2000)
        while (NameClient.scan().isEmpty()) {
            Thread.sleep(1000)
        }
    }

    init {
        additionalAfterEach {
            kernelServerFacade.ensureStopped()
        }
    }

    @Test
    fun `run script`() {
        initKoin(module {
            single<KernelConnectionManager> {
                DefaultKernelConnectionManager(
                    CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())
                ).apply {
                    val kernel = NameClient.scan().first()
                    connect(kernel.b, NameClient.getGrpcPort(kernel.b).unsafeRunSync())
                }
            }
        })

        val facade = DefaultBowlerKernelFacade(testLocalKoin.getKoin().get())
        val config = mockk<BowlerScriptRunConfiguration> {
            every { getScriptFile() } returns File("/home/salmon/Documents/bowler-kernel-test-repo/scriptA.groovy")
            every { projectDir } returns File("/home/salmon/Documents/bowler-kernel-test-repo")
        }

        repeat(10) {
            val result = facade.runScript(config)
            result.processHandler.startNotify()
            result.processHandler.waitFor()
        }
    }
}
