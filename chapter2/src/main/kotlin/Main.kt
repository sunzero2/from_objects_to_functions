import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    val items = listOf("write chapter", "insert code", "draw diagrams")
    val toDoList = ToDoList(ListName("book"), items.map(::ToDoItem))
    val lists = mapOf(User("uberto") to listOf(toDoList))
    val app: HttpHandler = Zettai(lists)
    app.asServer(Jetty(8080)).start()
    println("Server started at http://localhost:8080/todo/uberto/book")
}

data class Zettai(val lists: Map<User, List<ToDoList>>) : HttpHandler {
    val routes = routes(
        "/todo/{user}/{list}" bind Method.GET to ::showList
    )

    override fun invoke(request: Request): Response = routes(request)
    private fun showList(req: Request): Response = req.let(::extractListData)
        .let(::fetchListContent)
        .let(::renderHtml)
        .let(::createResponse)

    // 요청에서 사용자 이름과 목록 이름을 뽑아낸다.
    fun extractListData(request: Request): Pair<User, ListName> {
        val user = request.path("user").orEmpty()
        val list = request.path("list").orEmpty()
        return User(user) to ListName(list)
    }

    // 실제 사용자 이름과 목록 이름을 키로 사용해 저장소에서 목록 데이터를 가져온다.
    fun fetchListContent(listId: Pair<User, ListName>): ToDoList = lists[listId.first]
        ?.firstOrNull { it.listName == listId.second }
        ?: error("List unknown")

    // 이 함수는 목록을 모든 콘텐츠가 포함된 HTML 페이지로 변환하는 역할을 한다.
    fun renderHtml(todoList: ToDoList): HtmlPage =
        HtmlPage(
            """
           <html>
                <body>
                    <h1>Zettai<h1>
                    <h2>${todoList.listName.name}</h2>
                    <table>
                        <tbody>${renderItems(todoList.items)}</tbody>
                    </table>
                </body>
           </html>
        """.trimIndent()
        )

    fun renderItems(items: List<ToDoItem>) =
        items.joinToString("") {
            """<tr><td>${it.description}</td></tr>""".trimIndent()
        }

    // 마지막으로 생성된 HTML 페이지를 본문으로 하는 HTTP 응답을 생성한다.
    fun createResponse(html: HtmlPage): Response = Response(Status.OK).body(html.raw)
}

data class ToDoList(val listName: ListName, val items: List<ToDoItem>)
data class ListName(val name: String)
data class User(val name: String)
data class ToDoItem(val description: String)
enum class ToDoStatus { Todo, InProgress, Done, Blocked }

data class HtmlPage(val raw: String)
