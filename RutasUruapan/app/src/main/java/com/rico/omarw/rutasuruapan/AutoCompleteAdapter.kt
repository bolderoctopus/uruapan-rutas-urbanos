package com.rico.omarw.rutasuruapan

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import java.lang.Exception
import java.util.concurrent.TimeUnit

class AutoCompleteAdapter (context: Context,
                           private val placesClient: PlacesClient,
                           private val bounds: RectangularBounds)
    : ArrayAdapter<AutocompleteItemModel>(context, R.layout.current_location_list_item, android.R.id.text1),
    Filterable{
    /*next steps:
        [...] add powered by google
        [...] add "Use current location" item
        [] get latlng from palce
        [] set threshold,
     */

    enum class ViewTypes(val id: Int){
        CurrentLocation (0),
        AutocompletePrediction (1),
    }

    private val characterStyle = StyleSpan(Typeface.BOLD)
    private var resultsList: ArrayList<AutocompleteItemModel> = ArrayList()

    init {
        resultsList.add(AutocompleteItemModel("Usar ubicación actual", "Ignacio Manuel Altamirano", true))
    }

    override fun getCount() =  resultsList.size
    override fun getItem(pos: Int): AutocompleteItemModel = resultsList[pos]
    override fun getItemViewType(position: Int): Int =
        if(resultsList[position].isCurrentLocation)
            ViewTypes.CurrentLocation.id
        else
            ViewTypes.AutocompletePrediction.id
//todo: re: use only one kind of view if things cause problems with some devices (like my huawaei)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val prediction = getItem(position)
        var row: View
// new test code
        row = super.getView(position, convertView, parent)
        row.findViewById<ImageView>(R.id.imageview_gps).visibility =
                if(prediction.isCurrentLocation) View.VISIBLE
                else View.GONE

        row.findViewById<TextView>(android.R.id.text1).text = prediction.autocompletePrediction?.getPrimaryText(characterStyle) ?: prediction.primaryText
        row.findViewById<TextView>(android.R.id.text2).text = prediction.autocompletePrediction?.getSecondaryText(characterStyle) ?: prediction.secondaryText


// previously working code
//        if(prediction.isCurrentLocation){
//            row = LayoutInflater.from(parent.context).inflate(R.layout.current_location_list_item, parent, false).apply {
//                findViewById<TextView>(android.R.id.text1).text = prediction.primaryText
//                findViewById<TextView>(android.R.id.text2).text = prediction.secondaryText
//            }
//
//        }else{
//            row = super.getView(position, convertView, parent)
//            row.apply {
//                findViewById<TextView>(android.R.id.text1).text = prediction.autocompletePrediction?.getPrimaryText(characterStyle)
//                findViewById<TextView>(android.R.id.text2).text = prediction.autocompletePrediction?.getSecondaryText(characterStyle)
//            }
//        }
        return row
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                Log.d(DEBUG_TAG, "performFiltering")
                val results = FilterResults()
                var filterData: MutableList<AutocompletePrediction>? = null

                if(constraint != null)
                    filterData = getAutocomplete(constraint.toString())

                results.values = filterData
                results.count = filterData?.count() ?: 0

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                resultsList.removeAll {!it.isCurrentLocation}
                if(results != null && results.count > 0){
                    (results.values as MutableList<AutocompletePrediction>).forEach{
                        resultsList.add(0, AutocompleteItemModel(it))
                    }
                }
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence  =
                    if(resultValue is AutocompletePrediction)
                        resultValue.getFullText(null)
                    else
                        super.convertResultToString(resultValue)

        }
    }

    private fun getAutocomplete(query: String): MutableList<AutocompletePrediction>?{
        Log.d(DEBUG_TAG, "Starting autocomplete query for: $query")
        val request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(bounds)
                .setCountry("mx")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(AutocompleteSessionToken.newInstance())
                .setQuery(query)
                .build()
        val results = placesClient.findAutocompletePredictions(request)

        return try {
            Tasks.await(results, 10, TimeUnit.SECONDS)
//            arrayList.add(AutocompletePrediction.builder("").setPrimaryText("").setSecondaryText("Powered by Google").build())
            return results.result?.autocompletePredictions//?.forEach { arrayList.add(it) }
//            arrayList.add(AutocompletePrediction.builder("").setFullText("full baby").setPrimaryText("Use current location").setSecondaryText("").build())
        }catch(error: Exception){
            null
        }

    }
}