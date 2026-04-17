package FoodPassport.com

import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupChatActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: ImageButton

    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var groupId = ""
    private var groupName = ""
    private var myName = ""
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var messagesRef: DatabaseReference
    private var messagesListener: ValueEventListener? = null
    private var isSending = false

    data class ChatMessage(
        val messageId: String = "",
        val text: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val timestamp: Long = 0L
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_group_chat)

        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: ""

        findViewById<TextView>(R.id.toolbarTitle).text = groupName

        listView = findViewById(R.id.listViewChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSendMessage)

        db.getReference("users/$uid/name").get().addOnSuccessListener { snap ->
            myName = snap.getValue(String::class.java) ?: "Usuario"
        }

        adapter = ChatAdapter(this, messages, uid)
        listView.adapter = adapter

        messagesRef = db.getReference("groups/$groupId/messages")

        btnSend.setOnClickListener { sendMessage() }
    }

    override fun onResume() {
        super.onResume()
        startListening()
    }

    override fun onPause() {
        super.onPause()
        stopListening()
        // Guardar el timestamp actual com a últim vist
        markAsRead()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    private fun startListening() {
        stopListening()

        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                snapshot.children.forEach { child ->
                    val msg = ChatMessage(
                        messageId = child.key ?: "",
                        text = child.child("text").getValue(String::class.java) ?: "",
                        senderId = child.child("senderId").getValue(String::class.java) ?: "",
                        senderName = child.child("senderName").getValue(String::class.java) ?: "",
                        timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    )
                    messages.add(msg)
                }
                messages.sortBy { it.timestamp }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    listView.smoothScrollToPosition(messages.size - 1)
                }
                // Marcar com a llegit cada vegada que arriben missatges nous
                markAsRead()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GroupChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        messagesRef.addValueEventListener(messagesListener!!)
    }

    private fun stopListening() {
        messagesListener?.let {
            messagesRef.removeEventListener(it)
            messagesListener = null
        }
    }

    private fun markAsRead() {
        // Guardar el timestamp actual a Firebase per saber fins on hem llegit
        db.getReference("last_seen/$uid/$groupId")
            .setValue(System.currentTimeMillis())
    }

    private fun sendMessage() {
        if (isSending) return
        val text = editMessage.text.toString().trim()
        if (text.isEmpty()) return

        isSending = true
        btnSend.isEnabled = false

        val msgRef = messagesRef.push()
        val message = mapOf(
            "text" to text,
            "senderId" to uid,
            "senderName" to myName,
            "timestamp" to System.currentTimeMillis()
        )
        msgRef.setValue(message)
            .addOnSuccessListener {
                editMessage.setText("")
                isSending = false
                btnSend.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
                isSending = false
                btnSend.isEnabled = true
            }
    }
}