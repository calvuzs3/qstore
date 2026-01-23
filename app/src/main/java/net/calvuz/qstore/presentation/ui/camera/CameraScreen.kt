package net.calvuz.qstore.presentation.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream

/**
 * Camera Screen migliorata con feedback immediato e animazioni
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onSearchResults: (List<String>) -> Unit,
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Stato locale per il feedback della cattura
    var isCaptureInProgress by remember { mutableStateOf(false) }

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CameraUiState.SearchSuccess -> {
                isCaptureInProgress = false
                val articleUuids = state.articles.map { it.uuid }
                onSearchResults(articleUuids)
                viewModel.resetState()
            }
            is CameraUiState.NoResults, is CameraUiState.Error -> {
                isCaptureInProgress = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ricerca per Foto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (uiState is CameraUiState.Searching) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasCameraPermission -> {
                    PermissionDeniedContent(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }

                else -> {
                    CameraPreviewContent(
                        cameraController = cameraController,
                        lifecycleOwner = lifecycleOwner,
                        uiState = uiState,
                        isCaptureInProgress = isCaptureInProgress,
                        onCapturePhoto = { imageBytes ->
                            isCaptureInProgress = true
                            viewModel.searchByImage(imageBytes)
                        }
                    )
                }
            }

            // Overlay per ricerca in corso
            AnimatedVisibility(
                visible = uiState is CameraUiState.Searching || isCaptureInProgress,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // RIMUOVI l'if - AnimatedVisibility gestisce già la visibilità
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ricerca in corso...", color = Color.White)
                    }
                }
            }

//            // Overlay per ricerca in corso
//            AnimatedVisibility(
//                visible = uiState is CameraUiState.Searching || isCaptureInProgress,
//                modifier = Modifier.fillMaxSize(),
//                enter = fadeIn(),
//                exit = fadeOut()
//            ) {
//                SearchingOverlay()
//            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    cameraController: LifecycleCameraController,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    uiState: CameraUiState,
    isCaptureInProgress: Boolean,
    onCapturePhoto: (ByteArray) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Guida visiva per centrare l'oggetto
        CameraGuideOverlay(
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status message animato
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                }
            ) { state ->
                when (state) {
                    is CameraUiState.Ready -> {
                        if (!isCaptureInProgress) {
                            InstructionCard("Centra l'oggetto e scatta una foto")
                        }
                    }

                    is CameraUiState.Searching -> {
                        SearchingCard()
                    }

                    is CameraUiState.NoResults -> {
                        ErrorCard(
                            "Nessun articolo trovato",
                            MaterialTheme.colorScheme.errorContainer
                        )
                    }

                    is CameraUiState.Error -> {
                        ErrorCard(
                            state.message,
                            MaterialTheme.colorScheme.errorContainer
                        )
                    }

                    else -> {}
                }
            }

            // Capture button con animazioni
            val isEnabled = uiState is CameraUiState.Ready ||
                    uiState is CameraUiState.NoResults ||
                    uiState is CameraUiState.Error

            AnimatedContent(
                targetState = isEnabled to isCaptureInProgress,
                transitionSpec = { scaleIn().togetherWith(scaleOut()) }
            ) { (enabled, capturing) ->
                CaptureButton(
                    enabled = enabled && !capturing,
                    capturing = capturing,
                    onClick = {
                        // Il problema era qui - devi passare isCaptureInProgress al parent
                        capturePhoto(cameraController, context) { imageBytes ->
                            onCapturePhoto(imageBytes) // Questo chiama il parent che gestisce isCaptureInProgress
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraGuideOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .background(
                Color.Transparent,
                RoundedCornerShape(12.dp)
            )
    ) {
        // Corners per guidare il frame
        val cornerSize = 20.dp
        val strokeWidth = 3.dp

        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(topStart = 8.dp)
                )
        )

        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(topEnd = 8.dp)
                )
        )

        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(bottomStart = 8.dp)
                )
        )

        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(bottomEnd = 8.dp)
                )
        )
    }
}

@Composable
private fun SearchingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Analisi in corso...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    "Confronto con database articoli",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InstructionCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CenterFocusStrong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SearchingCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Ricerca con AI in corso...",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, backgroundColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    capturing: Boolean,
    onClick: () -> Unit
) {
    val size by animateDpAsState(
        targetValue = if (capturing) 64.dp else 72.dp,
        label = "button_size"
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = when {
            capturing -> MaterialTheme.colorScheme.tertiary
            enabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            capturing -> MaterialTheme.colorScheme.onTertiary
            enabled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    ) {
        AnimatedContent(
            targetState = capturing,
            transitionSpec = { scaleIn().togetherWith(scaleOut()) }
        ) { isCapturing ->
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            } else {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Scatta foto",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Permesso Fotocamera Richiesto",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Per cercare articoli tramite foto, è necessario concedere l'accesso alla fotocamera.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text("Concedi Permesso")
        }
    }
}

/**
 * Cattura foto e converte in ByteArray
 */
private fun capturePhoto(
    cameraController: LifecycleCameraController,
    context: android.content.Context,
    onImageCaptured: (ByteArray) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)

    cameraController.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val byteArray = bitmap.toByteArray()
                onImageCaptured(byteArray)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

/**
 * Converte Bitmap in ByteArray JPEG
 */
private fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 85, stream)
    return stream.toByteArray()
}