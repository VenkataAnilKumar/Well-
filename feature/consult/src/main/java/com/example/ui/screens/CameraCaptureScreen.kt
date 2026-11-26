package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.designsystem.R
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val CAMERA_TAG = "SanaCameraX"

enum class CameraGuideMode {
    SYMPTOM,
    LAB_DOCUMENT
}

@Composable
fun CameraCaptureScreen(
    initialGuideMode: CameraGuideMode = CameraGuideMode.SYMPTOM,
    onPhotoConfirmed: (Uri, Bitmap) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestedOnce = true
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequestedOnce) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionGate(
            permissionRequestedOnce = permissionRequestedOnce,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                runCatching { context.startActivity(intent) }
            },
            onClose = onClose
        )
    } else {
        CameraXCaptureContent(
            initialGuideMode = initialGuideMode,
            onPhotoConfirmed = onPhotoConfirmed,
            onClose = onClose
        )
    }
}

@Composable
private fun CameraPermissionGate(
    permissionRequestedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("camera_permission_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.camera_take_photo),
                style = MaterialTheme.typography.titleLarge
            )
        }

        SanctuaryCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.camera_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (permissionRequestedOnce) {
                        stringResource(R.string.camera_permission_settings_hint)
                    } else {
                        stringResource(R.string.camera_permission_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("camera_grant_permission_button")
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.camera_permission_grant))
                }
                if (permissionRequestedOnce) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("camera_open_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.camera_open_settings))
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onClose,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Back to Intake")
        }
    }
}

