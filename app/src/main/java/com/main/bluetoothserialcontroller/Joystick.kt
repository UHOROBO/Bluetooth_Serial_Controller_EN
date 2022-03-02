package com.main.bluetoothserialcontroller

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


class Joystick : SurfaceView, SurfaceHolder.Callback, View.OnTouchListener{

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var hatRadius = 0f
    //private var joystickCallback: JoystickListener? = null

    var xPercent = 0f
        private set
    var yPercent = 0f
        private set

    constructor(context: Context) : super(context) {
        holder.setFormat(PixelFormat.TRANSPARENT)
        setZOrderOnTop(true)

        holder.addCallback(this)
        setOnTouchListener(this)
        //if (context is JoystickListener) joystickCallback = context
    }

    constructor(context: Context, attributes: AttributeSet?) :super(context, attributes) {
        holder.setFormat(PixelFormat.TRANSPARENT)
        setZOrderOnTop(true)

        holder.addCallback(this)
        setOnTouchListener(this)
        //if (context is JoystickListener) joystickCallback = context
    }

    constructor(context: Context, attributes: AttributeSet?, style : Int) : super(context, attributes,style) {
        holder.setFormat(PixelFormat.TRANSPARENT)
        setZOrderOnTop(true)

        holder.addCallback(this)
        setOnTouchListener(this)
        //if (context is JoystickListener) joystickCallback = context
    }

//////////////////////////////////////////////////////////////

    override fun surfaceCreated(holder: SurfaceHolder) {
        setupDimensions()
        drawJoystick(centerX, centerY)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    /////////////////////////////////////////////////////////////////////

    private fun setupDimensions() {
        centerX = width / 2.0f
        centerY = height / 2.0f
        baseRadius = min(width, height) / 4f
        hatRadius = min(width, height) / 6f
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (v == this) {
            if (event!!.action != MotionEvent.ACTION_UP) {
                //Log.d("MainActivity", "x ${event.x} y  ${event.y}")
                val displacement =
                    sqrt((event.x - centerX).toDouble().pow(2.0) + (event.y - centerY).toDouble()
                        .pow(2.0))
                        .toFloat()
                if (displacement < baseRadius) {
                    drawJoystick(event.x, event.y)
                    setPercentXY((event.x - centerX) / baseRadius,(event.y - centerY) / baseRadius)
                    //joystickCallback?.onJoystickMoved(xPercent,yPercent,id)
                    /*joystickCallback!!.onJoystickMoved((event.x - centerX) / baseRadius,
                        (event.y - centerY) / baseRadius,
                        id)*/
                } else {
                    val ratio = baseRadius / displacement
                    val constrainedX: Float = centerX + (event.x - centerX) * ratio
                    val constrainedY: Float = centerY + (event.y - centerY) * ratio
                    drawJoystick(constrainedX, constrainedY)
                    setPercentXY((constrainedX - centerX) / baseRadius, (constrainedY - centerY) / baseRadius)
                    //joystickCallback?.onJoystickMoved(xPercent,yPercent,id)
                    /*joystickCallback!!.onJoystickMoved((constrainedX - centerX) / baseRadius,
                        (constrainedY - centerY) / baseRadius,
                        id)*/
                }
            } else {
                drawJoystick(centerX, centerY)
                setPercentXY(0f,0f)
                //joystickCallback?.onJoystickMoved(0f, 0f, id)
            }
        }
        return true
    }

    private fun drawJoystick(newX: Float, newY: Float) {
        if (holder.surface.isValid) {
            val myCanvas = this.holder.lockCanvas() //Stuff to draw
            val colors = Paint()
            myCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // Clear the BG
            //myCanvas.drawColor(0, PorterDuff.Mode.CLEAR) // Clear the BG

            //Draw the base first before shading

            colors.color = Color.GRAY
            myCanvas.drawCircle(centerX, centerY, baseRadius, colors)

            //Drawing the joystick hat
            //colors.setARGB(255, 0, 0, 255) //Change the joystick color for shading purposes
            //colors.color = Color.LTGRAY
            colors.setARGB(255,0x00,0x4d,0x40)
            myCanvas.drawCircle(newX, newY, hatRadius, colors) //Draw the shading for the hat
            holder.unlockCanvasAndPost(myCanvas) //Write the new drawing to the SurfaceView
        }
    }

    private fun setPercentXY(xPercent: Float, yPercent: Float){
        this.xPercent = xPercent
        this.yPercent = yPercent
    }

    interface JoystickListener {
        fun onJoystickMoved(xPercent: Float, yPercent: Float, id: Int)
    }
}