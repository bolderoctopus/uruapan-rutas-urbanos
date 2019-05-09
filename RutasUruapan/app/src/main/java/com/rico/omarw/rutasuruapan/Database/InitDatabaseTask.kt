package com.rico.omarw.rutasuruapan.Database

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.rico.omarw.rutasuruapan.Database.AppDatabase
import com.rico.omarw.rutasuruapan.Database.Points
import com.rico.omarw.rutasuruapan.Database.Routes
import java.lang.ref.WeakReference

class InitDatabaseTask(val contextReference: WeakReference<Context>) : AsyncTask<Unit, Unit, Unit>() {

    override fun doInBackground(vararg params: Unit?): Unit {
        if(contextReference.get() == null) return
        val routesDao = AppDatabase.getInstance(contextReference.get()!!)?.routesDAO()
        routesDao?.deleteAllPoints()
        routesDao?.deleteAllRoutes()
//        //Log.d(DEBUG_TAG, "routes and points deleted")
//
        val rID = routesDao?.insertRoute(Routes("21 LindaVista - Lomas UPN", "Azul", "#4286F4"))
        routesDao?.insertPoints(getPoints21(rID!!))

        routesDao?.insertRoute(Routes("1 Unidad - Palito Verde", "Azul", "#F48C42"))
        val rId2 = routesDao?.insertRoute(Routes("1-B Unidad - Jucutacato", "Azul", "#F48C42"))
        routesDao?.insertPoints(getPoints1B(rId2!!))
        routesDao?.insertRoute(Routes("3 Zapata", "Azul", "#4286F4"))
        routesDao?.insertRoute(Routes("5 Calzonzint", "Azul", "#4286F4"))
        routesDao?.insertRoute(Routes("9 Arroyo Colorado", "Azul", "#4286F4"))
        routesDao?.insertRoute(Routes("20 Cuba - México", "Azul", "#4286F4"))
//
//
//        Log.d(DEBUG_TAG, "routes inserted")
        return
    }

