package com.rico.omarw.rutasuruapan

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.google.android.gms.maps.model.Polyline

class RouteListAdapter  (val items: List<RouteModel>, val context: Context, val callback: ListCallback): RecyclerView.Adapter<RouteListAdapter.MyViewHolder>(){

    interface ListCallback {
        fun drawRoute(model: RouteModel)
    }

    class  MyViewHolder(itemView: View, val callback: ListCallback) : RecyclerView.ViewHolder(itemView){
        var checkBox: CheckBox = itemView.findViewById(R.id.route_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemLayout = LayoutInflater.from(context).inflate(R.layout.route_list_item, parent, false)
        return MyViewHolder(itemLayout, callback)
    }

    override fun getItemCount() = items.size


    override fun onBindViewHolder(holder: MyViewHolder, p: Int) {
        holder.checkBox.text = "${items[p].name}"
        holder.checkBox.isChecked = items[p].isDrawed

        holder.checkBox.setOnClickListener {
            holder.callback.drawRoute(items[p])
        }
    }

}