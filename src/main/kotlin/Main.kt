package org.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.math.abs

fun fibonacci(n: Int): BigInteger {
    if (n == 0) return BigInteger.ZERO

    var a = BigInteger.ZERO
    var b = BigInteger.ONE
    for (i in 2..abs(n)) {
        val sum = a + b
        a = b
        b = sum
    }

    if (n < 0 && n % 2 == 0) {
        b = b.negate()
    }

    return b
}

fun runServer(port: Int, maxFib: Int) = runBlocking {
    val server = ServerSocket(port)
    println("Server running on port $port, max Fibonacci number set to: ${maxFib}")

    coroutineScope {
        while (isActive) {
            val client = server.accept()
            launch(Dispatchers.IO) {
                handleClient(client, maxFib)
            }
        }
    }
}

suspend fun runClient(host: String, port: Int) {
    val client = Socket(host, port)
    val input = DataInputStream(client.getInputStream())
    val output = DataOutputStream(client.getOutputStream())

    try {
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
    } catch (e: Exception) {
        println("Error communicating with the server: ${e.message}")
    } finally {
        try {
            client.close()
        } catch (e: Exception) {
            println("Error closing client connection: ${e.message}")
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
            if (maxFib != null && abs(number) > maxFib) {
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

suspend fun requestFibonacci(host: String, port: Int, number: Int): String {
    return withContext(Dispatchers.IO) {
        Socket(host, port).use { client ->
            DataOutputStream(client.getOutputStream()).use { output ->
                DataInputStream(client.getInputStream()).use { input ->
                    output.writeInt(number)
                    return@withContext input.readUTF()
                }
            }
        }
    }
}

@Composable
fun ServerUI(port: String) {
    var text by remember { mutableStateOf(port) }
    var maxFibonacci by remember { mutableStateOf(MAX_FIBONACCI.toString()) } // Store as String for easy TextField handling
    var serverLog by remember { mutableStateOf("Server is not running") }
    val coroutineScope = rememberCoroutineScope()

    Column {
        Text("Enter port to start server:")
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true
        )
        Text("Max Fibonacci number (optional):")
        BasicTextField(
            value = maxFibonacci,
            onValueChange = { maxFibonacci = it },
            singleLine = true
        )
        Row {
            Button(onClick = {
                val portNumber = text.toIntOrNull() ?: 0 // Safely parse the port number
                val maxFib = maxFibonacci.toIntOrNull() ?: MAX_FIBONACCI // Safely parse the max Fibonacci number
                coroutineScope.launch {
                    // Update UI immediately before launching the server
                    serverLog = "Attempting to start server on port $text with max Fibonacci set to $maxFib"
                }
                // Launch server in a non-blocking manner
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        runServer(portNumber, maxFib)
                        withContext(Dispatchers.Main) {
                            serverLog = "Server running on port $text with max Fibonacci set to $maxFib"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            serverLog = "Failed to start server: ${e.message}"
                        }
                    }
                }
            }) {
                Text("Start Server")
            }
        }
        Text(serverLog, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
fun ClientUI(ip: String, port: String, number: String, result: MutableState<String>, onRequest: (String, String, String) -> Unit) {
    var serverIp by remember { mutableStateOf(ip) }
    var clientPort by remember { mutableStateOf(port) }
    var fibNumber by remember { mutableStateOf(number) }

    Text("Server IP:")
    BasicTextField(value = serverIp, onValueChange = { serverIp = it })
    Text("Port:")
    BasicTextField(value = clientPort, onValueChange = { clientPort = it })
    Text("Number to calculate:")
    BasicTextField(value = fibNumber, onValueChange = { fibNumber = it })
    Button(onClick = {
        onRequest(serverIp, clientPort, fibNumber)
    }) {
        Text("Get Fibonacci")
    }
    Text("Result: ${result.value}", modifier = Modifier.padding(top = 20.dp))
}

fun launchGui(args: Array<String>) = application {
    var isServer by remember { mutableStateOf(false) }
    var serverPort by remember { mutableStateOf("") }
    var serverIp by remember { mutableStateOf("") }
    var clientPort by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var result = remember { mutableStateOf("No result yet") }  // This is the MutableState<String>

    // Process command-line arguments for initial GUI setup
    if (args.isNotEmpty()) {
        when (args[0]) {
            "server" -> {
                isServer = true
                serverPort = args.getOrNull(1) ?: ""
            }
            "client" -> {
                isServer = false
                serverIp = args.getOrNull(1) ?: ""
                clientPort = args.getOrNull(2) ?: ""
            }
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "Fibonacci Server and Client") {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Server")
                Switch(checked = isServer, onCheckedChange = { isServer = it })
                Text("Client")
            }

            if (isServer) {
                ServerUI(serverPort)
            } else {
                ClientUI(serverIp, clientPort, number, result) { ip, port, num ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val fibResult = requestFibonacci(ip, port.toInt(), num.toInt())
                        withContext(Dispatchers.Main) {
                            result.value = fibResult
                        }
                    }
                }
            }
        }
    }
}

fun runCli(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar fibonacci.jar [server <port> [maxFib]] | [client <host> <port>]")
        return
    }

    when (args[0]) {
        "server" -> {
            val port = args.getOrNull(1)?.toIntOrNull() ?: return println("Error: Port number must be provided for server.")
            val maxFib = args.getOrNull(2)?.toIntOrNull() ?: MAX_FIBONACCI
            if (maxFib > MAX_FIBONACCI) {
                println("Max Fibonacci number cannot exceed $MAX_FIBONACCI or the client could throw an EOFException.")
            } else {
                runServer(port, maxFib)
            }
        }
        "client" -> {
            if (args.size < 3) {
                println("Error: Insufficient arguments for client. Usage: client <host> <port>")
                return
            }
            val host = args[1]
            val port = args[2].toIntOrNull() ?: return println("Error: Invalid port number.")

            runBlocking {
                runClient(host, port)
            }        }
        else -> {
            println("Invalid mode. Use 'server' or 'client'.")
        }
    }
}

const val MAX_FIBONACCI = 313579

fun main(args: Array<String>) {
    when {
        args.isEmpty() || args.contains("--gui") -> {
            launchGui(args)
        }
        else -> runCli(args)
    }
}
