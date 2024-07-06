package com.github.projectaj14.fluttertestsassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.Icon

class CreateFlutterTestFileAction : AnAction(), DumbAware {

    companion object {
        private val ICON: Icon = IconLoader.getIcon("/icons/any_type.png", CreateFlutterTestFileAction::class.java)
    }

    init {
        templatePresentation.icon = ICON
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.icon = ICON
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: run {
            showError("No project found")
            return
        }
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: run {
            showError("No file selected")
            return
        }

        if (!file.name.endsWith(".dart")) {
            showError("Selected file is not a Dart file")
            return
        }

        val libFile = findLibFile(file) ?: run {
            showError("Selected file is not in the 'lib' directory")
            return
        }

        val testFilePath = createTestFilePath(file, libFile)

        val testFile = File(testFilePath)
        if (testFile.exists()) {
            openExistingFile(project, testFilePath)
        } else {
            createAndOpenTestFile(project, testFilePath, file.name)
        }
    }

    private fun findLibFile(file: VirtualFile): VirtualFile? {
        var current: VirtualFile? = file.parent
        while (current != null) {
            if (current.name == "lib") {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun createTestFilePath(file: VirtualFile, libFile: VirtualFile): String {
        val projectDir = libFile.parent.path;
        val relativePath = file.path.substringAfter(libFile.path)

        val testDir = File(projectDir, "test")
        if (!testDir.exists() && !testDir.mkdirs()) {
            showError("Failed to create 'test' directory")
            return ""
        }

        val relativeDir = relativePath.substringBeforeLast('/', "")
        val fileName = relativePath.substringAfterLast('/')
        val testFileName = "${fileName.substringBeforeLast('.')}_test.dart"

        val fullTestDir = File(testDir, relativeDir)
        if (!fullTestDir.exists() && !fullTestDir.mkdirs()) {
            showError("Failed to create test subdirectories")
            return ""
        }

        return File(fullTestDir, testFileName).absolutePath
    }

    private fun createAndOpenTestFile(project: Project, testFilePath: String, originalFileName: String) {
        val testFile = File(testFilePath)
        testFile.createNewFile()
        val testFileContent = """
            import 'package:flutter_test/flutter_test.dart';
            
            void main() {
              test('TODO: Implement tests for $originalFileName', () {
                // TODO: Implement test
              });
            }
        """.trimIndent()
        testFile.writeText(testFileContent)

        openExistingFile(project, testFilePath)
    }

    private fun openExistingFile(project: Project, filePath: String) {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)?.let { virtualFile ->
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        } ?: showError("Failed to open the test file")
    }

    private fun showError(message: String) {
        Messages.showErrorDialog(message, "Flutter Test File Creation Error")
    }
}