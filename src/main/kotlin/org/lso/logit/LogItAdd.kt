package org.lso.logit

import com.intellij.lang.javascript.psi.JSIfStatement
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import org.lso.logit.settings.LogItSettings


class LogItAdd : AnAction("Insert log") {
  override fun actionPerformed(e: AnActionEvent) {
    // Editor is known to exist from update, so it's not null
    val editor = e.getData(CommonDataKeys.EDITOR)
    editor ?: throw IllegalStateException("Editor cannot be null")
    val actionManager = EditorActionManager.getInstance()
    val startNewLineHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_START_NEW_LINE)

    val vFile: VirtualFile? = e.getData(PlatformDataKeys.VIRTUAL_FILE)

    val variableName = moveCursorToInsertionPoint(editor)
    val logVar = variableName?.trim()

    val pattern = LogItSettings.instance.pattern.run {
      replace("{FN}", vFile?.name ?: "filename").replace("{FP}", vFile?.path ?: "file_path")
        .replace("{LN}", (editor.caretModel.currentCaret.logicalPosition.line + 2).toString())
    }

    val insertionPositions = "\\$\\$".toRegex().findAll(pattern)
      .map { it.range.first }
      .toList()

    val lineToInsert = if (logVar == "\n") {
      "\n${pattern.replace("$$", "")}"
    } else
      pattern.replace("$$", "$logVar")

