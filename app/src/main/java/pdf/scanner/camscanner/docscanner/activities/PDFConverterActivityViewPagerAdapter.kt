package pdf.scanner.camscanner.docscanner.activities

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import pdf.scanner.camscanner.docscanner.R
import pdf.scanner.camscanner.docscanner.core.ImageState

class PDFConverterActivityViewPagerAdapter(
    private val context: Context,
    private val imagesList: ArrayList<ImageState>
) : PagerAdapter() {

    private val mLayoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private lateinit var imageView : ImageView

    override fun getCount(): Int {
        return imagesList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as LinearLayout
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }


    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val itemView = mLayoutInflater.inflate(R.layout.pdf_converter_image_view, container, false)
        imageView = itemView.findViewById(R.id.pdf_converter_image_view)
        val setBitmap = if(imagesList[position].editedBitmap != null){
            imagesList[position].editedBitmap
        }else{
            imagesList[position].bitmap
        }
//        Glide.with(context)
//            .load(setBitmap).disallowHardwareConfig().into(imageView)

       imageView.setImageBitmap(setBitmap)

        container.addView(itemView)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as LinearLayout)
    }
}