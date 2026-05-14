package FoodPassport.com

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Vista personalitzada que dibuixa un cercle de color amb les inicials del nom.
 * No necessita cap recurs extern ni Firebase Storage.
 * El color es genera automàticament a partir del nom, sempre és el mateix per al mateix nom.
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var initials = "?"
    private var bgColor = Color.parseColor("#9E0202")


    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }


    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }


    private val colors = listOf(
        "#E53935",
        "#8E24AA",
        "#1E88E5",
        "#00897B",
        "#FB8C00",
        "#6D4C41", 
        "#546E7A",
        "#D81B60",
        "#43A047",
        "#F4511E"
    )

    // Assigna el nom i recalcula les inicials i el color
    fun setName(name: String) {
        initials = getInitials(name)
        bgColor = getColorForName(name)
        invalidate() // Redibuixar la vista
    }

    // Extrau les inicials del nom: "Aina de la Rosa" → "AR"
    private fun getInitials(name: String): String {
        val words = name.trim().split(" ").filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(1).uppercase()
            // Primera lletra del primer i de l'últim mot
            else -> "${words.first().take(1)}${words.last().take(1)}".uppercase()
        }
    }

    // Genera sempre el mateix color per al mateix nom
    private fun getColorForName(name: String): Int {
        if (name.isBlank()) return Color.parseColor(colors[0])
        // Usar el hash del nom per triar el color de la llista
        val index = Math.abs(name.trim().hashCode()) % colors.size
        return Color.parseColor(colors[index])
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy)

        // Dibuixar el cercle de fons
        bgPaint.color = bgColor
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Dibuixar les inicials centrades
        textPaint.textSize = radius * 0.7f
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(initials, cx, textY, textPaint)
    }
}