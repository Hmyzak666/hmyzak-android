package cz.rezim.app

import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import java.io.File
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.compositionLocalOf
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

internal val LocalHmyzakDesign=compositionLocalOf {2}

private val RezimColors=darkColorScheme(
    primary=StrengthGreen, background=PitchBlack, surface=Panel,
    onBackground=StrongText, onSurface=StrongText, onPrimary=Color(0xFF001A0B),
    secondary=RunningBlue, onSurfaceVariant=SecondaryText,
    surfaceVariant=PanelLight, outline=DarkBorder
)

@Composable
internal fun HmyzakTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=RezimColors,content=content)
}

class MainActivity : ComponentActivity() {
    internal var workoutIntentVersion by mutableIntStateOf(0)
        private set
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent);setIntent(intent);workoutIntentVersion++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=android.graphics.Color.BLACK
        window.navigationBarColor=android.graphics.Color.BLACK
        setContent {
            HmyzakTheme { AppRoot() }
        }
    }
}

@Composable
fun AppRoot(vm:RezimState=viewModel()) {
    val context=LocalContext.current
    val workoutIntentVersion=(context as? MainActivity)?.workoutIntentVersion ?: 0
    LaunchedEffect(workoutIntentVersion) {
        val activity=context as? MainActivity
        if(activity?.intent?.getBooleanExtra("open_workout",false)==true && vm.session!=null) {
            vm.page=Page.STRENGTH;activity.intent.removeExtra("open_workout")
        }
    }
    val density=LocalDensity.current
    val keyboardVisible=WindowInsets.ime.getBottom(density)>0
    fun copyProgress(scope:RezimState.ExportScope=RezimState.ExportScope.RECENT) {
        val summary=vm.shareSummary(scope)
        val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Hmyzák - tréninkový výpis",summary))
        vm.tell("Výpis je ve schránce. Můžeš ho vložit do ChatGPT.")
    }
    fun shareProgress(scope:RezimState.ExportScope=RezimState.ExportScope.RECENT) {
        val summary=vm.shareSummary(scope)
        try {
            val intent=Intent(Intent.ACTION_SEND).apply {
                if(scope==RezimState.ExportScope.ALL) {
                    // Whole history is sent as a text file to avoid Android intent size limits.
                    val directory=File(context.cacheDir,"shared_exports").apply{mkdirs()}
                    val file=File(directory,"hmyzak-cela-historie.txt")
                    file.writeText(summary,Charsets.UTF_8)
                    val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
                    type="text/plain";putExtra(Intent.EXTRA_STREAM,uri)
                    clipData=ClipData.newUri(context.contentResolver,"Hmyzák historie",uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type="text/plain";putExtra(Intent.EXTRA_TEXT,summary)
                }
            }
            context.startActivity(Intent.createChooser(intent,"Sdílet s ChatGPT nebo jinou aplikací"))
        } catch(e:Exception) {vm.tell("Sdílení se nepodařilo: ${e.localizedMessage}")}
    }
    fun shareRecordImage(name:String,headline:String,detail:String) {
        try {
            val bitmap=createBitmap(1080,1080,Bitmap.Config.ARGB_8888)
            val surface=Canvas(bitmap)
            surface.drawColor(android.graphics.Color.rgb(7,11,9))
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(24,36,30)}
            surface.drawRoundRect(RectF(52f,52f,1028f,1028f),58f,58f,paint)
            paint.color=android.graphics.Color.rgb(100,197,149)
            paint.textSize=58f;paint.isFakeBoldText=true
            surface.drawText("HMYZÁK.",105f,170f,paint)
            paint.isFakeBoldText=false;paint.textSize=31f
            surface.drawText(headline.take(35),105f,300f,paint)
            paint.color=android.graphics.Color.WHITE
            paint.textSize=68f;paint.isFakeBoldText=true
            val safeName=if(name.length>25) name.take(24)+"…" else name
            surface.drawText(safeName,105f,410f,paint)
            paint.isFakeBoldText=false;paint.textSize=37f
            var y=545f
            detail.split(" · ").take(6).forEach { line ->
                if(y<900f) {
                    val value=if(line.length>43) line.take(42)+"…" else line
                    surface.drawText(value,105f,y,paint);y+=88f
                }
            }
            paint.color=android.graphics.Color.rgb(155,173,163)
            paint.textSize=27f
            surface.drawText("OSOBNÍ TRÉNINKOVÝ DENÍK · OFFLINE",105f,962f,paint)
            val directory=File(context.cacheDir,"shared_exports").apply{mkdirs()}
            val file=File(directory,"hmyzak-rekord.png")
            file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
            val intent=Intent(Intent.ACTION_SEND).apply {
                type="image/png";putExtra(Intent.EXTRA_STREAM,uri)
                clipData=ClipData.newUri(context.contentResolver,"Hmyzák rekord",uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent,"Sdílet kartu výkonu"))
        } catch(e:Exception) {vm.tell("Kartu se nepodařilo sdílet: ${e.localizedMessage}")}
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
    var pendingNavigation by remember { mutableStateOf<Page?>(null) }
    var settingsOrigin by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(Page.HOME) }
    BackHandler(enabled=vm.page==Page.SETTINGS) { vm.page=settingsOrigin }
    fun navigate(page:Page) {
        if(page==vm.page) return
        if((vm.session!=null || vm.editingSavedWorkout!=null) && vm.page==Page.STRENGTH) pendingNavigation=page
        else vm.page=page
    }
    BackHandler(enabled=vm.page==Page.STRENGTH && (vm.session!=null || vm.editingSavedWorkout!=null)) {
        pendingNavigation=Page.HOME
    }
    pendingNavigation?.let { target ->
        AlertDialog(onDismissRequest={pendingNavigation=null},
            title={Text("Opustit trénink?")},
            text={Text(if(vm.editingSavedWorkout!=null)
                "Neuložené opravy historického tréninku se zahodí."
                else "Rozpracované série zůstanou bezpečně uložené a můžeš v nich později pokračovat.")},
            confirmButton={TextButton(onClick={
                if(vm.editingSavedWorkout!=null) vm.cancelSavedEdit()
                vm.page=target;pendingNavigation=null
            }){Text("Odejít")}},
            dismissButton={TextButton(onClick={pendingNavigation=null}){Text("Zůstat")}}
        )
    }
    val snackbar=remember { SnackbarHostState() }
    val notice=vm.message
    LaunchedEffect(notice) {
        if(notice!=null) { snackbar.showSnackbar(notice); vm.clearMessage() }
    }
    CompositionLocalProvider(LocalHmyzakDesign provides vm.designMode) {
    Scaffold(
        modifier=Modifier.background(PitchBlack),
        containerColor=PitchBlack,
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            Column {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=19.dp,end=19.dp,top=12.dp,bottom=13.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                Column {
                    Text("HMYZÁK",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,letterSpacing=2.sp)
                    Text(vm.page.title.uppercase(),style=MaterialTheme.typography.labelSmall,color=SecondaryText)
                }
                Column(horizontalAlignment=androidx.compose.ui.Alignment.End) {
                    TextButton(onClick={
                        if(vm.page==Page.SETTINGS) vm.page=settingsOrigin
                        else {settingsOrigin=vm.page;vm.page=Page.SETTINGS}
                    },contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)) {
                        Text(if(vm.page==Page.SETTINGS) "Zpět" else "Nastavení",fontSize=12.sp)
                    }
                    Text("v${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.labelSmall,color=SecondaryText)
                }
            }
            if(vm.page==Page.STRENGTH && vm.session!=null && vm.editingSavedWorkout==null) WorkoutToolbar(vm)
            }
        },
        bottomBar={
            if (!keyboardVisible) NavigationBar(containerColor=Color(0xFF080808),tonalElevation=0.dp) {
                Page.entries.filter {it!=Page.SETTINGS}.forEach { page ->
                    NavigationBarItem(
                        selected=vm.page==page, onClick={navigate(page)},
                        icon={Text(when(page) {
                            Page.HOME -> "⌂"; Page.STRENGTH -> "▣"; Page.RUN -> "↗"; Page.HIIT -> "◷"; Page.BODY -> "◇"; Page.SETTINGS -> "⚙"
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
                Page.HOME -> HomeScreen(vm, onExport={backupOut.launch("Hmyzak-zaloha-${currentDate()}.json")},onImport={backupIn.launch(arrayOf("application/json","text/plain"))},onShare={shareProgress()},onCopy={copyProgress()},
                    onShareWeek={shareProgress(RezimState.ExportScope.PREVIOUS_WEEK)},
                    onCopyWeek={copyProgress(RezimState.ExportScope.PREVIOUS_WEEK)},
                    onShareAll={shareProgress(RezimState.ExportScope.ALL)},onSettings={settingsOrigin=vm.page;vm.page=Page.SETTINGS})
                Page.STRENGTH -> StrengthScreen(vm,onShareRecord=::shareRecordImage)
                Page.RUN -> RunScreen(vm)
                Page.HIIT -> HiitScreen(vm)
                Page.BODY -> BodyScreen(vm)
                Page.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
    } // LocalHmyzakDesign
}
