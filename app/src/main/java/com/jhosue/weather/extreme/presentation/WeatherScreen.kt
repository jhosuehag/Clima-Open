package com.jhosue.weather.extreme.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import com.jhosue.weather.extreme.presentation.components.WeatherCard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.key
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import android.Manifest

// ... (existing imports)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WeatherScreen(
    state: WeatherState,
    onLocationClick: (String, Double, Double) -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onEditToggle: () -> Unit,
    onDelete: (com.jhosue.weather.extreme.domain.model.WeatherInfo) -> Unit,
    onRename: (com.jhosue.weather.extreme.domain.model.WeatherInfo, String) -> Unit,
    onMoveUp: (com.jhosue.weather.extreme.domain.model.WeatherInfo) -> Unit,
    onMoveDown: (com.jhosue.weather.extreme.domain.model.WeatherInfo) -> Unit,
    onDragEnd: (List<com.jhosue.weather.extreme.domain.model.WeatherInfo>) -> Unit = {},
    checkCurrentLocation: () -> Unit
) {
    var showRenameDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.jhosue.weather.extreme.domain.model.WeatherInfo?>(null) }
    
    // OPTION B: Blocking Flag (Strict Implementation)
    // "Crea una variable... if (isUserOrdering) return;"
    var isUserOrdering by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Reset blocking when leaving (DisposableEffect) or Refreshing
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { isUserOrdering = false }
    }

    if (showRenameDialog != null) {
        RenameDialog(
            currentName = showRenameDialog!!.locationName,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                onRename(showRenameDialog!!, newName)
                showRenameDialog = null
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRetry() 
        checkCurrentLocation()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        floatingActionButton = {
            if (!state.isEditMode) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Ubicación")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var isAnyItemDragging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            // "Silent Persistence": Keep the same MutableStateList instance (Adapter) alive.
            // Do NOT recreate it on every state change.
            val currentList = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf<com.jhosue.weather.extreme.domain.model.WeatherInfo>() }
            
            // One-way Sync: Remote/DB -> UI
            // Only update if fundamentally different, to avoid interrupting drag animations or causing glitches.
            LaunchedEffect(state.weatherInfo) {
                // OPTION B: BLOCKING
                // "Si el usuario está ordenando, IGNORA los datos que llegan de la BD"
                if (isUserOrdering) return@LaunchedEffect

                val incomingList = state.weatherInfo.values.filterNotNull().sortedBy { it.sortOrder }
                
                // Smart Sync (DiffUtil-ish)
                if (currentList.isEmpty()) {
                    currentList.addAll(incomingList)
                } else {
                    // If sizes differ, we must reset (e.g. deletion/insertion)
                    if (currentList.size != incomingList.size) {
                        currentList.clear()
                        currentList.addAll(incomingList)
                    } else {
                         // Same size. Update items in-place to preserve RecyclerView/LazyColumn state.
                         for (i in incomingList.indices) {
                             if (i < currentList.size) {
                                  // Update content if changed
                                  if (currentList[i] != incomingList[i]) {
                                      // GLITCH FIX: Ignore 'sortOrder' changes to prevent flicker/recomposition.
                                      // The user sees the order via List Position, not the internal field.
                                      val currentNoSort = currentList[i].copy(sortOrder = 0)
                                      val incomingNoSort = incomingList[i].copy(sortOrder = 0)
                                      
                                      if (currentNoSort != incomingNoSort) {
                                           // Real change (Temperature, Name, etc.) -> Execute Update
                                           currentList[i] = incomingList[i]
                                      }
                                      // If only sortOrder changed, do NOTHING. 
                                      // This satisfies "PROHIBIDO usar notifyItemChanged" for purely structural updates.
                                  }
                             }
                         }
                    }
                }
            }
            
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()

            LazyColumn(
                state = listState,
                // THIS IS THE CRITICAL FIX: Disable scroll on the List while any item is being dragged
                userScrollEnabled = !isAnyItemDragging,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pinned Header
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF121212)) 
                            .padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WeatherMaster",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row {
                            androidx.compose.material3.IconButton(onClick = {
                                // Reset blocking when explicitly refreshing
                                isUserOrdering = false
                                onRefresh()
                            }) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                    contentDescription = "Refrescar",
                                    tint = Color.White
                                )
                            }
                            androidx.compose.material3.IconButton(onClick = onEditToggle) {
                                androidx.compose.material3.Icon(
                                    imageVector = if (state.isEditMode) androidx.compose.material.icons.Icons.Default.Check else androidx.compose.material.icons.Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = if (state.isEditMode) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                            androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                    contentDescription = "Ajustes",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Current Location (pinned)
                item {
                    state.currentLocationWeather?.let { weather ->
                         val displayInfo = if (state.isFahrenheit) {
                            weather.copy(temperature = (weather.temperature * 9/5) + 32)
                        } else {
                            weather
                        }
                        
                        Text(
                            text = "📍 Tu Ubicación Actual",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                        WeatherCard(
                            locationName = displayInfo.locationName,
                            weatherInfo = displayInfo,
                            unit = if (state.isFahrenheit) "°F" else "°C",
                            modifier = Modifier.clickable { 
                                onLocationClick(displayInfo.locationName, displayInfo.getLatitude, displayInfo.getLongitude)
                            },
                        )
                         Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Saved Locations Header
                item {
                      Text(
                            text = "Lugares Guardados (Mantén presionado para mover)",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                }

                items(
                    items = currentList,
                    // 1. Stable IDs: Use unique ID based on content (Strict Compliance)
                    // Matches "return listaCiudades.get(position).nombreCiudad.hashCode()"
                    key = { item -> item.locationName.hashCode() }
                ) { info -> 
                    
                    val stableId = "${info.getLatitude}_${info.getLongitude}"
                    
                    val displayInfo = if (state.isFahrenheit) {
                        info.copy(temperature = (info.temperature * 9/5) + 32)
                    } else {
                        info
                    }

                    // Draggable Logic - Item local state
                    var isDragging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    var offsetY by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
                    var itemHeight by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
                    var totalDragDistance by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
                    var initialIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
                    
                    // 2. Direct Restoration (No Animation on Change/Drop)
                    // Matches "view.setAlpha(1.0f)" - Instant, no interpolation flicker.
                    val elevation = if (isDragging) 8.dp else 0.dp
                    val scale = if (isDragging) 1.05f else 1f
                    val alpha = if (isDragging) 0.7f else 1f

                    Box(
                        modifier = Modifier
                            .onSizeChanged { itemHeight = it.height }
                            // FIX: Removed animateItemPlacement to eliminate drop glitch (Change Animation)
                            //.then(if (isDragging) Modifier else Modifier.animateItemPlacement()) 
                            .zIndex(if (isDragging) 2f else 0f) // Keep dragged item on top
                            .graphicsLayer { 
                                translationY = offsetY
                                shadowElevation = elevation.toPx()
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        WeatherCard(
                            locationName = info.locationName,
                            weatherInfo = displayInfo,
                            unit = if (state.isFahrenheit) "°F" else "°C",
                            modifier = Modifier.clickable { if (!state.isEditMode) onLocationClick(info.locationName, info.getLatitude, info.getLongitude) }
                        )

                        // Edit Mode Overlays
                        if (state.isEditMode) {
                            // Drag Handle (Center End) - THE 'HANDLE' (Strict Implementation)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .zIndex(3f) 
                                    .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                                    .padding(8.dp)
                                    // 1. OnStartDragListener: Only this box triggers drag
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isDragging = true 
                                                isAnyItemDragging = true
                                                // Lock updates from DB to prevent flicker
                                                isUserOrdering = true
                                                
                                                // Capture Initial State for Stable Math
                                                initialIndex = currentList.indexOfFirst { 
                                                    "${it.getLatitude}_${it.getLongitude}" == stableId 
                                                }
                                                totalDragDistance = 0f
                                            },
                                            onDragEnd = { 
                                                // 3. clearView: Restore state and Persist
                                                isDragging = false
                                                isAnyItemDragging = false
                                                offsetY = 0f
                                                totalDragDistance = 0f
                                                // SAVE TO DB ONLY HERE
                                                onDragEnd(currentList.toList()) 
                                            },
                                            onDragCancel = { 
                                                isDragging = false
                                                isAnyItemDragging = false
                                                offsetY = 0f 
                                                totalDragDistance = 0f
                                            },
                                            onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                                change.consume()
                                                
                                                // 1. Track Absolute Distance (Finger physical move)
                                                totalDragDistance += dragAmount.y
                                                
                                                // 2. Refresh Logic Constants
                                                val currentIndex = currentList.indexOfFirst { 
                                                    "${it.getLatitude}_${it.getLongitude}" == stableId 
                                                }
                                                if (currentIndex == -1 || itemHeight == 0) return@detectDragGestures

                                                val paddingPx = 16.dp.toPx() 
                                                val fullItemSize = itemHeight + paddingPx
                                                
                                                // 3. Calculate Target Index based on Absolute Logic (Stable)
                                                // logicalPos = startPos + totalDrag
                                                val startPos = initialIndex * fullItemSize
                                                val currentLogicalPos = startPos + totalDragDistance
                                                // Center-based targeting
                                                val targetIndex = ((currentLogicalPos + fullItemSize / 2) / fullItemSize)
                                                    .toInt()
                                                    .coerceIn(0, currentList.size - 1)

                                                // 4. Move if needed (onMove)
                                                if (targetIndex != currentIndex) {
                                                    // Move item from current to target
                                                    val item = currentList.removeAt(currentIndex)
                                                    currentList.add(targetIndex, item)
                                                    // Note: notifyItemMoved is automatic in Compose StateList
                                                }
                                                
                                                // 5. Update Visual Offset to glue item to finger
                                                // Formula: VisualOffset = TotalDrag - (IndexShift * ItemSize)
                                                // This effectively cancels out the physical layout jump.
                                                offsetY = totalDragDistance - ((targetIndex - initialIndex) * fullItemSize)
                                            }
                                        )
                                    }
                            ) {
                                 Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                                    contentDescription = "Reordenar",
                                    tint = Color.White
                                 )
                            }
                            
                            // Delete/Edit Buttons
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                androidx.compose.material3.IconButton(onClick = { showRenameDialog = info }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Renombrar", tint = Color.White)
                                }
                                androidx.compose.material3.IconButton(onClick = { 
                                    // 1. Local Remove (Instant Feedback) - Fixes "Unresponsive" when Visual Updates are blocked
                                    currentList.remove(info)
                                    // 2. Persist
                                    onDelete(info) 
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }

            if (state.isLoading && state.weatherInfo.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFFF7043)
                )
            }
            
            state.error?.let { error ->
                if(state.weatherInfo.isEmpty()) {
                   Column(
                       modifier = Modifier.align(Alignment.Center),
                       horizontalAlignment = Alignment.CenterHorizontally
                   ) {
                       Text(
                           text = error,
                           color = Color.Red,
                           textAlign = TextAlign.Center,
                           modifier = Modifier.padding(20.dp)
                       )
                       androidx.compose.material3.Button(
                           onClick = onRetry
                       ) {
                           Text("Reintentar")
                       }
                   }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentName) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar Ubicación") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { newText -> text = newText },
                singleLine = true
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(text) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
