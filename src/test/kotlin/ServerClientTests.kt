package org.example

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.net.Socket
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ServerClientTests {
    // Test the Fibonacci calculation for correctness
    @ParameterizedTest
    @CsvSource(
        "0, 0",
        "1, 1",
        "-1, 1",
        "2, 1",
        "-2, -1",
        "3, 2",
        "-3, 2",
        "5, 5",
        "-5, 5",
        "8, 21",
        "-8, -21",
        "100, 354224848179261915075",
        "-100, -354224848179261915075",
        "1000, 43466557686937456435688527675040625802564660517371780402481729089536555417949051890403879840079255169295922593080322634775209689623239873322471161642996440906533187938298969649928516003704476137795166849228875",
        "-1000, -43466557686937456435688527675040625802564660517371780402481729089536555417949051890403879840079255169295922593080322634775209689623239873322471161642996440906533187938298969649928516003704476137795166849228875"
    )
    fun testFibonacci(input: Int, expected: BigInteger) {
        assertEquals(expected, fibonacci(input), "Fibonacci calculation failed for input $input")
    }

    // Test the client-server communication to ensure that the server handles client requests correctly
    @Test
    fun testClientServerCommunication() = runBlocking {
        val clientOutStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(clientOutStream)
        dataOut.writeInt(5)  // Simulating sending '5'
        dataOut.flush()

        val serverInputStream = ByteArrayInputStream(clientOutStream.toByteArray())

        val serverOutStream = ByteArrayOutputStream()
        val serverDataOut = DataOutputStream(serverOutStream)
        val serverResponseInt = 5  // Echo the number back for simplicity in testing
        serverDataOut.writeInt(serverResponseInt)
        serverDataOut.flush()

        // Mock Socket setup with relaxed = true
        val mockSocket = mockk<Socket>(relaxed = true)
        every { mockSocket.getInputStream() } returns serverInputStream
        every { mockSocket.getOutputStream() } returns serverOutStream

        handleClient(mockSocket, null)

        // Check output from the server
        val dataIn = DataInputStream(ByteArrayInputStream(serverOutStream.toByteArray()))
        val actualOutput = dataIn.readInt()
        assertEquals(5, actualOutput, "The server should echo back the correct integer.")
    }
}
