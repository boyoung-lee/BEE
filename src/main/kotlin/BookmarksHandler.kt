import Utils.pmap
import chrome.bookmarks.BookmarkTreeNode
import chrome.tabs.CreateProperties
import kotlinjs.common.jsonAs
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object BookmarksHandler {
    private var bookmarkNodes: ArrayList<BookmarkNode> = ArrayList()
    private var searchCount: Int = 0
    private var checkedCount: Int = 0
    private var totalCount: Int = 0
    private val rootNodesID = arrayOf("0", "1", "2")

    private var directoryCount: Int = 0
    private var openedDirectoryCount: Int = 0

    suspend fun getChromeBookmarkTree() {
        var response = suspendCoroutine { cont: Continuation<Array<BookmarkTreeNode>> ->
            chrome.bookmarks.getTree {
                cont.resume(it)
            }
        }
        getBookmearkList(response)
        console.log(bookmarkNodes)
    }

    fun expandBookmarks(isOpened: Boolean) {
        expandBookmarks(bookmarkNodes, isOpened)
    }

    fun getSearchCount(): Int {
        return searchCount
    }

    fun getCheckedCount(): Int {
        return checkedCount
    }

    fun getTotalCount(): Int {
        return totalCount
    }

    private fun expandBookmarks(bookmarkNodes: ArrayList<BookmarkNode>,isOpened: Boolean) {
        bookmarkNodes.forEach { bookmarkNode ->
            val sublistId = HtmlIDs.ID_PREFIX_BOOKMARK_SUBLIST + bookmarkNode.id
            bookmarkNode.isOpened = isOpened
            val bookmarkList = document.getElementById(sublistId) as HTMLElement?
            if(bookmarkList != null) {
                bookmarkList.hidden = !isOpened
            }
            bookmarkNode.children?.let{
                expandBookmarks(it, isOpened)
            }
        }
    }

    fun actionCheckedBookmark(actionFun: (bookmarkNodes: ArrayList<BookmarkNode>) -> Unit) {
        val queue = Queue()
        val selectedBookmarks : ArrayList<BookmarkNode> = ArrayList()

        bookmarkNodes[0].children?.let {
            queue.add(bookmarkNodes[0])

            while(queue.peek() != null) {
                var bookmarkNode = queue.poll()

                bookmarkNode?.let{ bookmark ->
                    if(bookmark.isSelected) {
                        selectedBookmarks.add(bookmark)
                        bookmark.isSelected = false
                    }

                    bookmarkNode.children?.forEach { child ->
                        queue.add(child)
                    }
                }
            }
        }
        actionFun(selectedBookmarks)
    }

    fun removeBookmark(bookmarkNodes: ArrayList<BookmarkNode>) {
        bookmarkNodes.asReversed().forEach { bookmarkNode ->

            if(!rootNodesID.contains(bookmarkNode.id)) {
                // Remove from chrome
                chrome.bookmarks.remove(bookmarkNode.id)

                // Remove form ParentNode
                bookmarkNode.parentNode?.let {
                    it.children?.remove(bookmarkNode)
                }
            }

            // Remove from ParantNode
            bookmarkNode.parentNode?.let {
                it.checkedChild--
                it.visibilityChild--
            }

            bookmarkNode.url?.let {
                searchCount--
                totalCount--
                checkedCount--
            }

            // Remove from view
            val bookmarkElem = HtmlUtils.getSpanElement(bookmarkNode.id)
            bookmarkElem?.remove()
        }
    }

    private fun getBookmearkList(nodes: Array<BookmarkTreeNode>) {
        totalCount = 0
        for (bookmark in nodes) {
            bookmarkNodes.add(getBookmarkChildren(bookmark))
        }
    }

    private fun getBookmarkChildren(node: BookmarkTreeNode): BookmarkNode{
        BookmarkNode(node).let { subBookmarkNode ->
            node.children?.forEach { child ->
                var childNode = getBookmarkChildren(child)
                childNode.parentNode = subBookmarkNode
                subBookmarkNode.children?.add(childNode)
            }
            if(node.children == null) totalCount++
            return subBookmarkNode
        }
    }

    suspend fun displayDirectories(searchCriteria: SearchCriteria? = null) {
        (document.getElementById("bookmarks") as HTMLDivElement).let { bookmark ->
            bookmark.innerHTML = ""
            bookmark.hidden = true

            ProgressPrinter.setProgressRingVisible(true)

            searchCount = 0
            checkedCount = 0
            bookmarkNodes[0].children?.let {
                val directoryList = displaySubItems(it, searchCriteria)
                bookmark.appendChild(directoryList)
            }

            ProgressPrinter.setProgressRingVisible(false)
            bookmark.hidden = false
        }
    }

    private suspend fun displaySubItems(bookmarkNodes: ArrayList<BookmarkNode>, searchCriteria: SearchCriteria? = null): HTMLUListElement {
        val list = HtmlUtils.makeUlElement()
        bookmarkNodes.pmap { bookmarkNode ->
            val spanElement: HTMLSpanElement? = if (bookmarkNode.children == null) {
                displayBookmarksItem(bookmarkNode, searchCriteria)
            } else {
                bookmarkNode.visibilityChild= bookmarkNode.children!!.size
                displayDirectoryItem(bookmarkNode, searchCriteria)
            }
            spanElement?.let { list.appendChild(spanElement) }
        }
        return list
    }

    private suspend fun displayDirectoryItem(bookmarkNode: BookmarkNode, searchCriteria: SearchCriteria? = null): HTMLSpanElement? {
        if(bookmarkNode.children == null) {
            return null
        }

        directoryCount++

        val span = HtmlUtils.makeSpanElement()
        val elemId = HtmlIDs.ID_PREFIX_BOOKMARK_ELEM + bookmarkNode.id
        val sublistId = HtmlIDs.ID_PREFIX_BOOKMARK_SUBLIST + bookmarkNode.id
        val iconId = HtmlIDs.ID_PREFIX_BOOKMARK_ICON + bookmarkNode.id
        val img = if (bookmarkNode.isOpened) {
            HtmlUtils.makeImgElement(Resources.ICON_FOLDER_OPEN)
        } else {
            HtmlUtils.makeImgElement(Resources.ICON_FOLDER_CLOSE)
        }
        img.id = iconId

        val anchor = HtmlUtils.makeAnchorElement(bookmarkNode.title, "#")
        val toggleIcon = { isOpened: Boolean ->
            val originIcon = document.getElementById(iconId) as HTMLImageElement
            originIcon.src = if(isOpened) Resources.ICON_FOLDER_CLOSE else Resources.ICON_FOLDER_OPEN
            bookmarkNode.isOpened = !isOpened
        }

        anchor.onclick = {
            bookmarkNode.children?.let {
                val bookmarkList = document.getElementById(sublistId) as HTMLElement?
                if(bookmarkList != null) {
                    bookmarkList.hidden = bookmarkNode.isOpened
                }
                toggleIcon(bookmarkNode.isOpened)
            }

            (document.getElementById("expandAllchk") as HTMLInputElement?)?.let {
                it.checked = (directoryCount == openedDirectoryCount)
            }
        }

        val chkbox = HtmlUtils.makeCheckBoxElement(bookmarkNode.id)
        bookmarkNode.checkedChild = 0
        bookmarkNode.isSelected = false
        chkbox.checked = bookmarkNode.isSelected

        chkbox.onclick = {
            chkbox.checked.let { state ->
                //본인 상태 변경
                bookmarkNode.isSelected = state

                //자식 node isSelected도 다 동일한 상태로 변경
                bookmarkNode.children?.let {
                    checkedCount -= bookmarkNode.checkedChild

                    if(state) {
                        checkedCount += bookmarkNode.visibilityChild
                        bookmarkNode.checkedChild = bookmarkNode.visibilityChild
                    } else
                        bookmarkNode.checkedChild = 0

                    changeChildChkboxState(it, state)
                }

                // 부모 node 상태 변경
                bookmarkNode.parentNode?.let {
                    if (state)
                        changeParentChkbokStateToTrue(it)
                    else
                        changeParentChkbokStateToFalse(it)
                }
            }
            ControlPanelHandler.setCountLabel(getSearchCount(), getCheckedCount(), getTotalCount())
        }

        span.addClass("directory-list")
        span.appendChild(chkbox)
        span.appendChild(img)
        span.appendChild(anchor)

        val br = HtmlUtils.makeBrElement()
        span.appendChild(br)
        span.id = elemId

        if(bookmarkNode.children != null && bookmarkNode.children!!.isNotEmpty()) {
            val bookmarkList = displaySubItems(bookmarkNode.children!!, searchCriteria)
            bookmarkList.id = sublistId
            span.appendChild(bookmarkList)
            if(!HtmlUtils.hasActiveChildElement(bookmarkList.children.asList() as List<HTMLElement>)) {
                span.hidden = true
                bookmarkNode.parentNode?.let {
                    it.visibilityChild--
                }
            }
            if(!bookmarkNode.isOpened) bookmarkList.hidden = true
        }
        return span
    }

    private suspend fun  displayBookmarksItem(bookmarkNode: BookmarkNode, searchCriteria: SearchCriteria? = null): HTMLSpanElement? {
        val elemId = HtmlIDs.ID_PREFIX_BOOKMARK_ELEM + bookmarkNode.id
        val img = HtmlUtils.makeImgElement("chrome://favicon/" + bookmarkNode.url)
        val anchor = HtmlUtils.makeAnchorElement(bookmarkNode.title, bookmarkNode.url)
        anchor.onclick = {
            chrome.tabs.create(jsonAs<CreateProperties>().apply {
                url = bookmarkNode.url
            })
        }

        val chkbox = HtmlUtils.makeCheckBoxElement(bookmarkNode.id)
        bookmarkNode.checkedChild = 0
        bookmarkNode.isSelected = false
        chkbox.checked = bookmarkNode.isSelected

        chkbox.onclick = {
            chkbox.checked.let { state ->
                //본인 상태 변경
                bookmarkNode.isSelected = state

                HtmlUtils.getSpanElement(bookmarkNode.id)?.let {
                    if (!it.hidden) {
                        if (state) checkedCount++
                        else checkedCount--
                    }
                }
                // 부모 node 상태 변경
                bookmarkNode.parentNode?.let {
                    if (state)
                        changeParentChkbokStateToTrue(it)
                    else
                        changeParentChkbokStateToFalse(it)
                }
            }
            ControlPanelHandler.setCountLabel(getSearchCount(), getCheckedCount(), getTotalCount())
        }

        val span = HtmlUtils.makeSpanElement()
        span.addClass("bookmark-list")
        span.appendChild(chkbox)
        span.appendChild(img)
        span.appendChild(anchor)

        var br = HtmlUtils.makeBrElement()
        span.appendChild(br)
        span.id = elemId
        if(isInSearchList(bookmarkNode, searchCriteria))
            searchCount++
        else {
            span.hidden = true

            bookmarkNode.parentNode?.let {
                it.visibilityChild--
            }
        }
        return span
    }


    private suspend fun isInSearchList(bookmarkNode: BookmarkNode, searchCriteria: SearchCriteria?): Boolean {
        if(searchCriteria == null) return true

        console.log("search criteria: ${searchCriteria.periodFrom} ${searchCriteria.periodTo} ${searchCriteria.status}")

        if(bookmarkNode.title.toUpperCase().indexOf(searchCriteria.title.toUpperCase()) == -1) return false

        if(searchCriteria.periodTo != null) {
            bookmarkNode.dateAdded?.let {
                if(it > (searchCriteria.periodTo?.getTime()?.toLong() ?: 0L))
                    return false
            }
        }

        if(searchCriteria.periodFrom != null) {
            bookmarkNode.dateAdded?.let {
                if(it < (searchCriteria.periodFrom?.getTime()?.toLong() ?: 0L))
                    return false
            }
        }

        bookmarkNode.url?.let {
            bookmarkUrl ->
            if(searchCriteria.status == 1 || searchCriteria.status == 2) {
                val result = Utils.checkUrl(bookmarkUrl)
                if(searchCriteria.status == 1 && !result)
                    return false

                if(searchCriteria.status == 2 && result)
                    return false
            }
        }
        return true
    }

    fun changeChildChkboxState(childNodes: ArrayList<BookmarkNode>, state: Boolean) {
        childNodes.forEach { child ->
            val childElem = HtmlUtils.getSpanElement(child.id)

            if(childElem?.hidden == false) {
                val childNodeObj = document.getElementById(child.id) as HTMLInputElement?
                childNodeObj?.checked = state
                child.isSelected = state

                child.children?.let {
                    checkedCount -= child.checkedChild

                    if(state) {
                        //부오에서 자식 수만큼 무조건 빼거나 더했기 때문에 폴더인 자식의 상태가 변했을 경우 Count를 하지 않기 위함
                        if(child.checkedChild != child.visibilityChild) checkedCount--

                        checkedCount += child.visibilityChild
                        child.checkedChild = child.visibilityChild
                    } else {
                        // 부모에서 자식 수만큼 무조건 뺴거나 더했기 떄문에 폴더인 자식의 상태가 변했을 경우 Count를 하지 않기 위함
                        if(child.checkedChild != 0) checkedCount++

                        child.checkedChild = 0
                    }
                    changeChildChkboxState(it, state)
                }
            }
        }
    }

    fun changeParentChkbokStateToFalse(parentBookmarkNode: BookmarkNode) {
        parentBookmarkNode.checkedChild -= 1

        val parentNodeObj = document.getElementById(parentBookmarkNode.id) as HTMLInputElement?

        if(parentNodeObj?.checked == true) {
            parentNodeObj.checked = false
            parentBookmarkNode.isSelected = false
            parentBookmarkNode.parentNode?.let {
                changeParentChkbokStateToFalse(it)
            }
        }
    }

    fun changeParentChkbokStateToTrue(parentBookmarkNode: BookmarkNode) {
        parentBookmarkNode.checkedChild += 1

        if(parentBookmarkNode.visibilityChild == parentBookmarkNode.checkedChild) {
            val parentNodeObj = document.getElementById(parentBookmarkNode.id) as HTMLInputElement?
            if(parentNodeObj?.checked == false) {
                parentNodeObj.checked = true
                parentBookmarkNode.isSelected = true
                parentBookmarkNode.parentNode?.let {
                    changeParentChkbokStateToTrue(it)
                }
            }
        }
    }
}