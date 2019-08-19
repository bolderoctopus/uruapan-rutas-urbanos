package com.rico.omarw.rutasuruapan

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.rico.omarw.rutasuruapan.models.RouteModel

class RouteListAdapter  (private val items: List<RouteModel>,
                         private val callback: Listener?): RecyclerView.Adapter<RouteListAdapter.MyViewHolder>(){

    public fun getItems() = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemLayout = LayoutInflater.from(parent.context).inflate(R.layout.route_list_item, parent, false)
        return MyViewHolder(itemLayout)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MyViewHolder, p: Int) {
        holder.checkBox.text = items[p].name
        holder.checkBox.isChecked = items[p].isDrawed

        holder.checkBox.setOnClickListener {
            callback?.drawRoute(items[p])
        }
    }

    class  MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var checkBox: CheckBox = itemView.findViewById(R.id.route_name)
    }

    interface Listener{
        fun drawRoute(route: RouteModel)
    }
}