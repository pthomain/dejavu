package uk.co.glass_software.android.cache_interceptor.demo


import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import io.reactivex.Observable
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse
import java.text.SimpleDateFormat
import java.util.*

internal class ExpandableListAdapter(context: Context,
                                     private val jokeCallback: (String) -> Unit,
                                     private val onComplete: () -> Unit)
    : BaseExpandableListAdapter() {

    private val headers: LinkedList<String> = LinkedList()
    private val logs: LinkedList<String> = LinkedList()
    private val children: LinkedHashMap<String, List<String>> = LinkedHashMap()
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("hh:mm:ss")

    fun loadJoke(observable: Observable<out JokeResponse>) {
        headers.clear()
        children.clear()
        logs.clear()
        val start = System.currentTimeMillis()
        observable.doOnComplete { onComplete(start) }
                .subscribe { joke -> onJokeReady(start, joke) }
    }

    private fun onJokeReady(start: Long,
                            jokeResponse: JokeResponse) {
//        val cacheToken = jokeResponse.metadata.getCacheToken()
//        val ellapsed = cacheToken.status + ", " + (System.currentTimeMillis() - start) + "ms"
        val info = ArrayList<String>()
        val header: String

//        if (jokeResponse.getMetadata().hasError()) {
//            header = "An error occurred: $ellapsed"
//
//            val error = jokeResponse.getMetadata().getError()
//            info.add("Description: " + error.description!!)
//            info.add("Message: " + error.message)
//            info.add("Cause: " + error.cause)
//        } else {
            val joke = Html.fromHtml(jokeResponse.value!!.joke).toString()
            jokeCallback(joke)
//            header = ellapsed

//            info.add("Cache token status: " + cacheToken.status)
//            info.add("Cache token cache date: " + simpleDateFormat.format(cacheToken.cacheDate))
//            info.add("Cache token expiry date: "
//                    + simpleDateFormat.format(cacheToken.expiryDate)
//                    + " (TTL: "
//                    + (jokeResponse.getTtlInMinutes() * 60f) as Int
//                    + "s)"
//            )

            info.add("Joke: $joke")
//        }

//        headers.add(header)
//        children[header] = info

        notifyDataSetChanged()
    }

    fun log(output: String) {
        logs.addLast(output)
    }

    private fun onComplete(start: Long) {
        val header = "Log output (total: " + (System.currentTimeMillis() - start) + "ms)"
        headers.add(header)
        children[header] = logs
        notifyDataSetChanged()
        onComplete()
    }

    override fun getChild(groupPosition: Int,
                          childPosition: Int) = headers[groupPosition].let {
        children[it]!![childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()

    override fun getGroupView(groupPosition: Int,
                              isExpanded: Boolean,
                              convertView: View?,
                              parent: ViewGroup): View =
            convertView
                    ?.let { inflater.inflate(R.layout.list_group, null) }!!
                    .apply {
                        val headerTitle = getGroup(groupPosition) as String
                        val lblListHeader = findViewById<TextView>(R.id.lblListHeader)
                        lblListHeader.setTypeface(null, Typeface.BOLD)
                        lblListHeader.text = headerTitle
                    }

    override fun getChildView(groupPosition: Int,
                              childPosition: Int,
                              isLastChild: Boolean,
                              convertView: View?,
                              parent: ViewGroup): View =
            convertView
                    ?.let { inflater.inflate(R.layout.list_item, null) }!!
                    .apply {
                        val childText = getChild(groupPosition, childPosition) as String
                        val txtListChild = findViewById<TextView>(R.id.lblListItem)
                        txtListChild.text = childText
                    }

    override fun getChildrenCount(groupPosition: Int) = children[headers[groupPosition]]!!.size

    override fun getGroup(groupPosition: Int) = headers[groupPosition]

    override fun getGroupCount() = headers.size

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun hasStableIds() = false

    override fun isChildSelectable(groupPosition: Int,
                                   childPosition: Int) = false
}
