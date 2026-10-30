from pathlib import Path
def rep(path,a,b):
 p=Path(path); s=p.read_text(); assert a in s,(path,a); p.write_text(s.replace(a,b,1))
g="app/build.gradle.kts"
rep(g,'applicationId = "com.ledinhtien.ttube"','applicationId = "com.ledinhtien.tubepro"')
rep(g,'versionCode = 318','versionCode = 319')
rep(g,'versionName = "1.1.20"','versionName = "1.1.21"')
rep(g,'"TTube_v','"TubePro_v')
for p in ["app/src/main/res/values/strings.xml","app/src/main/res/values-vi/strings.xml"]:
 rep(p,'<string name="app_name">T Tube</string>','<string name="app_name">TubePro</string>')
m="app/src/main/AndroidManifest.xml"
for a,b in [('android:label="T Tube"','android:label="TubePro"'),('android:label="T Tube Media"','android:label="TubePro Media"')]:
 rep(m,a,b)
svc="""        <!-- Isolated developer experiment: POI, not turn-by-turn navigation. -->
        <service android:name="com.carhud.aaproxy.TubeProCarAppService"
            android:exported="true" android:label="TubePro Maps"
            android:icon="@drawable/ic_carhud_logo">
            <intent-filter>
                <action android:name="androidx.car.app.CarAppService" />
                <category android:name="androidx.car.app.category.POI" />
            </intent-filter>
        </service>
"""
rep(m,'        <!-- Android Auto projection service (Fermata-style).',svc+'        <!-- Android Auto projection service (Fermata-style).')
p=Path("app/src/main/java/com/carhud/aaproxy/TubeProCarAppService.kt")
assert not p.exists()
p.write_text("""package com.carhud.aaproxy

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/** Handoff only from an active Android Auto Car App Library session. */
internal object TubeProCarBridge {
    @Volatile private var active: WeakReference<CarContext>? = null
    fun attach(car: CarContext) { active = WeakReference(car) }
    fun detach(car: CarContext) { if (active?.get() === car) active = null }
    fun navigateIfActive(place: String): Boolean {
        val car = active?.get() ?: return false
        if (place.isBlank() || Looper.myLooper() != Looper.getMainLooper()) return false
        return try {
            car.startCarApp(Intent(CarContext.ACTION_NAVIGATE,
                Uri.parse("geo:0,0?q=" + Uri.encode(place.trim()))))
            Log.i("TubeProLab", "Android Auto host accepted navigation request")
            true // Request accepted, not proof Maps became visible
        } catch (e: Exception) {
            Log.w("TubeProLab", "Android Auto host declined navigation request", e)
            false
        }
    }
}

/** Lab POI entry. Never claim full navigation-category eligibility. */
class TubeProCarAppService : CarAppService() {
    // Experiment only: tighten host allowlist before distribution.
    override fun createHostValidator() = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(sessionInfo: SessionInfo): Session = session()
    override fun onCreateSession(): Session = session()
    private fun session() = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen {
            val context = carContext
            TubeProCarBridge.attach(context)
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) { TubeProCarBridge.detach(context) }
            })
            return TubeProPlacesScreen(context)
        }
    }
}

internal class TubeProPlacesScreen(context: CarContext) : Screen(context) {
    private var status = "Chọn một địa điểm thử Maps (khi xe đang đỗ)."
    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        for (place in listOf("Sân bay Nội Bài", "Hồ Hoàn Kiếm, Hà Nội",
            "Sân vận động Mỹ Đình, Hà Nội")) {
            list.addItem(Row.Builder().setTitle(place)
                .addText("Chạm để yêu cầu Android Auto dẫn đường")
                .setOnClickListener {
                    status = if (TubeProCarBridge.navigateIfActive(place))
                        "Đã gửi yêu cầu; kiểm tra màn hình xe."
                    else "Host từ chối. Xem log TubeProLab."
                    invalidate()
                }.build())
        }
        list.addItem(Row.Builder().setTitle("Trạng thái").addText(status).build())
        return ListTemplate.Builder().setTitle("TubePro - Thử Google Maps")
            .setHeaderAction(Action.APP_ICON).setSingleList(list.build()).build()
    }
}
""")
nav="app/src/main/java/com/carhud/aaproxy/AssistantNavigator.kt"
rep(nav,'if (!walking && carNavigate(c, place)) return Result.CAR_HOST',
    'if (!walking && TubeProCarBridge.navigateIfActive(place)) return Result.CAR_HOST\n        if (!walking && carNavigate(c, place)) return Result.CAR_HOST')
Path("TUBEPRO_TEST.md").write_text("""# TubePro build 319 — isolated Android Auto Car App Library lab
Package: com.ledinhtien.tubepro (installs beside T-Tube).
App label: TubePro; Android Auto POI icon: TubePro Maps.
Test only using APK debug. Do not upload any AAB to Play testing.
On the Android Auto screen, open TubePro Maps, tap a sample place,
and check whether the host switches to the configured navigation app.
With a live POI session, Mimi destination commands may use CarContext;
otherwise the legacy projection falls back to a phone intent.
No guarantee the host chooses Google Maps; no bare map opening with an
invented route. Host eligibility and DHU compatibility must be tested.
Legacy projection/media code remains for comparison, but its video UI
is not an officially supported Car App Library template.
Do not publish: demo POIs, ALLOW_ALL_HOSTS_VALIDATOR, legacy projection
and video use require review and appropriate restrictions.
Existing T-Tube remains installed and its settings are separate.
""")
print("TubePro319 Car App Library lab patched")
