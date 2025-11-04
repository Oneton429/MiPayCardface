package cf.oneton.cardface.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import cf.oneton.cardface.R
import cf.oneton.cardface.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Key {
    val SHOW_ALL_IMAGES = booleanPreferencesKey("show_all_images")
    val ACKNOWLEGEMENT_READED = booleanPreferencesKey("acknowledgement_readed")
}


@Preview(showBackground = true)
@Composable
fun Settings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SwitchBar(getString(LocalContext.current, R.string.show_all_images), Key.SHOW_ALL_IMAGES)
        HorizontalDivider(Modifier.padding(10.dp), DividerDefaults.Thickness, DividerDefaults.color)
        Text(
            "Developed by Oneton",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
                .padding(10.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SwitchBar(
    text: String,
    key: Preferences.Key<Boolean>,
    dataStore: DataStore<Preferences> = LocalContext.current.dataStore
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        var showAllImages by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                dataStore.data.map { it[key] ?: false }.collect { value ->
                    showAllImages = value
                }
            }
        }
        Text(
            text,
            modifier = Modifier.wrapContentSize(Alignment.CenterEnd),
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = showAllImages, onCheckedChange = {
                showAllImages = it
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        dataStore.edit { pref ->
                            pref[key] = it
                        }
                    }
                }
            }, modifier = Modifier.wrapContentSize(Alignment.CenterEnd)
        )
    }
}