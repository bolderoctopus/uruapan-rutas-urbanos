package com.rico.omarw.rutasuruapan

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.lang.Exception
import java.util.concurrent.TimeUnit

class AutoCompleteAdapter (context: Context,
                           private val placesClient: PlacesClient,
                           private val bounds: RectangularBounds)
    : ArrayAdapter<AutocompletePrediction>(context, R.layout.custom_expandable_list_item, android.R.id.text1),
    Filterable{
// instead of a filter object
    // build the request
    // instead of geaDataClient use PlacesClient

    private val characterStyle = StyleSpan(Typeface.BOLD)
    private lateinit var resultsList: MutableList<AutocompletePrediction>

    override fun getCount(): Int = resultsList.size
    override fun getItem(pos: Int) = resultsList[pos]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)

        val prediction = getItem(position)
        val textView1 = row.findViewById<TextView>(android.R.id.text1)
        val textView2 = row.findViewById<TextView>(android.R.id.text2)

        textView1.text = prediction.getPrimaryText(characterStyle)
        textView2.text = prediction.getSecondaryText(characterStyle)

        return row
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                var filterData: MutableList<AutocompletePrediction>? = null
                if(constraint != null)
                    filterData = getAutocomplete(constraint.toString())

                results.values = filterData
                results.count = filterData?.count() ?: 0

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if(results != null && results.count > 0){
                    resultsList = results.values as MutableList<AutocompletePrediction>
                    notifyDataSetChanged()
                }
                else{
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence  =
                    if(resultValue is AutocompletePrediction)
                        (resultValue as AutocompletePrediction).getFullText(null)
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
            results.result?.autocompletePredictions
        }catch(error: Exception){
            null
        }

    }
}