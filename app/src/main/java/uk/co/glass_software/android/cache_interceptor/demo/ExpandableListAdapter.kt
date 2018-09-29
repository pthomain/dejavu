package uk.co.glass_software.android.cache_interceptor.demo


import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus.*
import java.text.SimpleDateFormat
import java.util.*

internal class ExpandableListAdapter(context: Context,
                                     private val factCallback: (String) -> Unit)
    : BaseExpandableListAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val simpleDateFormat = SimpleDateFormat("MM/dd/YY hh:mm:ss")

    private val headers: LinkedList<String> = LinkedList()
    private val logs: LinkedList<String> = LinkedList()
    private val children: LinkedHashMap<String, List<String>> = LinkedHashMap()

    private var callStart = 0L

    fun onStart() {
        headers.clear()
        children.clear()
        logs.clear()

        callStart = System.currentTimeMillis()

        notifyDataSetChanged()
    }

    fun showCatFact(catFactResponse: CatFactResponse) {
        val metadata = catFactResponse.metadata!!
        val cacheToken = metadata.cacheToken
        val exception = metadata.exception
        val operation = cacheToken.instruction.operation.type

        val elapsed = "${operation.name} -> ${cacheToken.status} (${metadata.callDuration}ms)"
        val info = ArrayList<String>()
        val header: String

        if (exception != null) {
            header = "An error occurred: $elapsed"

            info.add("Description: " + exception.description)
            info.add("Message: " + exception.message)
            info.add("Cause: " + exception.cause)
        } else {
            factCallback(catFactResponse.fact!!)
            header = elapsed

            info.add("Cache token instruction: $operation")
            info.add("Cache token status: ${cacheToken.status} (coming from ${getOrigin(cacheToken.status)})")
            info.add("Cache token cache date: " + simpleDateFormat.format(cacheToken.cacheDate))

            if (operation is Expiring) {
                info.add("Cache token expiry date: "
                        + simpleDateFormat.format(cacheToken.expiryDate)
                        + " (TTL: "
                        + (operation.durationInMillis * 1000).toInt()
                        + "s)"
                )
            }

            info.add("Cat fact: ${catFactResponse.fact!!}")
        }

        headers.add(header)
        children[header] = info

        notifyDataSetChanged()
    }

    fun onComplete() {
        val header = "Log output (total: " + (System.currentTimeMillis() - callStart) + "ms)"
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
            }

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
                        findViewById<TextView>(R.id.listItem).text = getChild(groupPosition, childPosition)
                    }

    override fun getChildrenCount(groupPosition: Int) = children[headers[groupPosition]]!!.size

    override fun getGroup(groupPosition: Int) = headers[groupPosition]

    override fun getGroupCount() = headers.size

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun hasStableIds() = false

    override fun isChildSelectable(groupPosition: Int,
                                   childPosition: Int) = false
}
