import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main() {
    document.addEventListener("DOMContentLoaded", {
        displayBookmarkList()
        ControlPanelHandler.initSearchCriteria()
    })
}

fun displayBookmarkList() {
    GlobalScope.launch {
        BookmarksHandler.getChromeBookmarkTree()
        BookmarksHandler.displayDirectories()
        ControlPanelHandler.setCountLabel(BookmarksHandler.getSearchCount(), BookmarksHandler.getCheckedCount(), BookmarksHandler.getTotalCount())
    }
}