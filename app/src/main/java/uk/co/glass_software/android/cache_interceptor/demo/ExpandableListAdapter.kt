package uk.co.glass_software.android.cache_interceptor.demo


import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.*
import java.text.SimpleDateFormat
import java.util.*

internal class ExpandableListAdapter(context: Context)
    : BaseExpandableListAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val simpleDateFormat = SimpleDateFormat("MM/dd/YY hh:mm:ss")

    private val headers: LinkedList<String> = LinkedList()
    private val logs: LinkedList<String> = LinkedList()
    private val children: LinkedHashMap<String, List<*>> = LinkedHashMap()

    private var callStart = 0L

    fun onStart(instruction: CacheInstruction) {
        headers.clear()
        children.clear()
        logs.clear()

        callStart = System.currentTimeMillis()

        val header = "Retrofit Call"
        headers.add(header)
        children[header] = listOf(instruction)

        notifyDataSetChanged()
    }

    fun showCatFact(catFactResponse: CatFactResponse) {
        val metadata = catFactResponse.metadata
        val cacheToken = metadata.cacheToken
        val exception = metadata.exception
        val operation = cacheToken.instruction.operation.type
        val callDuration = metadata.callDuration

        val elapsed = "${operation.name} -> ${cacheToken.status} (${callDuration.total}ms)"
        val duration = "Call duration: disk = ${callDuration.disk}ms, network = ${callDuration.network}ms, total = ${callDuration.total}ms"

        val info = ArrayList<String>()
        val header: String

        info.add("Cache token instruction: $operation")
        info.add("Cache token status: ${cacheToken.status} (coming from ${getOrigin(cacheToken.status)})")

        if (exception != null) {
            header = "An error occurred: $elapsed"

            info.add("Description: " + exception.description)
            info.add("Message: " + exception.message)
            info.add("Cause: " + exception.cause)
            info.add(duration)
        } else {
            header = elapsed

            info.add("Cache token cache date: " + simpleDateFormat.format(cacheToken.cacheDate))
            info.add(duration)

            if (operation is Expiring) {
                info.add("Cache token expiry date: "
                        + simpleDateFormat.format(cacheToken.expiryDate)
                        + " (TTL: "
                        + (operation.durationInMillis?.times(1000)?.toInt() ?: "N/A")
                        + "s)"
                )
            }
        }

        headers.add(header)
        children[header] = info

        val catFactHeader = "Cat Fact (${catFactResponse.metadata.cacheToken.status})"
        headers.add(catFactHeader)
        children[catFactHeader] = listOf(catFactResponse.fact ?: "N/A")

        notifyDataSetChanged()
    }

    fun onComplete() {
        val header = "Log Output (Total: " + (System.currentTimeMillis() - callStart) + "ms)"
        headers.add(header)
        children[header] = logs
        notifyDataSetChanged()
    }

    private fun getOrigin(status: CacheStatus) =
            when (status) {
                INSTRUCTION -> "instruction"
                NOT_CACHED,
                FRESH,
                REFRESHED -> "network"
                CACHED,
                STALE,
                COULD_NOT_REFRESH -> "disk"
                EMPTY -> "network or disk"
            } + ", " + (if (status.isFinal) "final" else "non-final")

    fun log(output: String) {
        logs.addLast(output)
    }

    override fun getChild(groupPosition: Int,
                          childPosition: Int) = headers[groupPosition].let {
        children[it]!![childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()

    override fun getGroupView(groupPosition: Int,
                              isExpanded: Boolean,
                              convertView: View?,
                              parent: ViewGroup) =
            (convertView ?: inflater.inflate(R.layout.list_group, parent, false))
                    .apply {
                        findViewById<TextView>(R.id.listHeader).apply {
                            setTypeface(null, Typeface.BOLD)
                            text = getGroup(groupPosition)
                        }
                    }!!

    override fun getChildView(groupPosition: Int,
                              childPosition: Int,
                              isLastChild: Boolean,
                              convertView: View?,
                              parent: ViewGroup): View =
            (convertView ?: inflater.inflate(R.layout.list_item, parent, false))
                    .apply {
                        val child = getChild(groupPosition, childPosition)
                        val text = findViewById<TextView>(R.id.listItem)
                        val instruction = findViewById<InstructionView>(R.id.instruction)

                        if (child is String) {
                            text.visibility = View.VISIBLE
                            instruction.visibility = View.GONE
                            text.text = child
                        } else if (child is CacheInstruction) {
                            text.visibility = View.GONE
                            instruction.visibility = View.VISIBLE
                            instruction.setInstruction(child)
                        }
                    }

    override fun getChildrenCount(groupPosition: Int) = children[headers[groupPosition]]!!.size

    override fun getGroup(groupPosition: Int) = headers[groupPosition]

    override fun getGroupCount() = headers.size

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun hasStableIds() = false

    override fun isChildSelectable(groupPosition: Int,
                                   childPosition: Int) = false
}