    variableName?.let {
      val line2insert = lineToInsert.replace("<CR>", "")

      val runnable = {
        if (variableName != "") {
          startNewLineHandler.execute(editor, editor.caretModel.primaryCaret, e.dataContext)
        }

        val offset = editor.caretModel.currentCaret.offset
        editor.document.insertString(offset, line2insert)
      }
      WriteCommandAction.writeCommandAction(editor.project)
        .withName("Insert LogIt log")
        .run<RuntimeException> { runnable() }

      positionCaret(editor, insertionPositions, line2insert, variableName.replace("<CR>", "").trim())
    }
  }

  private fun positionCaret(editor: Editor, insertionPositions: List<Int>, lineToInsert: String, variableName: String) {
    val offset = editor.caretModel.currentCaret.offset
    val logicalPosition = editor.offsetToLogicalPosition(offset)

    editor.caretModel.caretsAndSelections =
      listOf(
        CaretState(
          LogicalPosition(
            logicalPosition.line,
            logicalPosition.column + insertionPositions[0]
          ),
          LogicalPosition(
            logicalPosition.line,
            logicalPosition.column + insertionPositions[0]
          ),
          LogicalPosition(
            logicalPosition.line,
            logicalPosition.column + insertionPositions[0] + variableName.length
          )
        ),
        CaretState(
          LogicalPosition(
            logicalPosition.line,
            logicalPosition.column + insertionPositions[1] + variableName.length - 2
          ),
          LogicalPosition(
            logicalPosition.line,
            logicalPosition.column + insertionPositions[1] + variableName.length - 2
          ),
          LogicalPosition(
            logicalPosition.line,
            logicalPosition.column + insertionPositions[1] + variableName.length * 2 - 2
          )
        )
      )
    //println(editor.caretModel.caretsAndSelections)
  }

  /**
   * search for the cursor insertion point
   * return the name of the element to log
   */
  private fun moveCursorToInsertionPoint(
    editor: Editor
  ): String? {
    val project = editor.project ?: return null
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    psiDocumentManager.commitDocument(editor.document)
    val psiFile = psiDocumentManager.getPsiFile(editor.document) ?: return null

    val valueToLog: String
    val element: PsiElement?
    val offset: Int

    if (editor.selectionModel.hasSelection()) {
      val value = editor.selectionModel.selectedText

      offset = editor.selectionModel.selectionStart

      element = psiFile.findElementAt(offset)
        ?: psiFile.findElementAt((offset - 1).coerceAtLeast(0))

      valueToLog = value ?: "<CR>"
    } else {
      offset = editor.caretModel.currentCaret.offset

      val elementAtCursor = psiFile.findElementAt(offset)
        ?: psiFile.findElementAt((offset - 1).coerceAtLeast(0))
        ?: return null

      if (elementAtCursor.text.replace(" ", "").endsWith("\n\n")) return ""

      element = findElementToLogForSelection(elementAtCursor)

      valueToLog = element?.text?.replace(" ", "") ?: "<CR>"
    }

    val elementAtOffset = element
      ?: psiFile.findElementAt(offset)
      ?: psiFile.findElementAt((offset - 1).coerceAtLeast(0))
      ?: return null

    if (valueToLog.startsWith("\n") && !elementAtOffset.hasParentOfType("JS:OBJECT_LITERAL", 2)) {
      return "\n"
    }

    val block = findBlockForElement(elementAtOffset)

    when {
      block is JSIfStatement -> {
        // for "if" statements insert line above
        editor.caretModel.moveToOffset(block.prevSibling.textRange.startOffset - 1)
      }
      block != null -> editor.caretModel.moveToOffset(block.textRange.endOffset)
    }

    return valueToLog
  }

  /**
   * when the cursor is on a loggable identifier
   */
  private fun findElementToLogForSelection(
    element: PsiElement
  ): PsiElement? {

    val elementType = element.node.elementType.toString()
    val parentElementType = element.parent.node.elementType.toString()
    val qualifiedReference = element.parent as? JSReferenceExpression
    when {
      elementType == "JS:IDENTIFIER" && qualifiedReference?.qualifier != null -> return qualifiedReference
      elementType == "WHITE_SPACE" && element.text.replace(" ", "").startsWith("\n\n") -> return null
      element.prevSibling != null
        && element.prevSibling.node.elementType.toString() == "JS:DOT"
      -> return findElementToLogForSelection(element.parent)

      (elementType != "JS:IDENTIFIER"
        && elementType != "JS:REFERENCE_EXPRESSION"
        && elementType != "JS:BINARY_EXPRESSION")
        || (parentElementType == "JS:REFERENCE_EXPRESSION" && elementType != "JS:IDENTIFIER")
        || parentElementType == "JS:PROPERTY"
      -> {
        val block = findBlockForElement(element)
        return when {
          element.text.trim(' ') == "\n" && (element.prevSibling?.lastChild?.text == ";") -> null
          block?.text?.trim() == "{" -> null
          block?.node?.elementType.toString() == "JS:IF_STATEMENT" -> element.prevSibling?.let {
            findElementToLogForSelection(
              element.prevSibling
            )
          } ?: element
          else -> findElementToLogForBlock(block)
        }
      }

      elementType == "JS:IDENTIFIER" && parentElementType == "JS:VARIABLE" -> return findElementToLogForBlock(
        element
      )
      elementType == "JS:REFERENCE_EXPRESSION"
        && parentElementType != "JS:BINARY_EXPRESSION" -> {
        return findElementToLogForSelection(element.parent)
      }

      (elementType == "JS:IDENTIFIER"
        && !element.hasParentOfType("JS:ARGUMENT_LIST", 2)
        && element.hasParentOfType("JS:CALL_EXPRESSION", 2))
        && element.prevSibling == null -> return null
    }

    return element
  }

  /**
   * find the element to log inside a given block
   */
  private fun findElementToLogForBlock(element: PsiElement?): PsiElement? {
    element ?: return null
    val elementType = element.node.elementType.toString()
    val parentType = element.parent?.node?.elementType?.toString() ?: "FILE"

    when {
      (elementType == "JS:IDENTIFIER" && parentType != "JS:PROPERTY")
        || elementType == "JS:DEFINITION_EXPRESSION"
        || (elementType == "JS:REFERENCE_EXPRESSION" && parentType == "JS:REFERENCE_EXPRESSION")
      -> return element
      elementType == "JS:VARIABLE" -> return element.firstChild
      elementType == "JS:CALL_EXPRESSION" -> return null
    }

    if (element.firstChild == null) {
      return findElementToLogForBlock(element.nextSibling)
    }

    return findElementToLogForBlock(element.firstChild)
  }

  /**
   * find the block containing this element
   */
  private fun findBlockForElement(element: PsiElement?): PsiElement? {
    val current = element ?: return null
    val elementType = current.node?.elementType?.toString() ?: return null
    val parent = current.parent ?: return null
    val parentElementType = parent.node?.elementType?.toString() ?: "FILE"

    when {
      (elementType == "JS:EXPRESSION_STATEMENT" && parentElementType != "FILE") -> return current
      elementType == "JS:VAR_STATEMENT" -> return current
      elementType == "JS:IF_STATEMENT" -> return current

      current.text.trim(' ') == "{" -> return current
      current.text.trim(' ') == "\n" -> return findBlockForElement(current.prevSibling ?: parent)
    }

    return findBlockForElement(parent)
  }

  private fun PsiElement.hasParentOfType(type: String, maxRecursion: Int, recursionLevel: Int = 0): Boolean {
    val parent = parent ?: return false
    val parentType = parent.node?.elementType?.toString() ?: return false

    return if (parentType == type) {
      true
    } else {
      return if (parentType != "FILE" && recursionLevel < maxRecursion)
        parent.hasParentOfType(type, maxRecursion, recursionLevel + 1)
      else false
    }
  }
}
