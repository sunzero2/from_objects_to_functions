import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

class SeeATodoListAT {
//    @Test
//    fun `List owners can see their lists`() {
//        val user = "frank"
//        val listName = "shopping"
//        val foodToBuy = listOf("carrots", "apples", "milk")
//
//        startTheApplication(user, listName, foodToBuy)
//
//        val list = getToDoList(user, listName)
//
//        assertEquals(listName, list.listName.name)
//        assertEquals(foodToBuy, list.items.map { t -> t.description })
//    }

    @Test
    fun `Only onwers can see their lists`() {
        val listName = "shopping"
        startTheApplication("frank", listName, emptyList())

        assertThrows<AssertionFailedError> { getToDoList("bob", listName) }
    }

    @Test
    fun `List owners can see their lists`() {
        val listName = "shopping"
        val foodToBuy = listOf("carrots", "apples", "milk")
        val frank = ToDoListOwner("Frank")
        startTheApplication(frank.name, listName, foodToBuy)
        frank.canSeeTheList(listName, foodToBuy)
    }

    fun getToDoList(user: String, listName: String): ToDoList {
        val client = JettyClient()
        val request = Request(Method.GET, "http://localhost:8081/todo/$user/$listName")
        val response = client(request)

        return if (response.status == Status.OK)
            parseResponse(response.toMessage())
        else
            fail(response.toMessage())
    }

    fun startTheApplication(user: String, listName: String, items: List<String>) {
        val toDoList = ToDoList(ListName(listName), items.map(::ToDoItem))
        val lists = mapOf(User(user) to listOf(toDoList))
        val server = Zettai(lists).asServer(Jetty(8081))
        server.start()
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

    interface ScenarioActor {
        val name: String
    }

    class ToDoListOwner(override val name: String) : ScenarioActor {
        fun canSeeTheList(listName: String, items: List<String>) {
            val expectedList = createList(listName, items)
            val list = getToDoList(name, listName)
            assertEquals(expectedList, list)
        }

        private fun getToDoList(user: String, listName: String): ToDoList {
            TODO()
        }

    }

}

fun createList(listName: String, items: List<String>) = ToDoList(ListName(listName), items.map(::ToDoItem))
