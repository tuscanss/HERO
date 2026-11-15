package ro.hero.irremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.hero.irremote.ui.AppViewModel
import ro.hero.irremote.ui.CodeFinderScreen
import ro.hero.irremote.ui.HelpScreen
import ro.hero.irremote.ui.LearnScreen
import ro.hero.irremote.ui.LibraryScreen
import ro.hero.irremote.ui.RemoteScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { App() } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        android.os.Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFFFFB74D), secondary = Color(0xFF80CBC4))
        else -> lightColorScheme(primary = Color(0xFFE65100), secondary = Color(0xFF00796B))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Navigare simplă: rută codificată ca text, ex. "remote/<id>" sau "learn/<remoteId>/<buttonId>". */
@Composable
fun App(vm: AppViewModel = viewModel()) {
    var stack by rememberSaveable { mutableStateOf(listOf("library")) }
    fun go(route: String) { stack = stack + route }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1) { back() }

    val route = stack.last()
    val parts = route.split('/')
    when (parts[0]) {
        "library" -> LibraryScreen(vm, openRemote = { go("remote/$it") }, openHelp = { go("help") }, openFinder = { go("finder") })
        "remote" -> RemoteScreen(vm, parts[1], onBack = ::back, onLearn = { b -> go("learn/${parts[1]}/$b") })
        "learn" -> LearnScreen(vm, parts[1], parts[2], onBack = ::back, openFinder = { go("finder/${parts[1]}/${parts[2]}") })
        "finder" -> CodeFinderScreen(vm, parts.getOrNull(1), parts.getOrNull(2), onBack = ::back)
        "help" -> HelpScreen(vm, onBack = ::back)
    }
}
