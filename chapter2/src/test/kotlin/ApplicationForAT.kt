import org.http4k.client.JettyClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError


// 생성자에게 서버와 클라이언트를 전달한다.
class ApplicationForAT(val client: HttpHandler, val server: AutoCloseable) {

    // 이 메서드는 사용자와 목록 이름으로 서버를 호출하고, 응답을 파싱한 후 목록을 반환한다.
    fun getToDoList(user: String, listName: String): ToDoList {

        // 호스트가 이미 구성되어 있으므로 클라이언트가 호스트 이름을 알 필요가 없다.
        val response = client(
            Request(Method.GET, "/todo/$user/$listName")
        )

        // 응답 상태를 확인해 정상인 경우 응답을 파싱하고, 그렇지 않은 경우 오류로 실패한다.
        return if (response.status == Status.OK)
            parseResponse(response.toMessage())
        else
            fail(response.toMessage())
    }

    // 전체 시나리오를 실행하려면 여기에서 서버를 시작한 다음 서버에서 각 단계를 실행하고 마지막에 서버를 자동으로 닫을 수 있다.
    fun runScenario(vararg steps: Step) {
        server.use {
            steps.onEach { step -> step(this) }
        }
    }

    private fun parseResponse(html: String): ToDoList {
        val nameRegex = "<h2>.*<".toRegex()
        val listName = ListName(extractListName(nameRegex, html))
        val itemsRegex = "<td>.*?<".toRegex()
        val items = itemsRegex.findAll(html)
            .map { ToDoItem(extractItemDesc(it)) }.toList()
        return ToDoList(listName, items)
    }

    private fun extractListName(nameRegex: Regex, html: String): String = nameRegex.find(html)?.value
        ?.substringAfter("<h2>")
        ?.dropLast(1)
        .orEmpty()

    private fun extractItemDesc(matchResult: MatchResult): String = matchResult.value.substringAfter("<td>").dropLast(1)

    fun startTheApplication(lists: Map<User, List<ToDoList>>): ApplicationForAT {
        val port = 8081
        val server = Zettai(lists).asServer(Jetty(port))
        server.start()
        val client = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:$port"))
            .then(JettyClient())

        return ApplicationForAT(client, server)
    }

}

interface ScenarioActor {
    val name: String
}

class ToDoListOwner(override val name: String) : ScenarioActor {
    fun canSeeTheList(listName: String, items: List<String>, app: ApplicationForAT) {
        val expectedList = createList(listName, items)
        val list = app.getToDoList(name, listName)
        assertEquals(expectedList, list)
    }

    fun cannotSeeTheList(listName: String): Step = {
        assertThrows<AssertionFailedError> { getToDoList(name, listName) }
    }
}

interface Actions {
    fun getToDoList(user: String, listName: String): ToDoList?
}

typealias Step = Actions.() -> Unit