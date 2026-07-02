package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TraceItApp()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TraceItApp() {
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var alpha by remember { mutableFloatStateOf(0.5f) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val view = LocalView.current
    DisposableEffect(isLocked) {
        view.keepScreenOn = isLocked
        onDispose { view.keepScreenOn = false }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val storagePermissionState = rememberPermissionState(storagePermission)

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (cameraPermissionState.status.isGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1C1B1F))
            ) {
                CameraPreview(
                    lensFacing = if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Image Layer
                imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Tracing overlay",
                        onState = { state ->
                            if (state is coil.compose.AsyncImagePainter.State.Error) {
                                android.util.Log.e("TraceIt", "Image load error: ${state.result.throwable.message}", state.result.throwable)
                            }
                        },
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .scale(scale)
                            .alpha(alpha)
                            .pointerInput(isLocked) {
                                if (!isLocked) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale *= zoom
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                            }
                    )
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(256.dp)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(120.dp))
                        }
                    }
                }

                // Floating HUD Controls
                if (!isLocked) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (storagePermissionState.status.isGranted) {
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    storagePermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Select Image", tint = Color.White)
                        }

                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Overlay Settings Panel
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Transparency Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TRANSPARENCY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.sp), color = Color.White)
                                        Text("${(alpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                    }
                                    Slider(
                                        value = alpha,
                                        onValueChange = { alpha = it },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }

                                // Scale Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("SCALE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.sp), color = Color.White)
                                        Text(String.format("%.1fx", scale), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                    }
                                    Slider(
                                        value = scale,
                                        onValueChange = { scale = it },
                                        valueRange = 0.1f..5f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { isLocked = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.4f),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Position")
                            Spacer(Modifier.width(8.dp))
                            Text("LOCK POSITION", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(innerPadding)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val maxWidthPx = constraints.maxWidth.toFloat()
                            val thumbSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
                            var offsetX by remember { mutableFloatStateOf(0f) }

                            Text(
                                text = "SLIDE TO UNLOCK",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                                    .size(56.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                if (offsetX > maxWidthPx - thumbSizePx - 20f) {
                                                    isLocked = false
                                                }
                                                offsetX = 0f
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - thumbSizePx)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is needed to trace.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Camera Permission")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(lensFacing: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
    } }

    androidx.compose.runtime.LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val finalSelector = if (cameraProvider.hasCamera(cameraSelector)) {
                    cameraSelector
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    null
                }

                if (finalSelector != null) {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        finalSelector,
                        preview
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}
