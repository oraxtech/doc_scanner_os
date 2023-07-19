package pdf.scanner.camscanner.docscanner.core

import android.content.Context
import pdf.scanner.camscanner.docscanner.R

object Constants {
    fun getToolsFragmentItemsList(context: Context): ArrayList<ToolsFragmentRecyclerViewData> {
        val itemsList: ArrayList<ToolsFragmentRecyclerViewData> = ArrayList()

        itemsList.add(
            ToolsFragmentRecyclerViewData(
                1,
                context.getString(R.string.fragment_tools_name_text_merge_pdfs),
                R.drawable.ic_merge_pdf,
               SelectedTool.MERGE
            )
        )
        itemsList.add(
            ToolsFragmentRecyclerViewData(
                2,
                context.getString(R.string.fragment_tools_name_text_extract_pdf_pages),
                R.drawable.ic_extract_pdf_pages,
                SelectedTool.EXTRACT
            )
        )
        itemsList.add(
            ToolsFragmentRecyclerViewData(
                3,
                context.getString(R.string.fragment_tools_name_text_protect_pdf),
                R.drawable.ic_protect_pdf,
                SelectedTool.PROTECT
            )
        )
        itemsList.add(
            ToolsFragmentRecyclerViewData(
                4,
                context.getString(R.string.fragment_tools_name_text_reorder_pdf),
                R.drawable.ic_reorder_pdf_pages,
                SelectedTool.REORDER
            )
        )
        itemsList.add(
            ToolsFragmentRecyclerViewData(
                5,
                context.getString(R.string.fragment_tools_name_text_import_pdfs),
                R.drawable.ic_import_pdf,
                SelectedTool.IMPORT
            )
        )

        return itemsList
    }

}