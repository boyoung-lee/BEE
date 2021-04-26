import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val FETCH_TIMEOUT = 10000 // 10 sec

object Utils {
    suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
        map { async { f(it) } }.awaitAll()
    }

    suspend fun checkUrl(url: String): Boolean {
        var result = suspendCoroutine { cont: Continuation<Boolean> ->
            var urlChecker = XMLHttpRequest()
            // Open the file and specifies the method (get)
            // Asynchronous is true
            urlChecker.timeout = FETCH_TIMEOUT
            urlChecker.open("GET", url, true)
            // check each time th ready state changes
            // to see if the object is ready
            urlChecker.onreadystatechange = {
                if(urlChecker.readyState == XMLHttpRequest.DONE) {
                    // check to see whether request for the file failed or secceeded
                    console.log("urlCheckt.status ${urlChecker.status}")
                    if((urlChecker.status.toInt() == 200)) {
                        cont.resume(true)
                    }
                    else if((urlChecker.status.toInt() == 404) || (urlChecker.status.toInt() == 429)) {
                        cont.resume(false)
                    }
                }
            }
            urlChecker.ontimeout = {
                console.log("ontimeout")
                cont.resume(false)
            }
            urlChecker.onabort = {
                console.log("onabort")
                cont.resume(false)
            }
            urlChecker.onerror = {
                console.log("onerror")
                cont.resume(false)
            }
            urlChecker.setRequestHeader("Access-Control-Allow-Origin", "*")
            urlChecker.send(null)
        }
        console.log("check $url return $result")
        return result
    }
}

class Queue {
    var list = mutableListOf<BookmarkNode>()

    fun add(item: BookmarkNode) {
        list.add(item)
    }

    fun poll(): BookmarkNode? {
        if(!isEmpty()) {
            return list.removeAt(0)
        }
        return null
    }

    fun peek(): BookmarkNode? {
        if(!list.isEmpty()) {
            return list[0]
        }
        return null
    }

    fun isEmpty(): Boolean {
        return list.size == 0
    }

    fun size(): Int {
        return list.size
    }
}