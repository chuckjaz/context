import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.removingalldoubt.context.examples.io.ioExamples
import dev.removingalldoubt.context.examples.html.htmlExample
import dev.removingalldoubt.context.examples.mhtml.mhtmlExample

@Composable
@Preview
fun App() {

    MaterialTheme {
        Column {
            Button(onClick = {
                ioExamples()
                htmlExample()
            }) {
                Text("Context examples")
            }

            Button(onClick = {
                mhtmlExample()
            }) {
                Text("mhtml example")
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
