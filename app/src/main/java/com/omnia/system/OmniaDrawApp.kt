
package com.omnia.system

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OmniaDrawApp(
    private val activity: Activity
) : LinearLayout(activity) {

    private lateinit var drawingView: DrawingView

    private val palette = intArrayOf(
        Color.BLACK,
        Color.WHITE,
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(35, 38, 43))
        createToolbar()
        createDrawingArea()
    }

    private fun createToolbar() {
        val controls = LinearLayout(activity).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.rgb(43, 47, 54))
        }

        val undoButton = makeButton("Undo")
        undoButton.setOnClickListener {
            drawingView.undo()
        }

        val clearButton = makeButton("Clear")
        clearButton.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle("Clear drawing")
                .setMessage("Do you want to clear the canvas?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    drawingView.clear()
                }
                .show()
        }

        val saveButton = makeButton("Save")
        saveButton.setOnClickListener {
            saveImage()
        }

        controls.addView(undoButton, buttonParams())
        controls.addView(clearButton, buttonParams())
        controls.addView(saveButton, buttonParams())

        addView(
            controls,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val colorScroll = ScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
        }

        val colorRow = LinearLayout(activity).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }

        for (color in palette) {
            val colorButton = Button(activity).apply {
                text = "●"
                textSize = 19f
                setTextColor(color)
                setOnClickListener {
                    drawingView.setColor(color)
                }
            }

            colorRow.addView(
                colorButton,
                LayoutParams(dp(45), dp(44))
            )
        }

        colorScroll.addView(colorRow)

        addView(
            colorScroll,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        val brushRow = LinearLayout(activity).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(43, 47, 54))
        }

        val sizes = intArrayOf(4, 10, 20)

        for (size in sizes) {
            val button = makeButton("${size}px")
            button.setOnClickListener {
                drawingView.setBrushSize(size.toFloat())
            }

            brushRow.addView(button, buttonParams())
        }

        addView(
            brushRow,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )
    }

    private fun createDrawingArea() {
        drawingView = DrawingView(activity)

        addView(
            drawingView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun makeButton(label: String): Button {
        return Button(activity).apply {
            text = label
            textSize = 11f
            isAllCaps = false
        }
    }

    private fun buttonParams(): LayoutParams {
        return LayoutParams(
            0,
            dp(44),
            1f
        ).apply {
            setMargins(dp(3), 0, dp(3), 0)
        }
    }

    private fun saveImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(
                activity,
                "Saving requires Android 10 or newer.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (drawingView.width <= 0 || drawingView.height <= 0) {
            Toast.makeText(
                activity,
                "Canvas is not ready.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val bitmap = Bitmap.createBitmap(
            drawingView.width,
            drawingView.height,
            Bitmap.Config.ARGB_8888
        )

        var imageUri: Uri? = null
        var saved = false

        try {
            drawingView.draw(Canvas(bitmap))

            val fileName = "OMNIA_" +
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date()) + ".png"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/OMNIA"
                )
            }

            imageUri = activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            val uri = imageUri

            if (uri != null) {
                val stream = activity.contentResolver.openOutputStream(uri)

                if (stream != null) {
                    stream.use {
                        saved = bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            it
                        )
                    }
                }
            }

            if (saved) {
                Toast.makeText(
                    activity,
                    "Saved to Pictures/OMNIA",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                imageUri?.let {
                    activity.contentResolver.delete(it, null, null)
                }

                Toast.makeText(
                    activity,
                    "Could not save image.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            imageUri?.let {
                activity.contentResolver.delete(it, null, null)
            }

            Toast.makeText(
                activity,
                "Save error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

        } finally {
            bitmap.recycle()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

private class DrawingView(
    activity: Activity
) : View(activity) {

    private data class Stroke(
        val path: Path,
        val color: Int,
        val width: Float
    )

    private val strokes = mutableListOf<Stroke>()

    private var currentPath: Path? = null
    private var currentColor = Color.BLACK
    private var brushSize = 10f

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        setBackgroundColor(Color.WHITE)
    }

    fun setColor(color: Int) {
        currentColor = color
    }

    fun setBrushSize(size: Float) {
        brushSize = size
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.lastIndex)
            invalidate()
        }
    }

    fun clear() {
        strokes.clear()
        currentPath = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)

        for (stroke in strokes) {
            paint.color = stroke.color
            paint.strokeWidth = stroke.width
            canvas.drawPath(stroke.path, paint)
        }

        currentPath?.let {
            paint.color = currentColor
            paint.strokeWidth = brushSize
            canvas.drawPath(it, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply {
                    moveTo(event.x, event.y)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(event.x, event.y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                currentPath?.let {
                    strokes.add(
                        Stroke(
                            Path(it),
                            currentColor,
                            brushSize
                        )
                    )
                }

                currentPath = null
                invalidate()
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                currentPath = null
                invalidate()
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}