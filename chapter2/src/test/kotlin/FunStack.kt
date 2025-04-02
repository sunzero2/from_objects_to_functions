import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FunStack {
    @Test
    fun `push into the stack`() {
        val stack1 = FunStack<Char>()
        val stack2 = stack1.push('A')

        assertEquals(0, stack1.size())
        assertEquals(1, stack2.size())
    }

    @Test
    fun `push and pop`() {
        val (q, stack) = FunStack<Char>().push('Q').pop()

        assertEquals(0, stack.size())
        assertEquals('Q', q)
    }

    @Test
    fun `push push pop`() {
        val (b, stack) = FunStack<Char>()
            .push('A')
            .push('B')
            .pop()

        assertEquals(1, stack.size())
        assertEquals('B', b)
    }

    data class FunStack<T>(private val elements: List<T> = emptyList()) {

        fun push(element: T): FunStack<T> = FunStack(listOf(element) + elements)

        fun pop(): Pair<T, FunStack<T>> = elements.first() to FunStack(elements.drop(1))

        fun size(): Int = elements.size
    }
}

