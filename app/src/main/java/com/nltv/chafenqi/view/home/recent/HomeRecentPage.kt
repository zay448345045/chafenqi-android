package com.nltv.chafenqi.view.home.recent

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nltv.chafenqi.SCREEN_PADDING
import com.nltv.chafenqi.extension.toChunithmCoverPath
import com.nltv.chafenqi.extension.toDateString
import com.nltv.chafenqi.extension.toMaimaiCoverPath
import com.nltv.chafenqi.storage.datastore.user.chunithm.ChunithmRecentScoreEntry
import com.nltv.chafenqi.storage.datastore.user.maimai.MaimaiRecentScoreEntry
import com.nltv.chafenqi.util.RecentSelectableDates
import com.nltv.chafenqi.view.home.HomeNavItem
import com.nltv.chafenqi.view.songlist.chunithmDifficultyColors
import com.nltv.chafenqi.view.songlist.maimaiDifficultyColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRecentPage(navController: NavController) {
    val model: HomeRecentViewModel = viewModel()

    val listState = rememberLazyListState()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = model.getRecentList().firstOrNull()?.timestamp?.times(1000L),
        selectableDates = model.getRecentSelectableDates()
    )
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var datePicked by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(datePicked) {
        val index = model.getRecentList().indexOfFirst { it.timestamp.times(1000L) <= datePicked }
        if (index < 0) return@LaunchedEffect
        listState.animateScrollToItem(index)
        println("Jumped to index $index")
    }

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePickerDialog = false
                    datePicked = datePickerState.selectedDateMillis ?: 0L
                }) {
                    Text(text = "跳转")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "最近记录") },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                colors = TopAppBarDefaults.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回上一级"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "跳转到具体日期"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = SCREEN_PADDING),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            items(
                count = model.getRecentList().size,
                key = { index -> model.getRecentList()[index].timestamp },
                itemContent = { index ->
                    val entry = model.getRecentList()[index]
                    if (entry is MaimaiRecentScoreEntry) {
                        HomeRecentPageEntry(entry, index, navController)
                    } else if (entry is ChunithmRecentScoreEntry) {
                        HomeRecentPageEntry(entry, index, navController)
                    }
                }
            )
        }
    }
}

@Composable
fun HomeRecentPageEntry(entry: MaimaiRecentScoreEntry, index: Int, navController: NavController) {
    val context = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable {
                Log.i("HomeRecentPageEntry", "Jump from index $index")
                navController.navigate(HomeNavItem.Home.route + "/recent/maimai/${index}")
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AsyncImage(
            model = entry.associatedMusicEntry.musicID.toMaimaiCoverPath(),
            contentDescription = "歌曲封面",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(72.dp)
                .border(
                    border = BorderStroke(
                        width = 2.dp, color = maimaiDifficultyColors[entry.levelIndex]
                    ), shape = RoundedCornerShape(10.dp)
                )
                .padding(2.dp)
                .clip(RoundedCornerShape(size = 10.dp))
        )
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(entry.timestamp.toDateString(context), fontSize = 14.sp)
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    entry.title,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Text(
                    text = "%.4f".format(entry.achievements).plus("%"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun HomeRecentPageEntry(entry: ChunithmRecentScoreEntry, index: Int, navController: NavController) {
    val context = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable {
                Log.i("HomeRecentPageEntry", "Jump from index $index")
                navController.navigate(HomeNavItem.Home.route + "/recent/chunithm/${index}")
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AsyncImage(
            model = entry.associatedMusicEntry.musicID.toChunithmCoverPath(),
            contentDescription = "歌曲封面",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(72.dp)
                .border(
                    border = BorderStroke(
                        width = 2.dp, color = chunithmDifficultyColors[entry.levelIndex]
                    ), shape = RoundedCornerShape(10.dp)
                )
                .padding(2.dp)
                .clip(RoundedCornerShape(size = 10.dp))
        )
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(entry.timestamp.toDateString(context), fontSize = 14.sp)
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    entry.title, fontSize = 16.sp, overflow = TextOverflow.Ellipsis, maxLines = 1,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Text(text = entry.score.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}