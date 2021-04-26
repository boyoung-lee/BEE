import chrome.bookmarks.BookmarkTreeNode

class BookmarkNode(bookmarkTreeNode: BookmarkTreeNode)
{
    var index: Number? = bookmarkTreeNode.index
    var dateAdded: Long? = bookmarkTreeNode.dateAdded?.toLong()
    var title: String = bookmarkTreeNode.title
    var url: String? = bookmarkTreeNode.url
    var dateGroupModified: Number? = bookmarkTreeNode.dateGroupModified
    var id: String = bookmarkTreeNode.id
    var parentNode: BookmarkNode? = null
    var children: ArrayList<BookmarkNode>? = if(bookmarkTreeNode.children == null) null else ArrayList()
    var unmodifiable: Any? = bookmarkTreeNode.unmodifiable
    var visibilityChild: Int = 0
    var checkedChild: Int = 0
    var isSelected: Boolean = false
    var isOpened: Boolean = false // for directory status
}