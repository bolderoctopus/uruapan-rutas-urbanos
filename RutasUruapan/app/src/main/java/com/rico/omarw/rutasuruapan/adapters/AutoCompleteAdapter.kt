package com.rico.omarw.rutasuruapan.adapters

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Geocoder
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG
import com.rico.omarw.rutasuruapan.R
import com.rico.omarw.rutasuruapan.SearchFragment
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class AutoCompleteAdapter (context: Context,
                           private val coroutineScope: CoroutineScope,
                           private val locationClient: FusedLocationProviderClient,
                           private val placesClient: PlacesClient,
                           private val bounds: RectangularBounds,
                           includeCurrentLocation: Boolean,
                           includePickLocation: Boolean)
    : ArrayAdapter<AutocompleteItemModel>(context, R.layout.current_location_list_item, android.R.id.text1),
    Filterable{

    enum class ViewTypes(val id: Int){
        CurrentLocation (0),
        AutocompletePrediction (1),
        PickLocation(2)
    }
    private val characterStyle = StyleSpan(Typeface.BOLD)
    private var resultsList: ArrayList<AutocompleteItemModel> = ArrayList()
    var ignoreFiltering = false

    init {
        if(includeCurrentLocation)
            addCurrentLocation()
         if(includePickLocation) {
             resultsList.add(AutocompleteItemModel(AutocompleteItemModel.ItemKind.PickLocation, context.getString(R.string.pick_location_primary), context.getString(R.string.pick_location_secondary)))
         }
    }

    override fun getCount() =  resultsList.size
    override fun getItem(pos: Int): AutocompleteItemModel = resultsList[pos]
    override fun getItemViewType(position: Int): Int =
        when(resultsList[position].kind){
            AutocompleteItemModel.ItemKind.CurrentLocation -> ViewTypes.CurrentLocation.id
            AutocompleteItemModel.ItemKind.AutocompletePrediction -> ViewTypes.AutocompletePrediction.id
            AutocompleteItemModel.ItemKind.PickLocation -> ViewTypes.PickLocation.id
        }

//re: use only one kind of view if things cause problems with some devices (like my huawaei)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val prediction = getItem(position)
        val row: View = super.getView(position, convertView, parent)
        val icon = row.findViewById<ImageView>(R.id.imageview_gps)

        when (prediction.kind) {
            AutocompleteItemModel.ItemKind.CurrentLocation -> {
                icon.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_gps)
            }
            AutocompleteItemModel.ItemKind.PickLocation -> {
                icon.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_place)
            }
            else -> icon.visibility = View.GONE
        }

        row.findViewById<TextView>(android.R.id.text1).text = prediction.autocompletePrediction?.getPrimaryText(characterStyle) ?: prediction.primaryText
        row.findViewById<TextView>(android.R.id.text2).text = prediction.autocompletePrediction?.getSecondaryText(characterStyle) ?: prediction.secondaryText

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
                resultsList.removeAll {it.kind == AutocompleteItemModel.ItemKind.AutocompletePrediction}
                if(results != null && results.count > 0){
                    (results.values as MutableList<AutocompletePrediction>).forEach{
                        resultsList.add(0, AutocompleteItemModel(it))
                    }
                }
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                if (resultValue is AutocompleteItemModel)
                    return when (resultValue.kind){
                        AutocompleteItemModel.ItemKind.AutocompletePrediction -> resultValue.primaryText
                        AutocompleteItemModel.ItemKind.CurrentLocation -> resultValue.secondaryText
                        AutocompleteItemModel.ItemKind.PickLocation -> " "
                    }
                    return super.convertResultToString(resultValue)
            }
        }
    }

    // This function runs on the background when called by Filter
    private fun getAutocomplete(query: String): MutableList<AutocompletePrediction>?{
        if(ignoreFiltering) return null

//        Log.d(DEBUG_TAG, "Starting autocomplete query for: $query")
        val request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(bounds)
                .setCountry("mx")
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
//todo: check for permission in order to prevent going to the catch block if permissions is not granted
    fun addCurrentLocation(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        coroutineScope.launch {
            try {
                val location = withContext(Dispatchers.IO) { Tasks.await(locationClient.lastLocation) }
                val address = withContext(Dispatchers.IO) { Geocoder(context).getFromLocation(location.latitude, location.longitude, 1) }
                if(!address.isNullOrEmpty()){
                    resultsList.add(0, AutocompleteItemModel(AutocompleteItemModel.ItemKind.CurrentLocation, context.getString(R.string.current_location_primary),
                            SearchFragment.getShortAddress(address[0]), null, LatLng(address[0].latitude, address[0].longitude)))
                    notifyDataSetChanged()
                }
            }catch (exception: Exception) {
                Log.e(DEBUG_TAG, "Unable to find current location.", exception)
            }
        }
    }

    fun removeCurrentLocation(){
        for(item in resultsList) {
            if(item.kind == AutocompleteItemModel.ItemKind.CurrentLocation){
                resultsList.remove(item)
                notifyDataSetChanged()
                break
            }
        }
    }

}