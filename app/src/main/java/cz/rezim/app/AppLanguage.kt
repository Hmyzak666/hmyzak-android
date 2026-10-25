package cz.rezim.app

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale

internal object AppLanguage {
    val supported=listOf("cs","en","de","it","fr","ru","es","pt-BR","pl","tr")
    val names=listOf("Čeština","English","Deutsch","Italiano","Français","Русский","Español","Português (Brasil)","Polski","Türkçe")
    fun selected(context:Context):String = if(Build.VERSION.SDK_INT>=33)
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags().substringBefore(',')
        else context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).getString("language","")?:""
    fun locale(context:Context):Locale {
        val requested=selected(context)
        if(requested.isNotEmpty()) return Locale.forLanguageTag(requested)
        val system=Resources.getSystem().configuration.locales
        return (0 until system.size()).mapNotNull {i -> supportedLocale(system[i])}.firstOrNull()?:Locale.ENGLISH
    }
    internal fun supportedLocale(locale:Locale):Locale? {
        val tag=supported.firstOrNull {it.equals(locale.toLanguageTag(),ignoreCase=true)}
            ?: supported.firstOrNull {it==locale.language}
            ?: "pt-BR".takeIf {locale.language=="pt" && (locale.country.isEmpty() || locale.country=="BR")}
        return tag?.let(Locale::forLanguageTag)
    }
    fun wrap(context:Context):Context {
        val desired=locale(context)
        Locale.setDefault(desired)
        if(context.resources.configuration.locales[0]==desired) return context
        val config=Configuration(context.resources.configuration)
        config.setLocales(LocaleList(desired))
        return context.createConfigurationContext(config)
    }
    fun choose(context:Context,language:String) {
        require(language.isEmpty() || language in supported)
        context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).edit().putString("language",language).apply()
        if(Build.VERSION.SDK_INT>=33) context.getSystemService(LocaleManager::class.java).applicationLocales=
            if(language.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language)
        else {
            var owner:Context=context
            while(owner is ContextWrapper && owner !is Activity) owner=owner.baseContext
            (owner as? Activity)?.recreate()
        }
    }
}

internal fun Context.quantity(@androidx.annotation.PluralsRes id:Int,count:Int):String = resources.getQuantityString(id,count,count)

internal fun Context.tr(@StringRes id:Int,vararg args:Any?):String = getString(id,*args)

internal fun Context.supersetRoundText(exercise:Exercise,members:List<Exercise>,entries:List<SetEntry>?=null):String {
    val start=supersetRoundStarts(members,entries)[exercise.key]?:1
    val count=entries?.filter {it.exercise==exercise.key}?.maxOfOrNull {it.number}?:exercise.sets
    return if(count<=1) tr(R.string.round_single,start) else tr(R.string.round_range,start,start+count-1)
}
