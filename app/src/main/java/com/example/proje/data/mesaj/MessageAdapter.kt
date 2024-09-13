// MessageAdapter.kt
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proje.R

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.textViewMessage.text = message.text

        // Set background color and alignment based on message sender
        val params = holder.textViewMessage.layoutParams as LinearLayout.LayoutParams
        if (message.isUser) { // Assuming `isUser` is true for the messages sent by the user
            holder.textViewMessage.setBackgroundResource(R.drawable.sent_message_bubble)
            params.gravity = android.view.Gravity.END
            holder.textViewMessage.setTextColor(Color.WHITE)
        } else { // Messages from others
            holder.textViewMessage.setBackgroundResource(R.drawable.received_message_bubble)
            params.gravity = android.view.Gravity.START
            holder.textViewMessage.setTextColor(Color.BLACK)
        }
        holder.textViewMessage.layoutParams = params
    }

    override fun getItemCount(): Int = messages.size
}
