package dev.beefers.vendetta.manager.installer.step.patching

import android.os.Build
import android.content.Context
import androidx.compose.ui.graphics.Color
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceIdentifier
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import dev.beefers.vendetta.manager.R
import dev.beefers.vendetta.manager.installer.step.Step
import dev.beefers.vendetta.manager.installer.step.StepGroup
import dev.beefers.vendetta.manager.installer.step.StepRunner
import dev.beefers.vendetta.manager.installer.step.download.DownloadBaseStep
import dev.beefers.vendetta.manager.installer.utils.ArscUtil
import dev.beefers.vendetta.manager.installer.utils.ArscUtil.addColorResource
import dev.beefers.vendetta.manager.installer.utils.ArscUtil.addResource
import dev.beefers.vendetta.manager.installer.utils.ArscUtil.getMainArscChunk
import dev.beefers.vendetta.manager.installer.utils.ArscUtil.getPackageChunk
import dev.beefers.vendetta.manager.installer.utils.ArscUtil.getResourceFileName
import dev.beefers.vendetta.manager.installer.utils.AxmlUtil
import dev.beefers.vendetta.manager.utils.getResBytes
import org.koin.core.component.inject

/**
 * Replaces the existing app icons with Vendetta tinted ones
 */
class ReplaceIconStep : Step() {

    val context: Context by inject()

    override val group = StepGroup.PATCHING
    override val nameRes = R.string.step_change_icon

    override suspend fun run(runner: StepRunner) {
        val baseApk = runner.getCompletedStep<DownloadBaseStep>().workingCopy
        val arsc = ArscUtil.readArsc(baseApk)

        val iconRscIds = AxmlUtil.readManifestIconInfo(baseApk)
        val squareIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.squareIcon, "anydpi-v26")
        val roundIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.roundIcon, "anydpi-v26")

        val filePathIdx = arsc.getMainArscChunk().stringPool
            .addString("res/ic_pyoncord_monochrome.xml")

        val monochromeIcon = arsc.getPackageChunk().addResource(
            typeName = "drawable",
            resourceName = "ic_pyoncord_monochrome",
            configurations = { it.isDefault },
            valueType = BinaryResourceValue.Type.STRING,
            valueData = filePathIdx,
        )

        val backgroundIcon = arsc.getPackageChunk()
            .addColorResource("pyoncord", Color(0xFF48488B))

        for (rscFile in setOf(squareIconFile, roundIconFile)) { // setOf to not possibly patch same file twice
            AxmlUtil.patchAdaptiveIcon(
                apk = baseApk,
                resourcePath = rscFile,
                foregroundIcon = null,
                backgroundColor = backgroundIcon,
                monochromeIcon = monochromeIcon,
            )
        }

        ZipWriter(baseApk, /* append = */ true).use {
            it.writeEntry(
                "res/ic_pyoncord_monochrome.xml", 
                context.getResBytes(R.drawable.ic_discord_monochrome))

            it.deleteEntry("resources.arsc")
            it.writeEntry("resources.arsc", arsc.toByteArray())
        }
    }

}
