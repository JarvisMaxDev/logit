package logit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.lso.logit.LogItAdd

class LogItAddTest : BasePlatformTestCase() {

  @Test
  fun testPosition1() {
    doTest(this.name)
  }

  @Test
  fun testPosition2() {
    doTest(this.name)
  }

  @Test
  fun testPosition3() {
    doTest(this.name)
  }

  @Test
  fun testPosition4() {
    doTest(this.name)
  }

  @Test
  fun testPosition5() {
    doTest(this.name)
  }

  @Test
  fun testPosition6() {
    doTest(this.name)
  }

  @Test
  fun testPosition7() {
    doTest(this.name)
  }

  @Test
  fun testPosition8() {
    doTest(this.name)
  }

  @Test
  fun testPosition9() {
    doTest(this.name)
  }

  @Test
  fun testPosition10() {
    doTest(this.name)
  }

  @Test
  fun testPosition11() {
    doTest(this.name)
  }

  @Test
  fun testPosition12() {
    doTest(this.name)
  }

  @Test
  fun testPosition13() {
    doTest(this.name)
  }

  // selection
  @Test
  fun testPosition14() {
    doTest(this.name)
  }

  // selection
  @Test
  fun testPosition15() {
    doTest(this.name)
  }

  @Test
  fun testPosition16() {
    doTest(this.name)
  }

  @Test
  fun testPosition17() {
    doTest(this.name)
  }

  @Test
  fun testPosition18() {
    doTest(this.name)
  }

  @Test
  fun testPosition19() {
    doTest(this.name)
  }

  @Test
  fun testJavaScriptFile() {
    myFixture.configureByText(
      "script.js",
      """
        function show(value) {
          value<caret>;
        }
      """.trimIndent()
    )

    myFixture.testAction(LogItAdd())

    myFixture.checkResult(
      """
        function show(value) {
          value;
          console.log("=>(script.js:3) <selection>value</selection>", <selection>value</selection>);
        }
      """.trimIndent()
    )
  }

  @Test
  fun testTypeScriptFile() {
    myFixture.configureByText(
      "typed.ts",
      """
        type User = { name: string };
        function show(user: User) {
          user.na<caret>me;
        }
      """.trimIndent()
    )

    myFixture.testAction(LogItAdd())

    checkExactResult(
      """
        type User = { name: string };
        function show(user: User) {
          user.name;
          console.log("=>(typed.ts:4) <selection>user.name</selection>", <selection>user.name</selection>);
        }
      """.trimIndent()
    )
  }

  @Test
  fun testCaretAtEndOfFile() {
    myFixture.configureByText("end.js", "const value = 1;<caret>")

    myFixture.testAction(LogItAdd())

    myFixture.checkResult(
      """
        const value = 1;
        console.log("=>(end.js:2) <selection>value</selection>", <selection>value</selection>);
      """.trimIndent()
    )
  }

  @Test
  fun testEmptyFileDoesNothing() {
    myFixture.configureByText("empty.js", "<caret>")

    myFixture.testAction(LogItAdd())

    myFixture.checkResult("<caret>")
  }

  private fun doTest(name: String) {
    val source = requireNotNull(javaClass.getResource("/testdata/$name")).readText()
    val expected = requireNotNull(javaClass.getResource("/testdata/$name.result"))
      .readText()
      .replace("=>($name:", "=>($name.js:")

    myFixture.configureByText("$name.js", source)

    myFixture.testAction(LogItAdd())

    checkLegacyResult(expected)
  }

  private fun checkLegacyResult(expected: String) {
    checkResult(expected, ::normalizeIndentation)
  }

  private fun checkExactResult(expected: String) {
    checkResult(expected) { it }
  }

  private fun checkResult(expected: String, normalize: (String) -> String) {
    val marker = "<selection>(.*?)</selection>|<caret>".toRegex()
    val expectedSelections = marker.findAll(expected)
      .map { it.groups[1]?.value ?: "" }
      .toList()
    val expectedText = marker.replace(expected, "\$1")
    val actualSelections = myFixture.editor.caretModel.allCarets
      .map { it.selectedText ?: "" }

    assertEquals(normalize(expectedText), normalize(myFixture.editor.document.text))
    assertEquals(expectedSelections, actualSelections)
  }

  private fun normalizeIndentation(text: String): String {
    return text.lines().joinToString("\n") { it.trimStart() }
  }
}
