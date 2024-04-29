package org.example

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.Socket

class ServerClientTests {

    // Test the Fibonacci calculation for correctness
    @Test
    fun testFibonacciFunction() {
        assertEquals(BigInteger.ZERO, fibonacci(0), "Fibonacci of 0 should be 0")
        assertEquals(BigInteger.ONE, fibonacci(1), "Fibonacci of 1 should be 1")
        assertEquals(BigInteger.ONE, fibonacci(-1), "Fibonacci of -1 should be 1")
        assertEquals(BigInteger.ONE, fibonacci(2), "Fibonacci of 2 should be 1")
        assertEquals(-BigInteger.ONE, fibonacci(-2), "Fibonacci of -2 should be -1")
        assertEquals(BigInteger.valueOf(2), fibonacci(3), "Fibonacci of 3 should be 2")
        assertEquals(BigInteger.valueOf(2), fibonacci(-3), "Fibonacci of -3 should be 2")
        assertEquals(BigInteger.valueOf(5), fibonacci(5), "Fibonacci of 5 should be 5")
        assertEquals(BigInteger.valueOf(5), fibonacci(-5), "Fibonacci of -5 should be 5")
        assertEquals(BigInteger.valueOf(21), fibonacci(8), "Fibonacci of 8 should be 21")
        assertEquals(BigInteger.valueOf(-21), fibonacci(-8), "Fibonacci of -8 should be -21")
        assertEquals(BigInteger("354224848179261915075"), fibonacci(100), "Fibonacci of 100 should be 354224848179261915075")
        assertEquals(BigInteger("-354224848179261915075"), fibonacci(-100), "Fibonacci of -100 should be -354224848179261915075")
    }
}
