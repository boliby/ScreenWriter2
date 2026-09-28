package com.boliby.screenwriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boliby.screenwriter.editor.EditorScreen
import com.boliby.screenwriter.editor.EditorState
import com.boliby.screenwriter.ui.theme.ScreenWriterTheme

/** Keeps the editor's text through rotation and window resizing. */
class EditorViewModel : ViewModel() {
    val editor = EditorState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = viewModel()
            ScreenWriterTheme {
                EditorScreen(viewModel.editor)
            }
        }
    }
}
