package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    fun setupDrawer(contentLayoutId: Int): DrawerLayout {
        setContentView(R.layout.activity_base)

        drawerLayout = findViewById(R.id.drawerLayout)

        val frame = findViewById<FrameLayout>(R.id.frameContent)
        layoutInflater.inflate(contentLayoutId, frame, true)

        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupMenuItems()
        return drawerLayout
    }

    private fun setupMenuItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        findViewById<LinearLayout>(R.id.menuCountries).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is CountriesActivity) {
                startActivity(Intent(this, CountriesActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.menuFavorites).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is FavoritesActivity) {
                startActivity(Intent(this, FavoritesActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.menuCountryList).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is CountryListActivity) {
                startActivity(Intent(this, CountryListActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.menuCuriosity).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is CountryCuriosityActivity) {
                startActivity(Intent(this, CountryCuriosityActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.menuLogout).setOnClickListener {
            drawerLayout.closeDrawers()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        if (uid != null) {
            val db = com.google.firebase.database.FirebaseDatabase
                .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
                .getReference("users/$uid/name")
            db.get().addOnSuccessListener { snap ->
                val name = snap.getValue(String::class.java) ?: "Usuario"
                findViewById<TextView>(R.id.menuUserName).text = name
            }
        }
    }
}