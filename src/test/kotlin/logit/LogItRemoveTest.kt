package logit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.lso.logit.LogItRemove
import org.lso.logit.settings.DEFAULT_LOGIT_PATTERN
import org.lso.logit.settings.LogItSettings

class LogItRemoveTest : BasePlatformTestCase() {

  @Test
  fun testRemoveGeneratedLogsFromCurrentFile() {
    LogItSettings.instance.pattern = DEFAULT_LOGIT_PATTERN
    myFixture.configureByText(
      "remove.js",
      """
        const value = 1;
        console.log("=>(remove.js:2) value", value);
        console.log("manual", value);
        keep(value);
      """.trimIndent()
    )

    LogItRemove().removeFromCurrentFile(project, myFixture.editor)

    myFixture.checkResult(
      """
        const value = 1;
        console.log("manual", value);
        keep(value);
      """.trimIndent()
    )
  }
}
