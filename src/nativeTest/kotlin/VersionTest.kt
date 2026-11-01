import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionTest {
    @Test
    fun printsVersion() {
        val result = Dots().test("--version")
        assertEquals(0, result.statusCode)
        assertEquals("dots version $VERSION\n", result.stdout)
    }
}
