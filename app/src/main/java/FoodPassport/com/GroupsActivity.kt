package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupsActivity : BaseActivity() {

    private lateinit var listViewGroups: ListView
    private lateinit var listViewInvites: ListView
    private lateinit var btnCreateGroup: Button
    private lateinit var emptyText: TextView
    private lateinit var sectionInvites: LinearLayout
    private lateinit var dividerGroups: View
    private lateinit var tvInvitesBadge: TextView

    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var groups = mutableListOf<GroupItem>()
    private var invites = mutableListOf<InviteItem>()

    private var groupsListener: ValueEventListener? = null
    private var invitesListener: ValueEventListener? = null
    private val userGroupsRef get() = db.getReference("user_groups/$uid")
    private val invitesRef get() = db.getReference("group_invites/$uid")

    data class GroupItem(
        val groupId: String,
        val groupName: String,
        var unreadCount: Int = 0,
        var lastMessage: String = ""
    )

    data class InviteItem(
        val groupId: String,
        val groupName: String,
        val invitedBy: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_groups)

        findViewById<TextView>(R.id.toolbarTitle).text = "Grupos"

        listViewGroups = findViewById(R.id.listViewGroups)
        listViewInvites = findViewById(R.id.listViewInvites)
        btnCreateGroup = findViewById(R.id.btnCreateGroup)
        emptyText = findViewById(R.id.emptyTextGroups)
        sectionInvites = findViewById(R.id.sectionInvites)
        dividerGroups = findViewById(R.id.dividerGroups)
        tvInvitesBadge = findViewById(R.id.tvInvitesBadge)

        btnCreateGroup.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        startListeningGroups()
        startListeningInvites()
    }

    override fun onPause() {
        super.onPause()
        stopListeningGroups()
        stopListeningInvites()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListeningGroups()
        stopListeningInvites()
    }

    // ── GRUPS ──────────────────────────────────────────────

    private fun startListeningGroups() {
        stopListeningGroups()

        groupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groups.clear()
                val groupIds = snapshot.children.mapNotNull { it.key }

                if (groupIds.isEmpty()) {
                    updateGroupsAdapter()
                    return
                }

                var loaded = 0
                groupIds.forEach { groupId ->
                    db.getReference("groups/$groupId/info/name").get()
                        .addOnSuccessListener { nameSnap ->
                            val name = nameSnap.getValue(String::class.java) ?: "Grupo sin nombre"
                            groups.add(GroupItem(groupId, name))
                            loaded++
                            if (loaded == groupIds.size) {
                                groups.sortBy { it.groupName }
                                loadUnreadMessages()
                            }
                        }
                        .addOnFailureListener {
                            loaded++
                            if (loaded == groupIds.size) {
                                groups.sortBy { it.groupName }
                                updateGroupsAdapter()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GroupsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        userGroupsRef.addValueEventListener(groupsListener!!)
    }

    private fun stopListeningGroups() {
        groupsListener?.let {
            userGroupsRef.removeEventListener(it)
            groupsListener = null
        }
    }

    private fun loadUnreadMessages() {
        if (groups.isEmpty()) {
            updateGroupsAdapter()
            return
        }

        var loaded = 0
        groups.forEach { groupItem ->
            // Primer llegir el lastSeen d'aquest grup per aquest usuari
            db.getReference("last_seen/$uid/${groupItem.groupId}").get()
                .addOnSuccessListener { lastSeenSnap ->
                    val lastSeen = lastSeenSnap.getValue(Long::class.java) ?: 0L

                    // Ara llegir els missatges
                    db.getReference("groups/${groupItem.groupId}/messages")
                        .orderByChild("timestamp")
                        .limitToLast(50)
                        .get()
                        .addOnSuccessListener { msgsSnap ->
                            var unread = 0
                            var lastMsg = ""

                            msgsSnap.children.forEach { msg ->
                                val timestamp = msg.child("timestamp").getValue(Long::class.java) ?: 0L
                                val senderId = msg.child("senderId").getValue(String::class.java) ?: ""
                                val text = msg.child("text").getValue(String::class.java) ?: ""

                                if (text.isNotEmpty()) lastMsg = text

                                // No llegit si: més nou que lastSeen i no és meu
                                if (timestamp > lastSeen && senderId != uid) {
                                    unread++
                                }
                            }

                            groupItem.unreadCount = unread
                            groupItem.lastMessage = lastMsg

                            loaded++
                            if (loaded == groups.size) {
                                updateGroupsAdapter()
                                updateMenuBadge()
                            }
                        }
                        .addOnFailureListener {
                            loaded++
                            if (loaded == groups.size) {
                                updateGroupsAdapter()
                            }
                        }
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded == groups.size) {
                        updateGroupsAdapter()
                    }
                }
        }
    }

    private fun updateMenuBadge() {
        // Actualitzar el badge del menú lateral amb el total de no llegits
        val totalUnread = groups.sumOf { it.unreadCount }
        val inviteCount = invites.size
        val total = totalUnread + inviteCount

        // Actualitzar el badge del menú base
        val badge = findViewById<TextView>(R.id.menuGroupsBadge)
        if (total > 0) {
            badge?.visibility = View.VISIBLE
            badge?.text = if (total > 9) "9+" else total.toString()
        } else {
            badge?.visibility = View.GONE
        }
    }

    private fun updateGroupsAdapter() {
        if (groups.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listViewGroups.visibility = View.GONE
            return
        }

        emptyText.visibility = View.GONE
        listViewGroups.visibility = View.VISIBLE

        val adapter = object : BaseAdapter() {
            override fun getCount() = groups.size
            override fun getItem(position: Int) = groups[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@GroupsActivity)
                    .inflate(R.layout.item_group, parent, false)

                val group = groups[position]

                view.findViewById<TextView>(R.id.tvGroupName).text = group.groupName
                view.findViewById<TextView>(R.id.tvGroupLastMessage).text =
                    if (group.lastMessage.isNotEmpty()) group.lastMessage else "Sin mensajes"

                val badge = view.findViewById<TextView>(R.id.tvMessageBadge)
                if (group.unreadCount > 0) {
                    badge.visibility = View.VISIBLE
                    badge.text = if (group.unreadCount > 9) "9+" else group.unreadCount.toString()
                } else {
                    badge.visibility = View.GONE
                }

                view.findViewById<Button>(R.id.btnDeleteGroup).setOnClickListener {
                    android.app.AlertDialog.Builder(this@GroupsActivity)
                        .setTitle("Eliminar grupo")
                        .setMessage("¿Seguro que quieres eliminar '${group.groupName}'?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            deleteGroup(group.groupId, group.groupName)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }

                view.setOnClickListener {
                    // Marcar com a llegit localment
                    group.unreadCount = 0
                    notifyDataSetChanged()
                    updateMenuBadge()

                    val intent = Intent(this@GroupsActivity, GroupChatActivity::class.java)
                    intent.putExtra("groupId", group.groupId)
                    intent.putExtra("groupName", group.groupName)
                    startActivity(intent)
                }

                return view
            }
        }

        listViewGroups.adapter = adapter
    }

    private fun deleteGroup(groupId: String, groupName: String) {
        db.getReference("groups/$groupId/members").get()
            .addOnSuccessListener { membersSnap ->
                val memberIds = membersSnap.children.mapNotNull { it.key }
                memberIds.forEach { memberId ->
                    db.getReference("user_groups/$memberId/$groupId").removeValue()
                }
                db.getReference("groups/$groupId").removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "'$groupName' eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    // ── INVITACIONS ────────────────────────────────────────

    private fun startListeningInvites() {
        stopListeningInvites()

        invitesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                invites.clear()
                snapshot.children.forEach { child ->
                    val groupId = child.key ?: return@forEach
                    val groupName = child.child("groupName").getValue(String::class.java) ?: ""
                    val invitedBy = child.child("invitedBy").getValue(String::class.java) ?: ""
                    invites.add(InviteItem(groupId, groupName, invitedBy))
                }
                updateInvitesSection()
                updateMenuBadge()
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        invitesRef.addValueEventListener(invitesListener!!)
    }

    private fun stopListeningInvites() {
        invitesListener?.let {
            invitesRef.removeEventListener(it)
            invitesListener = null
        }
    }

    private fun updateInvitesSection() {
        if (invites.isEmpty()) {
            sectionInvites.visibility = View.GONE
            dividerGroups.visibility = View.GONE
            return
        }

        sectionInvites.visibility = View.VISIBLE
        dividerGroups.visibility = View.VISIBLE
        tvInvitesBadge.text = invites.size.toString()

        val adapter = object : BaseAdapter() {
            override fun getCount() = invites.size
            override fun getItem(position: Int) = invites[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@GroupsActivity)
                    .inflate(R.layout.item_invite, parent, false)

                val invite = invites[position]
                view.findViewById<TextView>(R.id.tvInviteGroupName).text = invite.groupName
                view.findViewById<TextView>(R.id.tvInvitedBy).text =
                    "Invitado por: ${invite.invitedBy}"

                view.findViewById<Button>(R.id.btnAcceptInvite).setOnClickListener {
                    acceptInvite(invite)
                }
                view.findViewById<Button>(R.id.btnRejectInvite).setOnClickListener {
                    rejectInvite(invite)
                }

                return view
            }
        }

        listViewInvites.adapter = adapter
    }

    private fun acceptInvite(invite: InviteItem) {
        db.getReference("groups/${invite.groupId}/members/$uid").setValue(true)
            .addOnSuccessListener {
                db.getReference("user_groups/$uid/${invite.groupId}").setValue(true)
                    .addOnSuccessListener {
                        db.getReference("group_invites/$uid/${invite.groupId}").removeValue()
                        Toast.makeText(this, "Te has unido a '${invite.groupName}'", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al aceptar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectInvite(invite: InviteItem) {
        db.getReference("group_invites/$uid/${invite.groupId}").removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Invitación rechazada", Toast.LENGTH_SHORT).show()
            }
    }
}