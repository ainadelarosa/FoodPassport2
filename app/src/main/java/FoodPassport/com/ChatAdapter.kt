package FoodPassport.com

import android.content.Context
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val context: Context,
    private val messages: List<GroupChatActivity.ChatMessage>,
    private val currentUid: String
) : BaseAdapter() {

    override fun getCount() = messages.size
    override fun getItem(position: Int) = messages[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val msg = messages[position]
        val isMe = msg.senderId == currentUid

        val layoutId = if (isMe) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)

        view.findViewById<TextView>(R.id.tvMessageText).text = msg.text

        if (!isMe) {
            view.findViewById<TextView>(R.id.tvSenderName)?.text = msg.senderName
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        view.findViewById<TextView>(R.id.tvMessageTime).text =
            sdf.format(Date(msg.timestamp))

        return view
    }
}