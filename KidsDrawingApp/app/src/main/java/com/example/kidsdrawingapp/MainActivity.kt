package com.example.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageCurrentPaint : ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView.setBrushSize(20.toFloat())

        mImageCurrentPaint = colourPalette[1] as ImageButton
        mImageCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallette_selected)
        )

        ib_brush.setOnClickListener{
            showBrushSizes()
        }

        ib_eraser.setOnClickListener{
            drawingView.setEraser()
        }

        ib_clr.setOnClickListener{
            drawingView.clearDrawArea()
        }

        ib_gallery.setOnClickListener{
            if(isPermissionGiven()){
                val picPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(picPhotoIntent, GALLERY)
            }
            else{
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener{
            drawingView.onClickUndo()
        }

        ib_redo.setOnClickListener{
            drawingView.onClickRedo()
        }

        ib_save.setOnClickListener{
            if(isPermissionGiven()){
                BitMapAsyncClass(getBitMapFromView(frameLayOut)).execute()
            }else{
                requestStoragePermission()
            }
        }


    }


    private fun showBrushSizes(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Choose Brush Size")

        val superSmallBtn = brushDialog.superSmallBrush
        superSmallBtn.setOnClickListener {
            drawingView.setBrushSize(5.toFloat())
            brushDialog.dismiss()
        }


        val smallBtn = brushDialog.smallBrush
        smallBtn.setOnClickListener {
            drawingView.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        val medBtn = brushDialog.mediumBrush
        medBtn.setOnClickListener {
            drawingView.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.largeBrush
        largeBtn.setOnClickListener {
            drawingView.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun colorClicked(view : View){
        if(view !== mImageCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallette_selected)
            )
            mImageCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallette_normal)
            )
            mImageCurrentPaint = view
        }

    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"Need permission to save and access images",Toast.LENGTH_LONG).show()
        }
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission granted to read and save files",Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this,"Please grant permission to save and access images",Toast.LENGTH_LONG).show()
        }
    }

    private fun isPermissionGiven() : Boolean {
        var permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode==Activity.RESULT_OK){
            if(requestCode== GALLERY){
                try{
                    if(data!!.data !=null){
                     backgroundImage.visibility = View.VISIBLE
                     backgroundImage.setImageURI(data.data)
                    }else{
                        Toast.makeText(this,"Error parsing the image",Toast.LENGTH_LONG).show()
                    }
                }catch(e : Exception){
                    e.printStackTrace()
                }

            }
        }
    }

    private fun getBitMapFromView(view : View) : Bitmap{
        val generatedBitMap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvasDrawn = Canvas(generatedBitMap)
        val bgUsed = view.background
        if(bgUsed!=null)
            bgUsed.draw(canvasDrawn)
        else
            canvasDrawn.drawColor(Color.WHITE)
        view.draw(canvasDrawn)
        return generatedBitMap

    }

    private fun isDrawingAvailable() : Boolean {
        return drawingView.isDrawingAvailable()
    }

    private inner class BitMapAsyncClass(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        private lateinit var mProgressDialog : Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(mBitmap != null){
                try{
                    if(this@MainActivity.isDrawingAvailable()) {
                        val bytesGenerated = ByteArrayOutputStream()
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytesGenerated)
                        val imageFile =
                            File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")
                        val imageFOS = FileOutputStream(imageFile)
                        imageFOS.write(bytesGenerated.toByteArray())
                        imageFOS.close()
                        result = imageFile.absolutePath
                    }
                    else
                        Toast.makeText(this@MainActivity,"Draw something to save",Toast.LENGTH_LONG).show()
                }
                catch(e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if(!result!!.isEmpty())
                Toast.makeText(this@MainActivity,"Image Saved successfully at : $result",Toast.LENGTH_LONG).show()
            else
                Toast.makeText(this@MainActivity,"Saved successfully",Toast.LENGTH_LONG).show()

            if(!result!!.isEmpty()){
                MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                    path, uri -> val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.type = "image/png"
                    startActivity(
                        Intent.createChooser(
                            shareIntent,"Share"
                        )
                    )
                }
            }
        }

        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }


    }
    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}