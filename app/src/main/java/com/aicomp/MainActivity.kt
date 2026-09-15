package com.aicomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aicomp.ads.AdManager
import com.aicomp.data.repository.CompanionRepository
import com.aicomp.ui.navigation.AppNavGraph
import com.aicomp.ui.theme.AicompTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CompanionRepository.init(applicationContext)
        AdManager.init(applicationContext)
        com.aicomp.ads.RewardedAdManager.preload(applicationContext)
        setContent {
            AiCompanionApp()
        }
    }
}

@Composable
fun AiCompanionApp() {
    AicompTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }
    }
}
