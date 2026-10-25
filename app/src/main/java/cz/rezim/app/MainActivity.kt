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
    override fun attachBaseContext(newBase:Context) {super.attachBaseContext(AppLanguage.wrap(newBase))}
    internal var workoutIntentVersion by mutableIntStateOf(0)
        private set
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent);setIntent(intent);workoutIntentVersion++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=android.graphics.Color.BLACK
        window.navigationBarColor=android.graphics.Color.BLACK
        renderApp()
    }
    internal fun renderApp() {
        val model=androidx.lifecycle.ViewModelProvider(this)[RezimState::class.java]
        setContent {
            HmyzakTheme { AppRoot(model) }
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
        clipboard.setPrimaryClip(ClipData.newPlainText(context.tr(R.string.ui_liftant_treninkovy_vypis_3408d),summary))
        vm.tell(context.tr(R.string.ui_vypis_je_ve_schrance_muzes_ho_3eb20))
    }
    fun shareProgress(scope:RezimState.ExportScope=RezimState.ExportScope.RECENT) {
        val summary=vm.shareSummary(scope)
        try {
            val intent=Intent(Intent.ACTION_SEND).apply {
                if(scope==RezimState.ExportScope.ALL) {
                    // Whole history is sent as a text file to avoid Android intent size limits.
                    val directory=File(context.cacheDir,"shared_exports").apply{mkdirs()}
                    val file=File(directory,"liftant-history.txt")
                    file.writeText(summary,Charsets.UTF_8)
                    val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
                    type="text/plain";putExtra(Intent.EXTRA_STREAM,uri)
                    clipData=ClipData.newUri(context.contentResolver,context.tr(R.string.ui_liftant_historie_eff22),uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type="text/plain";putExtra(Intent.EXTRA_TEXT,summary)
                }
            }
            context.startActivity(Intent.createChooser(intent,context.tr(R.string.ui_sdilet_vypis_pro_ai_nebo_jinou_d54d3)))
        } catch(e:Exception) {vm.tell(context.tr(R.string.ui_sdileni_se_nepodarilo_s_e2359 ,e.localizedMessage))}
    }
    fun shareRecordImage(name:String,headline:String,detail:String) {
        if(!vm.billing.hasPro) {vm.tell(context.tr(R.string.pro_analytics_benefits));return}
        try {
            val bitmap=createBitmap(1080,1080,Bitmap.Config.ARGB_8888)
            val surface=Canvas(bitmap)
            surface.drawColor(android.graphics.Color.rgb(7,11,9))
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(24,36,30)}
            surface.drawRoundRect(RectF(52f,52f,1028f,1028f),58f,58f,paint)
            paint.color=android.graphics.Color.rgb(100,197,149)
            paint.textSize=58f;paint.isFakeBoldText=true
            surface.drawText("LIFTANT.",105f,170f,paint)
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
            surface.drawText(context.tr(R.string.ui_osobni_treninkovy_denik_offline_32547),105f,962f,paint)
            val directory=File(context.cacheDir,"shared_exports").apply{mkdirs()}
            val file=File(directory,"liftant-record.png")
            file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
            val intent=Intent(Intent.ACTION_SEND).apply {
                type="image/png";putExtra(Intent.EXTRA_STREAM,uri)
                clipData=ClipData.newUri(context.contentResolver,context.tr(R.string.ui_liftant_rekord_a4e09),uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent,context.tr(R.string.ui_sdilet_kartu_vykonu_4e0df)))
        } catch(e:Exception) {vm.tell(context.tr(R.string.ui_kartu_se_nepodarilo_sdilet_s_428c5 ,e.localizedMessage))}
    }
    val csvOut=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {uri ->
        if(uri!=null) try {
            val csv=vm.exportWorkoutCsv()
            context.contentResolver.openOutputStream(uri)?.use {it.write(csv.toByteArray(Charsets.UTF_8))}
            vm.tell(context.tr(R.string.ui_csv_exportovano_a6fbc))
        } catch(e:Exception) {vm.tell(context.tr(R.string.ui_export_se_nepodaril_s_bf590 ,e.localizedMessage))}
    }
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(vm,lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            when(event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {vm.billing.resume();vm.reconcileRest()}
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> vm.billing.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer);vm.billing.resume()
        onDispose {lifecycle.removeObserver(observer);vm.billing.pause()}
    }
    val billingMessage=vm.billing.message
    LaunchedEffect(billingMessage) {if(billingMessage!=null) {vm.tell(billingMessage);vm.billing.clearMessage()}}
    var importText by remember { mutableStateOf<String?>(null) }
    val backupOut=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) try {
            val backup=vm.exportBackup().toByteArray(Charsets.UTF_8)
            checkNotNull(context.contentResolver.openOutputStream(uri)) {"Cannot open backup destination"}.use {it.write(backup)}
            vm.tell(context.tr(R.string.ui_zaloha_exportovana_e27d7))
        } catch(e:Exception) { vm.tell(context.tr(R.string.ui_export_selhal_s_83c42 ,e.localizedMessage)) }
    }
    val backupIn=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) try {
            importText=context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } catch(e:Exception) { vm.tell(context.tr(R.string.ui_soubor_nelze_nacist_s_0f2dd ,e.localizedMessage)) }
    }
    if(importText!=null) AlertDialog(
        onDismissRequest={importText=null},
        title={Text(context.tr(R.string.ui_obnovit_zalohu_f0146))},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(context.tr(R.string.restore_warning),color=HiitOrange)
            Text(context.tr(R.string.ui_stavajici_zaznamy_v_aplikaci_budou_nahrazeny_29bc1))
            TextButton(onClick={backupOut.launch("LiftAnt-before-restore-${currentDate()}.json")}) {Text(context.tr(R.string.backup_before_restore))}
        }},
        confirmButton={TextButton(onClick={
            try { vm.importBackup(importText!!) } catch(e:Exception) { vm.tell(context.tr(R.string.ui_obnova_selhala_s_03f27 ,e.localizedMessage)) }
            importText=null
        }) {Text(context.tr(R.string.ui_nahradit_data_c1090),color=HiitOrange)}},
        dismissButton={TextButton(onClick={importText=null}) {Text(context.tr(R.string.ui_zrusit_9bd5e))}}
    )
    if(vm.onboardingRequired) {
        OnboardingScreen(vm,onRestore={backupIn.launch(arrayOf("application/json","text/plain"))})
        return
    }
    var pendingNavigation by remember { mutableStateOf<Page?>(null) }
    var settingsOrigin by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(Page.HOME) }
    var proSettingsRequested by androidx.compose.runtime.saveable.rememberSaveable {mutableStateOf(false)}
    fun openPro() {settingsOrigin=vm.page;proSettingsRequested=true;vm.page=Page.SETTINGS}
    BackHandler(enabled=vm.page==Page.SETTINGS) { vm.page=settingsOrigin }
    BackHandler(enabled=vm.page==Page.HISTORY || vm.page==Page.WORKOUT_DETAIL) {vm.leaveWorkoutHistory()}
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
            title={Text(context.tr(R.string.ui_opustit_trenink_3f353))},
            text={Text(if(vm.editingSavedWorkout!=null)
                context.tr(R.string.ui_neulozene_opravy_historickeho_treninku_se_zahodi_98aa0)
                else context.tr(R.string.ui_rozpracovane_serie_zustanou_bezpecne_ulozene_a_a41df))},
            confirmButton={TextButton(onClick={
                if(vm.editingSavedWorkout!=null) vm.cancelSavedEdit()
                vm.page=target;pendingNavigation=null
            }){Text(context.tr(R.string.ui_odejit_88067))}},
            dismissButton={TextButton(onClick={pendingNavigation=null}){Text(context.tr(R.string.ui_zustat_665ff))}}
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
                    Text("LIFTANT",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,letterSpacing=2.sp)
                    Text(context.getString(vm.page.title).uppercase(context.resources.configuration.locales[0]),style=MaterialTheme.typography.labelSmall,color=SecondaryText)
                }
                Column(horizontalAlignment=androidx.compose.ui.Alignment.End) {
                    TextButton(onClick={
                        if(vm.page==Page.SETTINGS) vm.page=settingsOrigin
                        else if(vm.page==Page.HISTORY || vm.page==Page.WORKOUT_DETAIL) vm.leaveWorkoutHistory()
                        else {settingsOrigin=vm.page;proSettingsRequested=false;vm.page=Page.SETTINGS}
                    },contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)) {
                        Text(if(vm.page in listOf(Page.SETTINGS,Page.HISTORY,Page.WORKOUT_DETAIL)) context.tr(R.string.ui_zpet_7a77a) else context.tr(R.string.ui_nastaveni_55f52),fontSize=12.sp)
                    }
                    Text("v${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.labelSmall,color=SecondaryText)
                }
            }
            if(vm.page==Page.STRENGTH && vm.session!=null && vm.editingSavedWorkout==null) WorkoutToolbar(vm)
            }
        },
        bottomBar={
            if (!keyboardVisible) NavigationBar(containerColor=Color(0xFF080808),tonalElevation=0.dp) {
                listOf(Page.HOME,Page.STRENGTH,Page.RUN,Page.HIIT,Page.BODY).forEach { page ->
                    NavigationBarItem(
                        selected=vm.page==page || (page==Page.HOME && vm.page in listOf(Page.HISTORY,Page.WORKOUT_DETAIL)), onClick={navigate(page)},
                        icon={Text(when(page) {
                            Page.HOME -> "⌂"; Page.STRENGTH -> "▣"; Page.RUN -> "↗"; Page.HIIT -> "◷"; Page.BODY -> "◇"; else -> "⚙"
                        },fontWeight=FontWeight.Bold)},
                        label={Text(context.getString(page.short), maxLines=1)},
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
                Page.HOME -> HomeScreen(vm, onExport={backupOut.launch("LiftAnt-zaloha-${currentDate()}.json")},onImport={backupIn.launch(arrayOf("application/json","text/plain"))},onShare={shareProgress()},onCopy={copyProgress()},
                    onShareWeek={shareProgress(RezimState.ExportScope.PREVIOUS_WEEK)},
                    onCopyWeek={copyProgress(RezimState.ExportScope.PREVIOUS_WEEK)},
                    onShareAll={shareProgress(RezimState.ExportScope.ALL)},onSettings={settingsOrigin=vm.page;proSettingsRequested=false;vm.page=Page.SETTINGS},onCsv={csvOut.launch("LiftAnt-${currentDate()}.csv")},onUpgrade=::openPro)
                Page.STRENGTH -> StrengthScreen(vm,onShareRecord=::shareRecordImage,onShareWorkout={record,story->shareWorkoutImage(context,vm,record,story)},onUpgrade=::openPro)
                Page.RUN -> RunScreen(vm,onUpgrade=::openPro)
                Page.HIIT -> HiitScreen(vm,onUpgrade=::openPro)
                Page.BODY -> BodyScreen(vm,onUpgrade=::openPro)
                Page.HISTORY -> WorkoutHistoryScreen(vm)
                Page.WORKOUT_DETAIL -> WorkoutDetailScreen(vm)
                Page.SETTINGS -> SettingsScreen(vm,onExport={backupOut.launch("LiftAnt-zaloha-${currentDate()}.json")},
                    onImport={backupIn.launch(arrayOf("application/json","text/plain"))},showProFirst=proSettingsRequested)
            }
        }
    }
    } // LocalHmyzakDesign
}
