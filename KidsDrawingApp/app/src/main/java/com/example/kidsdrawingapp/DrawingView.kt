package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs){
    private var mDrawPath : CustomPath? = null
    private var mBitMap : Bitmap? = null
    private var mDrawPaint : Paint? = null
    private var mCanvasPaint : Paint? = null
    private var mBrushSize : Float = 0.toFloat()
    private var mColor = Color.BLACK;
    private var mCanvas : Canvas? = null
    private var mDrawPaths = ArrayList<CustomPath>()
    private var mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(mColor,mBrushSize)
        mDrawPaint!!.color = mColor
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitMap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitMap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitMap!!,0f,0f,mCanvasPaint)

        for(path in mDrawPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path,mDrawPaint!!)
        }
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchXCoordinate = event?.x
        val touchYCoordinate = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = mColor
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset()
                mDrawPath!!.moveTo(touchXCoordinate!!,touchYCoordinate!!)
            }

            MotionEvent.ACTION_MOVE -> {
                mDrawPath!!.lineTo(touchXCoordinate!!,touchYCoordinate!!)
            }

            MotionEvent.ACTION_UP -> {
                mDrawPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(mColor,mBrushSize)
            }
            else -> return false
        }
        invalidate()


        return true
        //return super.onTouchEvent(event)
    }

    fun setBrushSize(newSize : Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
        mColor = Color.BLACK
    }

    fun setColor(newColor : String){
        mColor = Color.parseColor(newColor)
        mDrawPaint!!.color = mColor
    }
    fun setEraser(){
        mColor = Color.WHITE
    }

    fun isDrawingAvailable() : Boolean {
        return mDrawPaths.size>0
    }
    fun clearDrawArea(){
//        mDrawPaths.clear()
//        mDrawPath!!.reset()
        mDrawPaths.clear()
        mUndoPaths.clear()
        invalidate()
    }

    fun onClickUndo(){
        if(mDrawPaths.size > 0){
            mUndoPaths.add(mDrawPaths.removeAt(mDrawPaths.size-1))
            invalidate()

        }
    }

    fun onClickRedo(){
        if(mUndoPaths.size > 0){
            mDrawPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }

    internal inner class CustomPath (var color:Int,var brushThickness : Float) : Path() {

    }


}