package org.lso.logit

import com.intellij.find.FindModel
import com.intellij.find.FindUtil
import com.intellij.find.replaceInProject.ReplaceInProjectManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.lso.logit.settings.LogItSettings


class LogItRemove : AnAction("Remove LogIt's Logs") {
  override fun actionPerformed(e: AnActionEvent) {
    // display the dialog
    val dlg = LogItRemoveDlg()
    if (!dlg.showAndGet()) return

    val project = e.getData(CommonDataKeys.PROJECT)!!
    val editor = e.getData(CommonDataKeys.EDITOR)
    editor ?: throw IllegalStateException("Editor cannot be null")

    when (dlg.scope) {
      Scope.CURRENT_FILE -> removeFromCurrentFile(project, editor)
      Scope.PROJECT -> {
        ReplaceInProjectManager.getInstance(project).replaceInPath(createFindModel())
      }
    }
  }

  internal fun removeFromCurrentFile(project: Project, editor: Editor) {
    FindUtil.replace(project, editor, 0, createFindModel())
  }

  private fun createFindModel(): FindModel {
    val patternToReplace = ".*" + LogItSettings.instance.pattern.run {
      replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("^", "\\^")
        .replace("+", "\\+")
        .replace("?", "\\?")
        .replace("|", "\\|")
        .replace(".", "\\.")
        .replace("*", "\\*")
        .replace("$$", ".*")
        .replace("{FN}", ".*")
        .replace("{FP}", ".*")
        .replace("{LN}", "\\d*")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("$", "\\$")
    } + "\n"

    return FindModel().apply {
      stringToFind = patternToReplace
      stringToReplace = ""
      isPromptOnReplace = false
      isRegularExpressions = true
      isGlobal = true
    }
  }
}
