package cz.rezim.app

import android.os.Bundle
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

val PitchBlack=Color(0xFF000000)
val Panel=Color(0xFF111111)
val PanelLight=Color(0xFF1B1B1D)
val StrongText=Color(0xFFECECEF)
val SecondaryText=Color(0xFF99999F)
val StrengthGreen=Color(0xFF64C595)
val RunningBlue=Color(0xFF72B3FA)
val HiitOrange=Color(0xFFFFB36C)
val BodyPurple=Color(0xFFB7A1F4)
val DarkBorder=Color(0xFF29292B)

private val RezimColors=darkColorScheme(
    primary=StrengthGreen, background=PitchBlack, surface=Panel,
    onBackground=StrongText, onSurface=StrongText, onPrimary=Color(0xFF001A0B),
    secondary=RunningBlue, onSurfaceVariant=SecondaryText,
    surfaceVariant=PanelLight, outline=DarkBorder
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=android.graphics.Color.BLACK
        window.navigationBarColor=android.graphics.Color.BLACK
        setContent {
            MaterialTheme(colorScheme=RezimColors) { AppRoot() }
        }
    }
}

@Composable
fun AppRoot(vm:RezimState=viewModel()) {
    val context=LocalContext.current
    val density=LocalDensity.current
    val keyboardVisible=WindowInsets.ime.getBottom(density)>0
    fun copyProgress() {
        val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Hmyzák - tréninkový výpis",vm.shareSummary()))
        vm.tell("Výpis je ve schránce. Můžeš ho vložit do ChatGPT.")
    }
    fun shareProgress() {
        val summary=vm.shareSummary()
        try {
            val intent=Intent(Intent.ACTION_SEND).apply {
                type="text/plain"; putExtra(Intent.EXTRA_TEXT,summary)
            }
            context.startActivity(Intent.createChooser(intent,"Sdílet s ChatGPT nebo jinou aplikací"))
        } catch(e:Exception) { vm.tell("Sdílení se nepodařilo: ${e.localizedMessage}") }
    }
    var importText by remember { mutableStateOf<String?>(null) }
    val backupOut=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportBackup().toByteArray(Charsets.UTF_8)) }
            vm.tell("Záloha exportována.")
        } catch(e:Exception) { vm.tell("Export selhal: ${e.localizedMessage}") }
    }
    val backupIn=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) try {
            importText=context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } catch(e:Exception) { vm.tell("Soubor nelze načíst: ${e.localizedMessage}") }
    }
    if(importText!=null) AlertDialog(
        onDismissRequest={importText=null},
        title={Text("Obnovit zálohu?")},
        text={Text("Stávající záznamy v aplikaci budou NAHRAZENY obsahem zálohy. Nejprve si případně vytvoř novou zálohu.")},
        confirmButton={TextButton(onClick={
            try { vm.importBackup(importText!!) } catch(e:Exception) { vm.tell("Obnova selhala: ${e.localizedMessage}") }
            importText=null
        }) {Text("Nahradit data",color=HiitOrange)}},
        dismissButton={TextButton(onClick={importText=null}) {Text("Zrušit")}}
    )
    val snackbar=remember { SnackbarHostState() }
    val notice=vm.message
    LaunchedEffect(notice) {
        if(notice!=null) { snackbar.showSnackbar(notice); vm.clearMessage() }
    }
    Scaffold(
        modifier=Modifier.background(PitchBlack),
        containerColor=PitchBlack,
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            Row(Modifier.fillMaxWidth().padding(start=19.dp,end=19.dp,top=14.dp,bottom=13.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                Column {
                    Text("HMYZÁK",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,letterSpacing=2.sp)
                    Text(vm.page.title.uppercase(),style=MaterialTheme.typography.labelSmall,color=SecondaryText)
                }
                Column(horizontalAlignment=androidx.compose.ui.Alignment.End) {
                    Text("OFFLINE",style=MaterialTheme.typography.labelMedium,color=StrengthGreen)
                    Text("v0.2.0",style=MaterialTheme.typography.labelSmall,color=SecondaryText)
                }
            }
        },
        bottomBar={
            if (!keyboardVisible) NavigationBar(containerColor=Color(0xFF080808),tonalElevation=0.dp) {
                Page.entries.forEach { page ->
                    NavigationBarItem(
                        selected=vm.page==page, onClick={vm.page=page},
                        icon={Text(when(page) {
                            Page.HOME -> "⌂"; Page.STRENGTH -> "▣"; Page.RUN -> "↗"; Page.HIIT -> "◷"; Page.BODY -> "◇"
                        },fontWeight=FontWeight.Bold)},
                        label={Text(page.short, maxLines=1)},
                        colors=NavigationBarItemDefaults.colors(
                            selectedIconColor=when(page){Page.RUN->RunningBlue;Page.HIIT->HiitOrange;Page.BODY->BodyPurple;else->StrengthGreen},
                            selectedTextColor=StrongText, indicatorColor=PanelLight,
                            unselectedTextColor=SecondaryText,unselectedIconColor=SecondaryText
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            when(vm.page) {
                Page.HOME -> HomeScreen(vm, onExport={backupOut.launch("Hmyzak-zaloha-${currentDate()}.json")},onImport={backupIn.launch(arrayOf("application/json","text/plain"))},onShare={shareProgress()},onCopy={copyProgress()})
                Page.STRENGTH -> StrengthScreen(vm)
                Page.RUN -> RunScreen(vm)
                Page.HIIT -> HiitScreen(vm)
                Page.BODY -> BodyScreen(vm)
            }
        }
    }
}