@Composable
private fun CameraXCaptureContent(
    initialGuideMode: CameraGuideMode,
    onPhotoConfirmed: (Uri, Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var guideMode by remember { mutableStateOf(initialGuideMode) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var canSwitchCamera by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var focusIndicatorOffset by remember { mutableStateOf<Offset?>(null) }

    var capturedPair by remember { mutableStateOf<Pair<Uri, Bitmap>?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Clear tap-to-focus ring after brief display
    LaunchedEffect(focusIndicatorOffset) {
        if (focusIndicatorOffset != null) {
            delay(900L)
            focusIndicatorOffset = null
        }
    }

    // Keep ImageCapture flash mode synchronized
    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    // Bind CameraX Preview + ImageCapture to LifecycleOwner
    LaunchedEffect(lensFacing, capturedPair == null) {
        if (capturedPair != null) return@LaunchedEffect
        cameraError = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    cameraProviderRef = provider

                    val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
                    val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
                    canSwitchCamera = hasBack && hasFront

                    val resolvedSelector = when {
                        lensFacing == CameraSelector.LENS_FACING_BACK && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                        lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                        hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                        hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> throw IllegalStateException("No camera hardware detected on this device.")
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        resolvedSelector,
                        preview,
                        imageCapture
                    )
                    boundCamera = camera
                    hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                } catch (e: Exception) {
                    Log.e(CAMERA_TAG, "Failed to bind CameraX use cases", e)
                    cameraError = e.localizedMessage ?: "Unable to start the camera preview."
                }
            },
            mainExecutor
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProviderRef?.unbindAll() }
        }
    }

    val previewPair = capturedPair
    if (previewPair != null) {
        CapturedPhotoReviewView(
            bitmap = previewPair.second,
            guideMode = guideMode,
            onRetake = {
                deleteTempCaptureFile(previewPair.first)
                capturedPair = null
            },
            onConfirm = { onPhotoConfirmed(previewPair.first, previewPair.second) },
            onClose = {
                deleteTempCaptureFile(previewPair.first)
                onClose()
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SanctuaryErrorBanner(
                    message = cameraError ?: "Camera unavailable",
                    onRetry = {
                        cameraError = null
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Back to Intake")
                }
            }
        } else {
            // Live CameraX PreviewView with tap-to-focus
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(boundCamera) {
                        detectTapGestures { offset ->
                            focusIndicatorOffset = offset
                            val camera = boundCamera ?: return@detectTapGestures
                            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            camera.cameraControl.startFocusAndMetering(action)
                        }
                    }
                    .testTag("camerax_preview_view")
            )

            // Framing Guide & Focus Ring Overlay
            CameraFramingOverlay(
                guideMode = guideMode,
                focusOffset = focusIndicatorOffset,
                accentColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize()
            )

            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("camera_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }
                }

                // Guide Mode Pill Switcher (Symptom vs Lab Document)
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CameraModePill(
                            label = stringResource(R.string.camera_mode_symptom),
                            icon = Icons.Filled.Visibility,
                            selected = guideMode == CameraGuideMode.SYMPTOM,
                            onClick = { guideMode = CameraGuideMode.SYMPTOM },
                            testTag = "camera_guide_symptom"
                        )
                        CameraModePill(
                            label = stringResource(R.string.camera_mode_lab),
                            icon = Icons.Filled.DocumentScanner,
                            selected = guideMode == CameraGuideMode.LAB_DOCUMENT,
                            onClick = { guideMode = CameraGuideMode.LAB_DOCUMENT },
                            testTag = "camera_guide_lab"
                        )
                    }
                }

                // Flash Toggle (only shown when active lens has flash unit)
                if (hasFlashUnit) {
                    val flashIcon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                        ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                        else -> Icons.Filled.FlashAuto
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.52f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                    else -> ImageCapture.FLASH_MODE_AUTO
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("camera_flash_button")
                        ) {
                            Icon(
                                imageVector = flashIcon,
                                contentDescription = stringResource(R.string.cd_toggle_flash),
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Bottom Guidance & Capture Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (guideMode == CameraGuideMode.LAB_DOCUMENT) {
                        stringResource(R.string.camera_tip_lab)
                    } else {
                        stringResource(R.string.camera_tip_symptom)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Privacy badge placeholder on left for symmetry
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Ephemeral on-device capture",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Primary Capture Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCapturing) MaterialTheme.colorScheme.primary else Color.White
                            )
                            .clickable(enabled = !isCapturing) {
                                isCapturing = true
                                val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
                                imageCapture.takePicture(
                                    mainExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            scope.launch {
                                                val result = processCapturedImageProxy(
                                                    context = context,
                                                    image = image,
                                                    mirrorHorizontally = isFront
                                                )
                                                isCapturing = false
                                                if (result != null) {
                                                    capturedPair = result
                                                } else {
                                                    cameraError = "Could not process captured photo. Please try again."
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e(CAMERA_TAG, "Image capture failed", exception)
                                            isCapturing = false
                                            cameraError = exception.localizedMessage ?: "Photo capture failed."
                                        }
                                    }
                                )
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = stringResource(R.string.cd_capture_photo),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Front / Rear Camera Switch
                    if (canSwitchCamera) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("camera_switch_lens_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cameraswitch,
                                    contentDescription = stringResource(R.string.cd_switch_camera),
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CameraFramingOverlay(
    guideMode: CameraGuideMode,
    focusOffset: Offset?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val frameWidth = if (guideMode == CameraGuideMode.LAB_DOCUMENT) {
            size.width * 0.84f
        } else {
            size.width * 0.68f
        }
        val frameHeight = if (guideMode == CameraGuideMode.LAB_DOCUMENT) {
            (size.height * 0.52f).coerceAtMost(frameWidth * 1.35f)
        } else {
            frameWidth
        }
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f - 18.dp.toPx()

        drawRoundRect(
            color = accentColor.copy(alpha = 0.72f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        focusOffset?.let { pt ->
            drawCircle(
                color = accentColor,
                radius = 28.dp.toPx(),
                center = pt,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun CapturedPhotoReviewView(
    bitmap: Bitmap,
    guideMode: CameraGuideMode,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Column {
                Text(
                    text = "REVIEW CAPTURED PHOTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (guideMode == CameraGuideMode.LAB_DOCUMENT) {
                        "Lab Document Check"
                    } else {
                        "Symptom Photo Check"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured health photo preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Check that the photo is clear and well-lit before sending it for ephemeral AI analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("camera_retake_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.camera_retake))
                }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("camera_use_photo_button")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.camera_use_photo))
                }
            }
        }
    }
}

private suspend fun processCapturedImageProxy(
    context: Context,
    image: ImageProxy,
    mirrorHorizontally: Boolean,
    maxDimension: Int = 1024
): Pair<Uri, Bitmap>? = withContext(Dispatchers.IO) {
    try {
        val rawBitmap = image.toBitmap()
        val rotationDegrees = image.imageInfo.rotationDegrees
        val matrix = Matrix().apply {
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
            if (mirrorHorizontally) postScale(-1f, 1f)
        }

        val oriented = if (rotationDegrees != 0 || mirrorHorizontally) {
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                .also { if (it != rawBitmap) rawBitmap.recycle() }
        } else {
            rawBitmap
        }

        val longest = maxOf(oriented.width, oriented.height)
        val finalBitmap = if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest.toFloat()
            val targetW = (oriented.width * scale).toInt().coerceAtLeast(1)
            val targetH = (oriented.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also {
                if (it != oriented) oriented.recycle()
            }
        } else {
            oriented
        }

        val cacheFile = File(context.cacheDir, "sana_capture_${System.currentTimeMillis()}.jpg")
        FileOutputStream(cacheFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        Uri.fromFile(cacheFile) to finalBitmap
    } catch (e: Exception) {
        Log.e(CAMERA_TAG, "Failed to process captured ImageProxy", e)
        null
    } finally {
        image.close()
    }
}

private fun deleteTempCaptureFile(uri: Uri) {
    runCatching {
        if (uri.scheme == "file") {
            uri.path?.let { File(it).delete() }
        }
    }
}
