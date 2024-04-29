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

fun runServer(port: Int) {
    val server = ServerSocket(port)
    println("Server running on port $port")

    while (true) {
        val client = server.accept()
        Thread {
            val input = DataInputStream(client.getInputStream())
            val output = DataOutputStream(client.getOutputStream())

            try {
                while (true) {
                    val number = input.readInt()
                    if (number < 0) {
                        output.writeUTF("Must be a positive number\n")
                    } else {
                        val fibResult = fibonacci(number)
                        output.writeUTF("$fibResult\n")
                    }
                }
            } catch (e: Exception) {
                println("Client disconnected")
            } finally {
                client.close()
            }
        }.start()
    }
}

fun runClient(host: String, port: Int) {
    val client = Socket(host, port)
    val input = DataInputStream(client.getInputStream())
    val output = DataOutputStream(client.getOutputStream())

    while (true) {
        print("Enter a number (blank to exit): ")
        val userInput = readLine() ?: ""
        if (userInput.isBlank()) break

        val number = userInput.toInt()
        output.writeInt(number)
        val result = input.readUTF()
        println("Fibonacci result: $result")
    }

    client.close()
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: java -jar fibonacci.jar server <port> | client <host> <port>")
        return
    }

    when (args[0]) {
        "server" -> runServer(args[1].toInt())
        "client" -> runClient(args[1], args[2].toInt())
        else -> println("Invalid mode. Use 'server' or 'client'.")
    }
}
