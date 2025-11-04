package cf.oneton.cardface

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import cf.oneton.cardface.compose.AppTheme
import cf.oneton.cardface.ui.Home
import cf.oneton.cardface.ui.Key
import cf.oneton.cardface.ui.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class Tabs(
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", Icons.Rounded.Home), SETTINGS("settings", Icons.Rounded.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var acknowledgementRead by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    dataStore.data.map { it[Key.ACKNOWLEGEMENT_READED] ?: false }.collect {
                        acknowledgementRead = it
                    }
                }
            }
            AppTheme {
                when (acknowledgementRead) {
                    true -> TabBar()
                    false -> Splash()
                }
            }
        }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TabBar() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(Tabs.HOME.ordinal) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.sizeIn(
                maxHeight = LocalWindowInfo.current.containerSize.height.dp * 0.92f
            )
        ) {
            when (selectedTabIndex) {
                Tabs.HOME.ordinal -> Home()
                Tabs.SETTINGS.ordinal -> Settings()
            }
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex, modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Tabs.entries.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = {
                        Icon(
                            title.icon, contentDescription = title.label
                        )
                    })
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Splash(dataStore: DataStore<Preferences> = LocalContext.current.dataStore) {
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width.dp
    val screenHeight = windowInfo.containerSize.height.dp
    var secondRemain by remember { mutableIntStateOf(5) }
    var acknowledgeEnable by remember { mutableStateOf(false) }
    object : CountDownTimer(4000, 1000) {
        override fun onFinish() {
            acknowledgeEnable = true
        }
        override fun onTick(millisUntilFinished: Long) {
            secondRemain = (millisUntilFinished / 1000 + 1).toInt()
        }
    }.start()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Warning,
            stringResource(R.string.root_unavailable),
            modifier = Modifier
                .size(min(screenWidth, screenHeight) * 0.15f)
                .align(alignment = Alignment.CenterHorizontally),
            tint = Color.LightGray
        )
        Text(
            text = stringResource(R.string.warning_info),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = TextStyle(textIndent = TextIndent(20.sp)),
            modifier = Modifier.padding(16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalButton(onClick = {
                exitProcess(0)
            }) {
                Text(stringResource(R.string.exit))
            }
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.IO) {
                            dataStore.edit { pref ->
                                pref[Key.ACKNOWLEGEMENT_READED] = true
                            }
                        }
                    }
                }, enabled = acknowledgeEnable
            ) {
                Text(
                    when (acknowledgeEnable) {
                        true -> stringResource(R.string.acknowledge)
                        false -> stringResource(R.string.acknowledge) + " ($secondRemain)"
                    }
                )
            }
        }
    }
}