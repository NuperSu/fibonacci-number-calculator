package org.example

import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.net.ServerSocket
import java.net.Socket

fun fibonacci(n: Int): BigInteger {
    var a = BigInteger.ZERO
    var b = BigInteger.ONE
    for (i in 2..n) {
        val sum = a + b
        a = b
        b = sum
    }
    return b
}

fun runServer(port: Int, maxFib: Int? = null) = runBlocking {
    val server = ServerSocket(port)
    println("Server running on port $port, max Fibonacci number set to: ${maxFib ?: "No limit"}")

    coroutineScope {
        while (isActive) {
            val client = server.accept()
            launch(Dispatchers.IO) {
                handleClient(client, maxFib)
            }
        }
    }
}

suspend fun handleClient(client: Socket, maxFib: Int?) {
    val input = DataInputStream(withContext(Dispatchers.IO) {
        client.getInputStream()
    })
    val output = DataOutputStream(withContext(Dispatchers.IO) {
        client.getOutputStream()
    })

    try {
        while (true) {
            val number = withContext(Dispatchers.IO) {
                input.readInt()
            }
            if (number < 0) {
                withContext(Dispatchers.IO) {
                    output.writeUTF("Error: Number must be a positive integer.\n")
                }
            } else if (maxFib != null && number > maxFib) {
                withContext(Dispatchers.IO) {
                    output.writeUTF("Error: Number exceeds maximum limit of $maxFib.\n")
                }
            } else {
                val fibResult = withContext(Dispatchers.Default) {
                    fibonacci(number)
                }
                withContext(Dispatchers.IO) {
                    output.writeUTF("$fibResult\n")
                }
            }
        }
    } catch (e: Exception) {
        println("Client disconnected")
    } finally {
        withContext(Dispatchers.IO) {
            client.close()
        }
    }
}

fun runClient(host: String, port: Int) {
    val client = Socket(host, port)
    val input = DataInputStream(client.getInputStream())
    val output = DataOutputStream(client.getOutputStream())

    while (true) {
        print("Enter a number (blank to exit): ")
        val userInput = readlnOrNull() ?: ""
        if (userInput.isBlank()) break

        try {
            val number = userInput.toInt()
            output.writeInt(number)
            val result = input.readUTF()
            println("Fibonacci result: $result")
        } catch (e: NumberFormatException) {
            println("Invalid input. Please enter a valid integer.")
        }
    }

    client.close()
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: java -jar fibonacci.jar [server <port> [maxFib]] | [client <host> <port>]")
        println("    server: Runs the server on the given port. Optionally specify a max Fibonacci number.")
        println("    client: Connects to the server at <host>:<port> and starts the client.")
        return
    }

    when (args[0]) {
        "server" -> {
            val port = args[1].toInt()
            val maxFib = args.getOrNull(2)?.toInt()
            runServer(port, maxFib)
        }

        "client" -> runClient(args[1], args[2].toInt())
        else -> println("Invalid mode. Use 'server' or 'client'.")
    }
}
