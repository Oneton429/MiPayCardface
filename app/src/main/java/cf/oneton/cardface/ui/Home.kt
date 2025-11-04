package cf.oneton.cardface.ui

import android.content.ClipData
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.getString
import cf.oneton.cardface.R
import cf.oneton.cardface.dataStore
import cf.oneton.cardface.utils.FileUtils
import cf.oneton.cardface.utils.PermissionUtils
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TAG = "Home"

data class ImageInfo(
    val name: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val restoreEnabled: Boolean = false
)

@Composable
fun Home() {
    val context = LocalContext.current
    val dataStore = remember { context.dataStore }
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width.dp
    val screenHeight = windowInfo.containerSize.height.dp
    var rootAvailable by rememberSaveable { mutableStateOf(false) }
    var imageInfos by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        if (PermissionUtils.isRootAvailable(Dispatchers.IO)) {
            rootAvailable = true
        }
    }
    if (!rootAvailable) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Warning,
                stringResource(R.string.root_unavailable),
                modifier = Modifier
                    .size(min(screenWidth, screenHeight) * 0.25f)
                    .align(alignment = Alignment.CenterHorizontally),
                tint = Color.LightGray
            )
            Text(
                stringResource(R.string.root_unavailable),
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        val cardWidth = min(screenWidth * 0.75f, (800 / LocalDensity.current.density).dp)
        var loaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            val loadedImages = mutableListOf<ImageInfo>()
            withContext(Dispatchers.IO) {
                val showAllImages = dataStore.data.map { it[Key.SHOW_ALL_IMAGES] ?: false }.first()
                val imagePaths = FileUtils.listFiles()
                Log.i(TAG, "Read file list: $imagePaths")

                imagePaths.forEach { imageName ->
                    if (imageName != "journal" && !imageName.endsWith(".bak")) {
                        Log.i(TAG, "Loading file: $imageName")

                        val imageBytes = FileUtils.read(imageName, 2048)
                        imageBytes.let { bytes ->
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            if (options.outWidth == -1 && options.outHeight == -1) {
                                FileUtils.read(imageName).let { bytes ->
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                }
                            }
                            if (showAllImages) {
                                loadedImages.add(
                                    ImageInfo(
                                        imageName,
                                        FileUtils.read(imageName),
                                        options.outWidth,
                                        options.outHeight,
                                        "$imageName.bak" in imagePaths
                                    )
                                )
                            } else {
                                if ((options.outWidth == 960 && options.outHeight == 606) || (options.outWidth == 1280 && options.outHeight == 807)) {
                                    loadedImages.add(
                                        ImageInfo(
                                            imageName,
                                            FileUtils.read(imageName),
                                            options.outWidth,
                                            options.outHeight,
                                            "$imageName.bak" in imagePaths
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                imageInfos = loadedImages
                loaded = true
            }
        }
        when (loaded) {
            true -> if (imageInfos.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(cardWidth),
                    modifier = Modifier.padding(25.dp, 0.dp)
                ) {
                    items(imageInfos) { imageInfo ->
                        ImageCard(imageInfo)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        stringResource(R.string.root_unavailable),
                        modifier = Modifier
                            .size(min(screenWidth, screenHeight) * 0.25f)
                            .align(alignment = Alignment.CenterHorizontally),
                        tint = Color.LightGray
                    )
                    Text(
                        stringResource(R.string.list_file_failed),
                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            false -> LoadingScreen()
        }
    }
}

@Composable
fun ImageCard(imageInfo: ImageInfo) {
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    var showOverlay by rememberSaveable { mutableStateOf(false) }
    var imageRequest by remember {
        mutableStateOf(
            ImageRequest.Builder(context).data(imageInfo.bytes).build()
        )
    }
    var newImage by remember {
        mutableStateOf(
            ImageRequest.Builder(context).data(ByteArray(0)).build()
        )
    }
    val openReplaceDialog = remember { mutableStateOf(false) }
    val filePicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            it?.let {
                Log.i(TAG, "Loading image for replacement from $it")
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(it), null, options
                )
                if (options.outWidth == imageInfo.width && options.outHeight == imageInfo.height) {
                    Log.i(TAG, "Size fit")
                    newImage = ImageRequest.Builder(context).data(it).build()
                    openReplaceDialog.value = true
                } else {
                    Log.i(TAG, "Size not fit")
                    Toast.makeText(
                        context, getString(context, R.string.size_mismatch), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    val fileExporter = rememberLauncherForActivityResult(
        contract = CreateDocument("image/*")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(imageInfo.bytes)
                outputStream.flush()
                showOverlay = false
            }
            Log.i(TAG, "Successfully export ${imageInfo.name} to $uri.")
            Toast.makeText(
                context, getString(context, R.string.export_success), Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.e(TAG, "Failed to export ${imageInfo.name}, uri is null")
            Toast.makeText(
                context, getString(context, R.string.export_failed), Toast.LENGTH_SHORT
            ).show()
        }
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (showOverlay) 1f else 0f, label = "alpha"
    )
    ElevatedCard(
        shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(15.dp, 15.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showOverlay = !showOverlay }) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "Image ${imageInfo.width}x${imageInfo.height}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = animatedAlpha
                }
                .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    IconButton(onClick = { filePicker.launch("image/*") }) {
                        Icon(
                            Icons.Rounded.Edit, stringResource(R.string.replace), tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        Log.i(TAG, "Loading image for restore from ${imageInfo.name}.bak")
                        val newImageBytes = FileUtils.read("${imageInfo.name}.bak")
                        newImage = ImageRequest.Builder(context).data(newImageBytes).build()
                        openReplaceDialog.value = true
                    }, enabled = imageInfo.restoreEnabled) {
                        Icon(
                            Icons.Rounded.Refresh,
                            stringResource(R.string.restore),
                            tint = when (imageInfo.restoreEnabled) {
                                true -> Color.White
                                false -> Color.DarkGray
                            },
                            modifier = Modifier.graphicsLayer(rotationY = 180.0f)
                        )
                    }
                    IconButton(onClick = { fileExporter.launch("${imageInfo.name}.png") }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ExitToApp,
                            stringResource(R.string.export),
                            tint = Color.White
                        )
                    }
                }
            }
        }
        Text(
            "Size: ${imageInfo.width}x${imageInfo.height}",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        Text(
            "Name: ${imageInfo.name}",
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clickable {
                    CoroutineScope(Dispatchers.IO).launch {
                        clipboardManager.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    TAG, imageInfo.name
                                )
                            )
                        )
                    }
                    Toast.makeText(
                        context,
                        getString(context, R.string.copied_to_clipboard),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.i(TAG, "Copied filename ${imageInfo.name} to clipboard.")
                })
    }
    when {
        openReplaceDialog.value -> {
            DialogWithImage(
                onDismissRequest = { openReplaceDialog.value = false },
                onConfirmation = {
                    openReplaceDialog.value = false
                    val data = newImage.data
                    imageRequest = newImage
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            FileUtils.copy(imageInfo.name, "${imageInfo.name}.bak")
                            when (data) {
                                is Uri -> {
                                    val inputStream = context.contentResolver.openInputStream(data)
                                    val imageBytes = inputStream?.use { it.readBytes() }
                                    if (imageBytes != null) {
                                        FileUtils.write(imageInfo.name, imageBytes)
                                        Log.i(TAG, "Replace ${imageInfo.name} successfully")
                                        showOverlay = false
                                    } else {
                                        Log.e(TAG, "Failed to read image bytes from URI: $data")
                                        Toast.makeText(
                                            context,
                                            getString(context, R.string.read_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                is ByteArray -> {
                                    FileUtils.write(imageInfo.name, data)
                                    Log.i(TAG, "Replace ${imageInfo.name} successfully")
                                    showOverlay = false
                                }

                                else -> {
                                    Log.e(TAG, "Unsupported data type: ${data.javaClass.name}")
                                    Toast.makeText(
                                        context,
                                        getString(context, R.string.replace_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Replace failed: ${e.message}")
                            Toast.makeText(
                                context,
                                getString(context, R.string.replace_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                imageFrom = imageRequest,
                imageTo = newImage,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DialogWithImage(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    imageFrom: ImageRequest,
    imageTo: ImageRequest
) {
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width.dp
    val screenHeight = windowInfo.containerSize.height.dp
    val useRow: Boolean = screenWidth > screenHeight
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.replace_ask),
                    modifier = Modifier.padding(vertical = 15.dp)
                )
                val icon = when (useRow) {
                    true -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
                    false -> Icons.Rounded.KeyboardArrowDown
                }
                if (useRow) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = imageFrom,
                            contentDescription = "Image From",
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp)
                        )
                        Icon(
                            icon, "Change to", modifier = Modifier.size(48.dp), tint = Color.Gray
                        )
                        AsyncImage(
                            model = imageTo,
                            contentDescription = "Image To",
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = imageFrom,
                            contentDescription = "Image From",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                        Icon(
                            icon, "Change to", modifier = Modifier.size(48.dp), tint = Color.Gray
                        )
                        AsyncImage(
                            model = imageTo,
                            contentDescription = "Image To",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}