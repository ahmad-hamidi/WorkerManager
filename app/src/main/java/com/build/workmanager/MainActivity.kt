package com.build.workmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import com.build.workmanager.ui.theme.WorkManagerTheme
import com.build.workmanager.worker.TodoWorker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkManagerTheme {

                // We are using rememberSaveable if the app state killed your input should be not gone
                var nameFieldValue by rememberSaveable { mutableStateOf("") }
                var minutesFieldValue by rememberSaveable { mutableStateOf("") }
                val nameListener: (String) -> Unit = {
                    nameFieldValue = it.trim()
                }
                val minutesListener: (String) -> Unit = {
                    minutesFieldValue = it
                }

                val nameModel = FieldModel(nameFieldValue, nameListener)
                val minutesModel = FieldModel(minutesFieldValue, minutesListener)
                val workerData = Data.Builder()
                //var textFieldValue by remember { mutableStateOf("") }
                val keyboardController = LocalSoftwareKeyboardController.current

                val snackBarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackBarHostState,
                            snackbar = { snackBarData ->
                                Snackbar(
                                    modifier = Modifier
                                        .background(Color.Green) // Custom background color
                                        .wrapContentSize(Alignment.BottomCenter),
                                    action = {
                                        TextButton(onClick = { snackBarData.dismiss() }) {
                                            Text("OK", color = Color.White)
                                        }
                                    }
                                ) {
                                    Text(text = snackBarData.visuals.message, color = Color.White)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) { innerPadding ->
                    Content(
                        modifier = Modifier.padding(innerPadding),
                        nameModel,
                        minutesModel
                    ) {

                        val minutes = minutesFieldValue.toLongOrNull()

                        keyboardController?.hide()

                        val message = if (nameFieldValue.isBlank()) {
                            "Reminder name cannot empty!"
                        } else if (minutes == null || minutes < 1) {
                            "Minimal 1 minutes"
                        } else {
                            "$nameFieldValue, Successfully added your reminder!"
                        }

                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = message
                            )
                        }

                        if (nameFieldValue.isBlank()) return@Content
                        if (minutes == null || minutes < 1) return@Content

                        workerData.putString(TodoWorker.REMINDER_NAME_DATA, nameFieldValue)
                        workerData.putLong(TodoWorker.MINUTES_DATA, minutes)
                        workerData.putBoolean(TodoWorker.IS_PERIODIC_DATA, it)

                        if (it) {
                            WorkManager.getInstance(this@MainActivity)
                                .enqueue(
                                    PeriodicWorkRequest.Builder(
                                        TodoWorker::class.java,
                                        minutes,
                                        TimeUnit.MINUTES
                                    ).setInputData(workerData.build()).build()
                                )
                        } else {
                            WorkManager.getInstance(this@MainActivity)
                                .beginWith(
                                    OneTimeWorkRequest.Builder(TodoWorker::class.java)
                                        .setInputData(workerData.build())
                                        .build()
                                )
                                //.then(oneTimeTodoWorker)
                                .enqueue()
                        }

                        nameFieldValue = ""
                        minutesFieldValue = ""
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Content(
    modifier: Modifier,
    nameModel: FieldModel,
    minutesModel: FieldModel,
    buttonListener: (Boolean) -> Unit
) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS).apply {
            LaunchedEffect(key1 = true) {
                if (!status.isGranted) {
                    launchPermissionRequest()
                }
            }
        }
    }

    Column {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Create Your Reminder by One time or Periodically!",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.title_reminder_name),
            color = Color.Gray
        )
        CustomTextInputComponent(
            nameModel.text,
            "Reminder Name for notification",
            KeyboardType.Text,
            ImeAction.Next,
            nameModel.textListener
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.hint_minutes),
            color = Color.Gray
        )
        CustomTextInputComponent(
            minutesModel.text,
            "Time to minutes",
            KeyboardType.Number,
            ImeAction.Done,
            minutesModel.textListener
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            onClick = {
                buttonListener.invoke(false)
            }
        ) {
            Text(text = "Add reminder one time")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onClick = {
                buttonListener.invoke(true)
            }
        ) {
            Text(text = "Add reminder periodically time")
        }
    }
}

@Composable
fun CustomTextInputComponent(
    text: String,
    placeHolderText: String,
    keyboard: KeyboardType,
    ime: ImeAction,
    onTextChanged: (String) -> Unit,
) {

    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Color.White, CircleShape),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = keyboard,
            imeAction = ime
        ),
        shape = RoundedCornerShape(8.dp),
        placeholder = { Text(placeHolderText) },
        maxLines = 1,
        singleLine = true
    )
}

data class FieldModel(
    val text: String,
    val textListener: (String) -> Unit,
)