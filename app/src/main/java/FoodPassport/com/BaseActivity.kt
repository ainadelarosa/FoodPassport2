package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

open class BaseActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
    private val activeListeners = mutableMapOf<DatabaseReference, ValueEventListener>()

    fun setupDrawer(contentLayoutId: Int): DrawerLayout {
        setContentView(R.layout.activity_base)
        drawerLayout = findViewById(R.id.drawerLayout)

        // Fer que el toolbar respecti la status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val toolbar = findViewById<LinearLayout>(R.id.baseToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

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

    override fun onResume() {
        super.onResume()
        listenNotifications()
    }

    override fun onPause() {
        super.onPause()
        stopAllListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllListeners()
    }

    private fun listen(ref: DatabaseReference, listener: ValueEventListener) {
        ref.addValueEventListener(listener)
        activeListeners[ref] = listener
    }

    private fun stopAllListeners() {
        activeListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
    }

    private fun setupMenuItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        findViewById<LinearLayout>(R.id.menuCountries).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is CountriesActivity)
                startActivity(Intent(this, CountriesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuFavorites).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is FavoritesActivity)
                startActivity(Intent(this, FavoritesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuCountryList).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is CountryListActivity)
                startActivity(Intent(this, CountryListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuCuriosity).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is CountryCuriosityActivity)
                startActivity(Intent(this, CountryCuriosityActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuGroups).setOnClickListener {
            drawerLayout.closeDrawers()
            if (this !is GroupsActivity)
                startActivity(Intent(this, GroupsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuLogout).setOnClickListener {
            drawerLayout.closeDrawers()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        if (uid != null) {
            db.getReference("users/$uid/name").get()
                .addOnSuccessListener { snap ->
                    val name = snap.getValue(String::class.java) ?: "Usuario"
                    findViewById<TextView>(R.id.menuUserName).text = name
                }
        }
    }

    private fun listenNotifications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val badge = findViewById<TextView>(R.id.menuGroupsBadge) ?: return

        stopAllListeners()

        var inviteCount = 0
        var unreadMsgCount = 0

        fun updateBadge() {
            val total = inviteCount + unreadMsgCount
            runOnUiThread {
                if (total > 0) {
                    badge.visibility = View.VISIBLE
                    badge.text = if (total > 9) "9+" else total.toString()
                } else {
                    badge.visibility = View.GONE
                }
            }
        }

        fun listenGroupMessages(groupId: String) {
            val lastSeenRef = db.getReference("last_seen/$uid/$groupId")
            val msgsRef = db.getReference("groups/$groupId/messages")

            val msgsListener = object : ValueEventListener {
                override fun onDataChange(msgsSnap: DataSnapshot) {
                    lastSeenRef.get().addOnSuccessListener { lastSeenSnap ->
                        val lastSeen = lastSeenSnap.getValue(Long::class.java) ?: 0L
                        var unread = 0
                        msgsSnap.children.forEach { msg ->
                            val ts = msg.child("timestamp").getValue(Long::class.java) ?: 0L
                            val sender = msg.child("senderId").getValue(String::class.java) ?: ""
                            if (ts > lastSeen && sender != uid) {
                                unread++
                            }
                        }
                        unreadMsgCount = unread
                        updateBadge()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }

            listen(msgsRef, msgsListener)
        }

        // Listener d'invitacions
        val invRef = db.getReference("group_invites/$uid")
        val invListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                inviteCount = snapshot.childrenCount.toInt()
                updateBadge()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        listen(invRef, invListener)

        // Listener dels grups
        val groupsRef = db.getReference("user_groups/$uid")
        val groupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupIds = snapshot.children.mapNotNull { it.key }

                if (groupIds.isEmpty()) {
                    unreadMsgCount = 0
                    updateBadge()
                    return
                }

                groupIds.forEach { groupId ->
                    val msgsRef = db.getReference("groups/$groupId/messages")
                    if (!activeListeners.containsKey(msgsRef)) {
                        listenGroupMessages(groupId)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        listen(groupsRef, groupsListener)
    }
}