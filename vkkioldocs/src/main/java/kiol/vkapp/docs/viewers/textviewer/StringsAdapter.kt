package kiol.vkapp.docs.viewers.textviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kiol.vkapp.docs.R

class StringsAdapter(private val text: List<String>) : RecyclerView.Adapter<StringsAdapter.ViewHolder>() {

    data class ViewHolder(val textView: View) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.txt_file_string_item_layout, parent, false)
        )


    override fun getItemCount() = text.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.textView as TextView).text = text[position]
    }
}