import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

object ProgressPrinter {
    fun setProgressRingVisible(visible: Boolean) {
        (document.getElementById("progressRing") as HTMLDivElement).hidden = !visible
    }
}