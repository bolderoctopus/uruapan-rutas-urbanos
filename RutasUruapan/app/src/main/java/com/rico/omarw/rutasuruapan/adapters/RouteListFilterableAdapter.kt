package com.rico.omarw.rutasuruapan.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.rico.omarw.rutasuruapan.R
import com.rico.omarw.rutasuruapan.Utils.formatRouteTitle
import com.rico.omarw.rutasuruapan.models.RouteModel

class RouteListFilterableAdapter  (private val callback: DrawRouteListener?,
                                   private val comparator: Comparator<RouteModel>): RecyclerView.Adapter<RouteListFilterableAdapter.MyViewHolder>(){
    private val sortedListCallback = object : SortedListAdapterCallback<RouteModel>(this) {
        override fun onInserted(position: Int, count: Int) = notifyItemRangeInserted(position, count)
        override fun onRemoved(position: Int, count: Int) =  notifyItemRangeRemoved(position, count)
        override fun onMoved(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)
        override fun onChanged(position: Int, count: Int) = notifyItemRangeChanged(position, count)
        override fun areItemsTheSame(item1: RouteModel?, item2: RouteModel?): Boolean = item1?.id == item2?.id
        override fun compare(o1: RouteModel?, o2: RouteModel?): Int = comparator.compare(o1, o2)
        override fun areContentsTheSame(oldItem: RouteModel?, newItem: RouteModel?): Boolean = oldItem?.equals(newItem) ?: false
    }
    private val sortedList = SortedList<RouteModel>(RouteModel::class.java, sortedListCallback)

    fun getItems() = sortedList

    fun add(list: List<RouteModel>) = sortedList.addAll(list)

    fun replaceAll(newItems: List<RouteModel>){
        sortedList.beginBatchedUpdates()
        for (x in sortedList.size()-1 downTo 0 step 1){
            val model = sortedList[x]
            if(!newItems.contains(model))
                sortedList.remove(model)
        }
        sortedList.addAll(newItems)
        sortedList.endBatchedUpdates()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemLayout = LayoutInflater.from(parent.context).inflate(R.layout.route_list_item, parent, false)
        return MyViewHolder(itemLayout)
    }

    override fun getItemCount() = sortedList.size()

    override fun onBindViewHolder(holder: MyViewHolder, p: Int) {
        holder.model = sortedList[p]
        holder.checkBox.setText(formatRouteTitle(holder.model.routeDb.name, holder.model.routeDb.shortName), TextView.BufferType.SPANNABLE)
        holder.colorTag.setBackgroundColor(Color.parseColor(holder.model.color))
        holder.checkBox.isChecked = holder.model.isDrawn

        holder.checkBox.setOnClickListener {
            callback?.drawRoute(holder.model)
        }
    }

    class  MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        lateinit var model: RouteModel
        var colorTag: View = itemView.findViewById(R.id.view_colorTag)
        var checkBox: CheckBox = itemView.findViewById(R.id.route_name)
    }

    interface DrawRouteListener{
        fun drawRoute(route: RouteModel)
    }
}