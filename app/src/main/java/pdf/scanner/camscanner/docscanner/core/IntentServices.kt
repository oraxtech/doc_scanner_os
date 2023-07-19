package pdf.scanner.camscanner.docscanner.core

import android.graphics.Bitmap

class IntentServices {


    companion object {
        private var imageType: ImageType? = null
        private var selectedImagesList: ArrayList<ImageState> = ArrayList()
        private var bitmap: Bitmap? = null
        private var isImageFromPDFConvertorActivity = false
        private var isActivityForResult = false
        private var selectedTool: SelectedTool? = null

        fun setIsActivityForResult(isActivityForResult : Boolean){
            Companion.isActivityForResult = isActivityForResult
        }

        fun setSelectedTool(selectedTool: SelectedTool){
            this.selectedTool = selectedTool
        }
        fun getSelectTool() = selectedTool
        fun getIsActivityForResult() = isActivityForResult

        fun setIsImageFromPDFActivity(isImageFromPDFFromImageActivity: Boolean){
            isImageFromPDFConvertorActivity =isImageFromPDFFromImageActivity
        }
        fun getIsImageFromPDFActivity() = isImageFromPDFConvertorActivity
        fun setImageType(imageType: ImageType) {
            Companion.imageType = imageType
        }

        fun getImageType(): ImageType? {
            return imageType
        }

        fun setSelectedImagesList(dataList: List<ImageState>) {

            for (i in dataList) {
                selectedImagesList.add(i)
            }
        }

        fun updateImageList(imageIndex: Int, newImageState: ImageState){
            selectedImagesList[imageIndex] = newImageState
        }

        fun removeAtSelectedImageList(index: Int){
            selectedImagesList.removeAt(index)
        }

        fun getSelectedImagesList(): ArrayList<ImageState> {
            return selectedImagesList
        }

        fun setBitmap(bitmap: Bitmap){
            Companion.bitmap = bitmap
        }

        fun getBitmap(): Bitmap?{
            return bitmap
        }

        fun clear(){
            imageType = null
            selectedImagesList.trimToSize()
            selectedImagesList.clear()
            bitmap?.recycle()
            isImageFromPDFConvertorActivity = false
            isActivityForResult = false
        }
    }
}