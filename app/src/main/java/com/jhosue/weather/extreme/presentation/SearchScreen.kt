package com.jhosue.weather.extreme.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jhosue.weather.extreme.data.remote.dto.SearchResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: WeatherState,
    onSearch: (String) -> Unit,
    onResultClick: (SearchResponseDto) -> Unit,
    navController: NavController
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Ubicación", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nombre de ciudad (ej: Lima)", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Color(0xFF1E1E1E),
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if(state.isSearching) {
                 CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally))
            }

            // Results List
            LazyColumn {
                items(state.searchResults) { result ->
                     SearchResultItem(
                         result = result,
                         onClick = { 
                             onResultClick(result)
                             navController.popBackStack() 
                         }
                     )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: SearchResponseDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = result.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(text = "${result.region}, ${result.country}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