    private fun getPoints21(routeId: Long): ArrayList<Points> {

        val points = ArrayList<Points>()
        points.add(Points(routeId, 19.422341, -102.082270))
        points.add(Points(routeId, 19.422219, -102.081498))
        points.add(Points(routeId, 19.421673, -102.081433))
        points.add(Points(routeId, 19.421369, -102.078858))
        points.add(Points(routeId, 19.421086, -102.076562))
        points.add(Points(routeId, 19.420681, -102.075768))
        points.add(Points(routeId, 19.420540, -102.075361))
        points.add(Points(routeId, 19.420135, -102.074975))
        points.add(Points(routeId, 19.419771, -102.073601))
        points.add(Points(routeId, 19.419528, -102.073301))
        points.add(Points(routeId, 19.419017, -102.071528))
        points.add(Points(routeId, 19.417008, -102.072161))
        points.add(Points(routeId, 19.416381, -102.070048))
        points.add(Points(routeId, 19.415096, -102.065767))
        points.add(Points(routeId, 19.420975, -102.063864))
        points.add(Points(routeId, 19.420428, -102.061836))
        points.add(Points(routeId, 19.419811, -102.059637))
        points.add(Points(routeId, 19.418688, -102.055635))
        points.add(Points(routeId, 19.420449, -102.055012))
        points.add(Points(routeId, 19.421633, -102.054658))
        points.add(Points(routeId, 19.423079, -102.054648))
        points.add(Points(routeId, 19.424658, -102.054626))
        points.add(Points(routeId, 19.426327, -102.055002))
        points.add(Points(routeId, 19.427471, -102.055270))
        points.add(Points(routeId, 19.428381, -102.055399))
        points.add(Points(routeId, 19.428887, -102.055313))
        points.add(Points(routeId, 19.431123, -102.054347))
        points.add(Points(routeId, 19.431953, -102.054004))
        points.add(Points(routeId, 19.433228, -102.053500))
        points.add(Points(routeId, 19.434240, -102.053521))
        points.add(Points(routeId, 19.434806, -102.053038))

        // r=r

        points.add(Points(routeId, 19.434806, -102.053038))
        points.add(Points(routeId, 19.434240, -102.053521))
        points.add(Points(routeId, 19.433228, -102.053500))
        points.add(Points(routeId, 19.431953, -102.054004))
        points.add(Points(routeId, 19.431123, -102.054347))
        points.add(Points(routeId, 19.428887, -102.055313))
        points.add(Points(routeId, 19.428381, -102.055399))
        points.add(Points(routeId, 19.427471, -102.055270))
        points.add(Points(routeId, 19.426530, -102.055092))
        points.add(Points(routeId, 19.427177, -102.057109))
        points.add(Points(routeId, 19.427845, -102.059298))
        points.add(Points(routeId, 19.428816, -102.062538))
        points.add(Points(routeId, 19.426651, -102.063267))
        points.add(Points(routeId, 19.423676, -102.064168))
        points.add(Points(routeId, 19.421289, -102.065005))
        points.add(Points(routeId, 19.420661, -102.062860))
        points.add(Points(routeId, 19.417767, -102.063761))
        points.add(Points(routeId, 19.413781, -102.065134))
        points.add(Points(routeId, 19.414651, -102.068288))
        points.add(Points(routeId, 19.416675, -102.067666))
        points.add(Points(routeId, 19.417828, -102.071786))
        points.add(Points(routeId, 19.419017, -102.071528))
        points.add(Points(routeId, 19.419528, -102.073301))
        points.add(Points(routeId, 19.419771, -102.073601))
        points.add(Points(routeId, 19.420135, -102.074975))
        points.add(Points(routeId, 19.420540, -102.075361))
        points.add(Points(routeId, 19.420681, -102.075768))
        points.add(Points(routeId, 19.421086, -102.076562))
        points.add(Points(routeId, 19.421369, -102.078858))
        points.add(Points(routeId, 19.421673, -102.081433))
        points.add(Points(routeId, 19.422219, -102.081498))
        points.add(Points(routeId, 19.422341, -102.082270))


        /*
        points.add(Points(routeId, 19.431943, -102.054023))
        points.add(Points(routeId, 19.431943, -102.054023))
        points.add(Points(routeId, 19.429472, -102.055083))
        points.add(Points(routeId, 19.428516, -102.055368))
        points.add(Points(routeId, 19.427960, -102.055389))
        points.add(Points(routeId, 19.426584, -102.055153))
        points.add(Points(routeId, 19.428646, -102.062079))
        points.add(Points(routeId, 19.428807, -102.062311))
        points.add(Points(routeId, 19.428782, -102.062518))
        points.add(Points(routeId, 19.426523, -102.063307))
        points.add(Points(routeId, 19.421268, -102.064965))
        points.add(Points(routeId, 19.420701, -102.062830))
        points.add(Points(routeId, 19.413740, -102.065112))
        points.add(Points(routeId, 19.414663, -102.068283))
        points.add(Points(routeId, 19.415672, -102.067988))
        points.add(Points(routeId, 19.416910, -102.072190))
        points.add(Points(routeId, 19.418949, -102.071530))
        points.add(Points(routeId, 19.419858, -102.074345))
        points.add(Points(routeId, 19.420219, -102.075115))
        points.add(Points(routeId, 19.420978, -102.076531))
        points.add(Points(routeId, 19.421701, -102.081423))
        points.add(Points(routeId, 19.421554, -102.081162))
        points.add(Points(routeId, 19.421146, -102.078542))
        points.add(Points(routeId, 19.421159, -102.078217))
        points.add(Points(routeId, 19.420948, -102.076810))
        points.add(Points(routeId, 19.420740, -102.076145))
        points.add(Points(routeId, 19.421159, -102.078217))
        points.add(Points(routeId, 19.419885, -102.074634))
        points.add(Points(routeId, 19.418914, -102.071469))
        points.add(Points(routeId, 19.416890, -102.072212))
        points.add(Points(routeId, 19.415044, -102.065796))
        points.add(Points(routeId, 19.420963, -102.063893))
        points.add(Points(routeId, 19.420406, -102.061849))
        points.add(Points(routeId, 19.420044, -102.060758))
        points.add(Points(routeId, 19.418906, -102.056715))
        points.add(Points(routeId, 19.418595, -102.055624))
        points.add(Points(routeId, 19.421400, -102.054701))
        points.add(Points(routeId, 19.422951, -102.054650))
        points.add(Points(routeId, 19.424504, -102.054632))
        points.add(Points(routeId, 19.426358, -102.055066))
        points.add(Points(routeId, 19.427990, -102.055348))
        points.add(Points(routeId, 19.428847, -102.055291))
        points.add(Points(routeId, 19.430598, -102.054583))
        points.add(Points(routeId, 19.432118, -102.053948))
        points.add(Points(routeId, 19.433102, -102.053502))
*/
        return points
    }

    private fun getPoints1B(routeId: Long): ArrayList<Points> {

        val points = ArrayList<Points>()

        points.add(Points(routeId, 19.427022, -102.033190 ))
        points.add(Points(routeId, 19.427851, -102.033339 ))
        points.add(Points(routeId, 19.428600, -102.033465))
        points.add(Points(routeId,  19.428855, -102.033514))
        points.add(Points(routeId,  19.428621, -102.034372))
        points.add(Points(routeId,  19.427268, -102.038553))
        points.add(Points(routeId, 19.426368, -102.041590 ))
        points.add(Points(routeId,  19.425679, -102.044270))
        points.add(Points(routeId,  19.425739, -102.048248))
        points.add(Points(routeId,  19.425211, -102.048356))
        points.add(Points(routeId, 19.423620, -102.047851 ))
        points.add(Points(routeId,  19.421000, -102.048149))
        points.add(Points(routeId,  19.418158, -102.050720))
        points.add(Points(routeId,  19.412908, -102.052326))
        points.add(Points(routeId, 19.414584, -102.056882 ))
        points.add(Points(routeId, 19.416737, -102.064109 ))
        points.add(Points(routeId,  19.408628, -102.066716))
        points.add(Points(routeId,  19.407449, -102.066905))
        points.add(Points(routeId, 19.405229, -102.066481 ))
        points.add(Points(routeId,  19.403059, -102.066887))
        points.add(Points(routeId, 19.400821, -102.068349 ))
        points.add(Points(routeId,  19.399740, -102.068060))
        points.add(Points(routeId, 19.396311, -102.068926 ))
        points.add(Points(routeId, 19.391858, -102.069822 ))
        points.add(Points(routeId, 19.387355, -102.070874 ))


        return points
    }

}
