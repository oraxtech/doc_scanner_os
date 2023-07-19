package pdf.scanner.camscanner.docscanner.activities

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import pdf.scanner.camscanner.docscanner.core.SelectedTool
import pdf.scanner.camscanner.docscanner.core.ToolsFragmentRecyclerViewData
import pdf.scanner.camscanner.docscanner.databinding.ToolsFragmentRecyclerViewItemBinding

class ToolsFragmentRecyclerViewAdapter(
    val context: Context,
    private val itemsList: ArrayList<ToolsFragmentRecyclerViewData>,
    private val onItemClicked: (selectedTool: SelectedTool) -> Unit
) :
    Adapter<ToolsFragmentRecyclerViewAdapter.MainViewHolder>() {

    inner class MainViewHolder(binding: ToolsFragmentRecyclerViewItemBinding) :
        ViewHolder(binding.root) {
        val view = binding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return MainViewHolder(
            ToolsFragmentRecyclerViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val item = itemsList[position]
        var view = holder.view
        view.fragmentToolsRecyclerViewImage.setImageResource(item.image)
        view.fragmentToolsRecyclerViewText.text = item.name
        view.root.setOnClickListener {
            onItemClicked.invoke(item.selectedTool)
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }
}