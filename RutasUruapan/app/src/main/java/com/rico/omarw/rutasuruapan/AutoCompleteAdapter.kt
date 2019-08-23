package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

class AutoCompleteAdapter (context: Context,
                           private val placesClient: PlacesClient,
                           private val bounds: RectangularBounds)
    : ArrayAdapter<AutocompleteItemModel>(context, R.layout.current_location_list_item, android.R.id.text1),
    Filterable{
    /*todo:
        [x] add powered by google
        [x] add "Use current location" item
        [...] get latlng from place
        [] check autocomplete threshold,
        [] check double call
     */

    enum class ViewTypes(val id: Int){
        CurrentLocation (0),
        AutocompletePrediction (1),
    }

    private val characterStyle = StyleSpan(Typeface.BOLD)
    private var resultsList: ArrayList<AutocompleteItemModel> = ArrayList()

    init {
        getCurrentPlace()
    }

    override fun getCount() =  resultsList.size
    override fun getItem(pos: Int): AutocompleteItemModel = resultsList[pos]
    override fun getItemViewType(position: Int): Int =
        if(resultsList[position].isCurrentLocation)
            ViewTypes.CurrentLocation.id
        else
            ViewTypes.AutocompletePrediction.id
//re: use only one kind of view if things cause problems with some devices (like my huawaei)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val prediction = getItem(position)
        val row: View = super.getView(position, convertView, parent)
        row.findViewById<ImageView>(R.id.imageview_gps).visibility =
                if(prediction.isCurrentLocation) View.VISIBLE
                else View.GONE
        row.findViewById<TextView>(android.R.id.text1).text = prediction.autocompletePrediction?.getPrimaryText(characterStyle) ?: prediction.primaryText
        row.findViewById<TextView>(android.R.id.text2).text = prediction.autocompletePrediction?.getSecondaryText(characterStyle) ?: prediction.secondaryText
        row.findViewById<TextView>(R.id.textview_google).visibility = if(position == 0 && count > 1) View.VISIBLE else View.GONE

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
            return results.result?.autocompletePredictions//?.forEach { arrayList.add(it) }
        }catch(error: Exception){
            null
        }

    }

    private fun getCurrentPlace(){
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return

        val placeFields = arrayListOf<Place.Field>()
        placeFields.add(Place.Field.ADDRESS)
        placeFields.add(Place.Field.LAT_LNG)

        val request = FindCurrentPlaceRequest.builder(placeFields).build()
        placesClient.findCurrentPlace(request).addOnCompleteListener{
            if(it.isSuccessful && it.result != null){
                resultsList.add(0, AutocompleteItemModel("Usar ubicacion actual", it.result!!.placeLikelihoods[0].place))

                Log.d(DEBUG_TAG, "success, ${it.result!!.placeLikelihoods.size} results")
                for(p in it.result!!.placeLikelihoods){
                    Log.d(DEBUG_TAG, "___")
                    Log.d(DEBUG_TAG, "place: ${p.place.address}")
                    Log.d(DEBUG_TAG, "likeLiehood: ${p.likelihood}")
                    Log.d(DEBUG_TAG, "latLng: ${p.place.latLng?.toString()}")
                    Log.d(DEBUG_TAG, "id: ${p.place.id}")
                    Log.d(DEBUG_TAG, "address_components: ${p.place.addressComponents?.toString()}")
                    Log.d(DEBUG_TAG, "___")
                }
            }else{
                Log.d(DEBUG_TAG, "couldnt find current place")
                if(it.exception != null)
                    Log.d(DEBUG_TAG, "exception", it.exception)
            }
        }
    }
}