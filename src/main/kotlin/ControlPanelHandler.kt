import chrome.history.search
import kotlinjs.common.jsonAs
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.*
import kotlin.js.Date

object ControlPanelHandler {
    private var searchRunning = false

    fun initSearchCriteria() {
        initDatepicker()

        setSearchTitleEditorCallback()
        setSearchButtonCallback()
        setSearchPeriodCallback()
        setExpandAllCheckButtonCallback()
        setDeleteButtonCallback()
        setGotoTopButtonCallback()
        trackingScroll()
    }

    private fun initDatepicker() {
//        document.getElementById("datepickerFrom")?.let { inputElement ->
//            (inputElement as HTMLInputElement).value = "1970-01-01"
//        }

        document.getElementById("datepickerTo")?.let { inputElement ->
            val dateElement = Date().toLocaleDateString("ko-KR", jsonAs<Date.LocaleOptions>().apply {
                year = "numeric"
                month = "2-digit"
                day = "2-digit"
            }).split(". ", ".")

            (inputElement as HTMLInputElement).value = "${dateElement[0]}-${dateElement[1]}-${dateElement[2]}"
        }
    }

    private fun getSearchTitle(): String {
        return (document.getElementById("searchTitle") as HTMLInputElement?)?.value ?: ""
    }

    private fun getSearchBookmarksStatus(): Int {
        return (document.getElementById("bookmarkStatus") as HTMLSelectElement?)?.selectedIndex ?: 0
    }

    private fun getSearchPeriodFrom(): Date? {
        document.getElementById("datepickerFrom")?.let { inputElement ->
            val inputValue = (inputElement as HTMLInputElement).value
            if(inputValue.isNotEmpty()) {
                return Date("$inputValue 00:00:00")
            }
        }
        return null
    }

    private fun getSearchPeriodTo(): Date? {
        document.getElementById("datepickerTo")?.let { inputElement ->
            val inputValue = (inputElement as HTMLInputElement).value
            if(inputValue.isNotEmpty()) {
                return Date("$inputValue 23:59:59")
            }
        }
        return null
    }

    fun setCountLabel(searchCount: Int, checkedCount: Int, totalCount: Int) {
        (document.getElementById("check-count") as HTMLLabelElement?)?.innerHTML = "$checkedCount "
        (document.getElementById("search-count") as HTMLLabelElement?)?.innerHTML = "$searchCount "
        (document.getElementById("total-count") as HTMLLabelElement?)?.innerHTML = "$totalCount "
    }

    private fun setSearchTitleEditorCallback() {
        (document.getElementById("searchTitle") as HTMLInputElement?)?.onkeyup = {
            if(it.keyCode == 13) onClickSearchButton()
        }
    }

    private fun setSearchButtonCallback() {
        (document.getElementById("searchButton") as HTMLButtonElement?)?.onclick = {
            onClickSearchButton()
        }
    }

    private fun onClickSearchButton() {
        if(!searchRunning) {
            GlobalScope.launch {
                searchRunning = true

                val title = getSearchTitle()
                val status = getSearchBookmarksStatus()
                val periodFrom = getSearchPeriodFrom()
                val periodTo = getSearchPeriodTo()

                BookmarksHandler.displayDirectories(SearchCriteria(title, status, periodFrom, periodTo))
                setCountLabel(BookmarksHandler.getSearchCount(), BookmarksHandler.getCheckedCount(), BookmarksHandler.getTotalCount())
                console.log("search clicked")

                searchRunning = false
            }
        }
    }

    private fun setSearchPeriodCallback() {
        var fromPicker = document.getElementById("datepickerFrom") as HTMLInputElement?
        var toPicker = document.getElementById("datepickerTo") as HTMLInputElement?

        (document.getElementById("periodFull") as HTMLButtonElement?)?.onclick = {
            fromPicker?.let {
                it.value = ""
            }

            toPicker?.let {
                val dateElement = Date().toLocaleDateString("ko-KR", jsonAs<Date.LocaleOptions>().apply {
                    year = "numeric"
                    month = "2-digit"
                    day = "2-digit"
                }).split(" .", ".")

                it.value = "${dateElement[0]}-${dateElement[1]}-${dateElement[2]}"
            }
        }
    }

    private fun setExpandAllCheckButtonCallback() {
        (document.getElementById("expendAllChk") as HTMLInputElement?)?.let { checkInput ->
            checkInput.onclick = {
                GlobalScope.launch {
                    BookmarksHandler.expandBookmarks(checkInput.checked)
                    setCountLabel(BookmarksHandler.getSearchCount(), BookmarksHandler.getCheckedCount(), BookmarksHandler.getTotalCount())
                }
            }
        }
    }

    private fun setDeleteButtonCallback() {
        (document.getElementById("deleteBtn") as HTMLButtonElement?)?.onclick = {
            GlobalScope.launch {
                BookmarksHandler.actionCheckedBookmark(BookmarksHandler::removeBookmark)
                setCountLabel(BookmarksHandler.getSearchCount(), BookmarksHandler.getCheckedCount(), BookmarksHandler.getTotalCount())
                console.log("delete button clicked")
            }
        }
    }

    private fun setGotoTopButtonCallback() {
        (document.getElementById("topBtn") as HTMLButtonElement?)?.onclick = {
            val bodyContents = document.getElementById("bookmarks") as HTMLDivElement
            bodyContents.scrollTop = 0.0
            console.log("go to top botton clicked")
        }
    }

    private fun trackingScroll() {
        val topButton = document.getElementById("topBtn") as HTMLButtonElement
        val bodyContents = document.getElementById("bookmarks") as HTMLDivElement

        bodyContents.onscroll = {
            if(bodyContents.scrollTop > 20) {
                topButton.style.display = "block"
            }
            else {
                topButton.style.display = "none"
            }
        }
    }
}