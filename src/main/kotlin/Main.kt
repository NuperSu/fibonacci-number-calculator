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
fun ServerUI() {
    var text by remember { mutableStateOf("") }
    var serverLog by remember { mutableStateOf("Server is not running") }
    val coroutineScope = rememberCoroutineScope()

    Text("Enter port to start server:")
    Row {
        BasicTextField(value = text, onValueChange = { text = it })
        Button(onClick = {
            coroutineScope.launch {
                serverLog = "Server running on port $text"
                runServer(text.toInt(), 1000)
            }
        }) {
            Text("Start Server")
        }
    }
    Text(serverLog, modifier = Modifier.padding(top = 20.dp))
}

@Composable
fun ClientUI() {
    var serverIp by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("No result yet") }
    val coroutineScope = rememberCoroutineScope()

    Text("Server IP:")
    BasicTextField(value = serverIp, onValueChange = { serverIp = it })
    Text("Port:")
    BasicTextField(value = port, onValueChange = { port = it })
    Text("Number to calculate:")
    BasicTextField(value = number, onValueChange = { number = it })
    Button(onClick = {
        coroutineScope.launch {
            result = requestFibonacci(serverIp, port.toInt(), number.toInt())
        }
    }) {
        Text("Get Fibonacci")
    }
    Text("Result: $result", modifier = Modifier.padding(top = 20.dp))
}

const val MAX_FIBONACCI = 313579

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Fibonacci Server and Client") {
        var isClient by remember { mutableStateOf(true) }

        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Server")
                Switch(checked = isClient, onCheckedChange = { isClient = it })
                Text("Client")
            }

            if (isClient) {
                ClientUI()
            } else {
                ServerUI()
            }
        }
    }
}
