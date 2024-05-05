package org.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
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

fun runServer(server: ServerSocket, maxFib: Int) {
    println("Server running on port ${server.localPort}, max Fibonacci number set to: $maxFib")

    try {
        while (true) {
            val client = server.accept()
            GlobalScope.launch(Dispatchers.IO) {
                handleClient(client, maxFib)
            }
        }
    } catch (e: Exception) {
        println("Server stopped: ${e.message}")
    } finally {
        server.close()
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
    var maxFibonacci by remember { mutableStateOf(MAX_FIBONACCI.toString()) }
    var serverLog by remember { mutableStateOf("Server is not running") }
    var server by remember { mutableStateOf<ServerSocket?>(null) }
    var isStopping by remember { mutableStateOf(false) }  // Track if the server is stopping

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Enter port to start server:", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("Server Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Max Fibonacci number (optional):", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = maxFibonacci,
            onValueChange = { maxFibonacci = it },
            singleLine = true,
            label = { Text("Max Fibonacci") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(
                onClick = {
                    if (!isStopping) {
                        val portNumber = text.toIntOrNull() ?: 0
                        val maxFib = maxFibonacci.toIntOrNull() ?: MAX_FIBONACCI
                        serverLog =
                            "Attempting to start server on port $text with max Fibonacci set to $maxFib"

                        if (server?.isClosed != false) {
                            server?.close()
                            server = ServerSocket(portNumber).apply { reuseAddress = true }
                            serverLog = "Starting server on port $portNumber"
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    server?.let { runServer(it, maxFib) }
                                } catch (e: IOException) {
                                    serverLog = "Failed to start server: ${e.message}"
                                } finally {
                                    server?.close()
                                    isStopping =
                                        false  // Reset stopping state when server is closed
                                }
                            }
                        }
                    }
                },
                enabled = !isStopping,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Server")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isStopping = true  // Set the stopping state
                    server?.close()
                    serverLog = "Server stopping..."
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)  // Allow some time for server to close properly
                        isStopping = false  // Reset the stopping state
                        serverLog = "Server stopped"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Server")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            serverLog,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
fun ClientUI(
    ip: String,
    port: String,
    number: String,
    result: MutableState<String>,
    onRequest: (String, String, String) -> Unit
) {
    var serverIp by remember { mutableStateOf(ip) }
    var clientPort by remember { mutableStateOf(port) }
    var fibNumber by remember { mutableStateOf(number) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Server IP:", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = serverIp,
            onValueChange = { serverIp = it },
            singleLine = true,
            label = { Text("IP Address") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Port:", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = clientPort,
            onValueChange = { clientPort = it },
            singleLine = true,
            label = { Text("Port Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Number to calculate:", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = fibNumber,
            onValueChange = { fibNumber = it },
            singleLine = true,
            label = { Text("Fibonacci Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                try {
                    val fibResult =
                        requestFibonacci(serverIp, clientPort.toInt(), fibNumber.toInt())
                    result.value = fibResult
                } catch (e: Exception) {
                    result.value = "Error: ${e.localizedMessage}"
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Get Fibonacci")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Result: ${result.value}",
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

fun launchGui(args: Array<String>) = application {
    var isClient by remember { mutableStateOf(true) }
    var serverPort by remember { mutableStateOf("") }
    var serverIp by remember { mutableStateOf("") }
    var clientPort by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var result = remember { mutableStateOf("No result yet") }  // This is the MutableState<String>

    // Process command-line arguments for initial GUI setup
    if (args.isNotEmpty()) {
        when (args[0]) {
            "server" -> {
                isClient = false
                serverPort = args.getOrNull(1) ?: ""
            }

            "client" -> {
                isClient = true
                serverIp = args.getOrNull(1) ?: ""
                clientPort = args.getOrNull(2) ?: ""
            }
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "Fibonacci Server and Client") {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Server")
                Switch(checked = isClient, onCheckedChange = { isClient = it })
                Text("Client")
            }

            if (isClient) {
                ClientUI(serverIp, clientPort, number, result) { ip, port, num ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val fibResult = requestFibonacci(ip, port.toInt(), num.toInt())
                        withContext(Dispatchers.Main) {
                            result.value = fibResult
                        }
                    }
                }
            } else {
                ServerUI(serverPort)
            }
        }
    }
}

fun runCli(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar fibonacci.jar [server <port> [maxFib]] | [client <host> <port>] [--gui]")
        return
    }

    when (args[0]) {
        "server" -> {
            val port = args.getOrNull(1)?.toIntOrNull()
                ?: return println("Error: Port number must be provided for cli server. Use --gui for GUI.")
            val maxFib = args.getOrNull(2)?.toIntOrNull() ?: MAX_FIBONACCI
            if (maxFib > MAX_FIBONACCI) {
                println("Max Fibonacci number cannot exceed $MAX_FIBONACCI or the client could throw an EOFException.")
            } else {
                val server = ServerSocket(port)
                try {
                    runServer(server, maxFib)
                } catch (e: Exception) {
                    println("Failed to start server: ${e.message}")
                } finally {
                    server.close()
                }
            }
        }

        "client" -> {
            if (args.size < 3) {
                println("Error: Insufficient arguments for cli client. Usage: client <host> <port>. Use --gui for GUI.")
                return
            }
            val host = args[1]
            val port = args[2].toIntOrNull() ?: return println("Error: Invalid port number.")

            runBlocking {
                runClient(host, port)
            }
        }

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
