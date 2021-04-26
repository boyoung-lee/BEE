import kotlinx.browser.document
import org.w3c.dom.*

object HtmlUtils {
    fun makeSpanElement(): HTMLSpanElement {
        return document.createElement("span") as HTMLSpanElement
    }

    fun makeBrElement(): HTMLBRElement {
        return document.createElement("br") as HTMLBRElement
    }

    fun makeAnchorElement(title: String, url: String?): HTMLAnchorElement {
        var anchor = document.createElement("a") as HTMLAnchorElement
        anchor.setAttribute("href", url ?: "#")
        anchor.text = title
        return anchor
    }

    fun makeImgElement(src: String): HTMLImageElement {
        var img = document.createElement("img") as HTMLImageElement
        img.src = src
        img.width = 15
        return img
    }

    fun makeUlElement(): HTMLUListElement {
        return document.createElement("ul") as HTMLUListElement
    }

    fun makeDivElement(): HTMLDivElement {
        return document.createElement("div") as HTMLDivElement
    }

    fun makeCheckBoxElement(id_: String): HTMLInputElement {
        var checkBox = document.createElement("input") as HTMLInputElement

        checkBox.type = "checkbox"
        checkBox.id = id_

        return checkBox
    }

    fun hasActiveChildElement(elementList: List<HTMLElement>): Boolean {
        return elementList.any { !it.hidden }
    }

    fun getSpanElement(nodeId: String): HTMLElement? {
        val elemId = HtmlIDs.ID_PREFIX_BOOKMARK_ELEM + nodeId

        return document.getElementById(elemId) as HTMLElement?
    }
}