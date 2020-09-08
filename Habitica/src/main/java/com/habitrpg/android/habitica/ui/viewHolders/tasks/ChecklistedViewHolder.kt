package com.habitrpg.android.habitica.ui.viewHolders.tasks

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.extensions.getThemeColor
import com.habitrpg.android.habitica.extensions.isUsingNightModeResources
import com.habitrpg.android.habitica.helpers.RxErrorHandler
import com.habitrpg.android.habitica.models.responses.TaskDirection
import com.habitrpg.android.habitica.models.tasks.ChecklistItem
import com.habitrpg.android.habitica.models.tasks.Task
import com.habitrpg.android.habitica.ui.helpers.MarkdownParser
import com.habitrpg.android.habitica.ui.views.HabiticaEmojiTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

abstract class ChecklistedViewHolder(itemView: View, scoreTaskFunc: ((Task, TaskDirection) -> Unit), var scoreChecklistItemFunc: ((Task, ChecklistItem) -> Unit), openTaskFunc: ((Task) -> Unit), brokenTaskFunc: ((Task) -> Unit)) : BaseTaskViewHolder(itemView, scoreTaskFunc, openTaskFunc, brokenTaskFunc), CompoundButton.OnCheckedChangeListener {

    private val checkboxHolder: ViewGroup = itemView.findViewById(R.id.checkBoxHolder)
    internal val checkbox: CheckBox = itemView.findViewById(R.id.checkBox)
    internal val checklistView: LinearLayout = itemView.findViewById(R.id.checklistView)
    internal val checklistIndicatorWrapper: ViewGroup = itemView.findViewById(R.id.checklistIndicatorWrapper)
    private val checklistCompletedTextView: TextView = itemView.findViewById(R.id.checkListCompletedTextView)
    private val checklistAllTextView: TextView = itemView.findViewById(R.id.checkListAllTextView)
    private val checklistDivider: View = itemView.findViewById(R.id.checklistDivider)

    init {
        checklistIndicatorWrapper.isClickable = true
        checklistIndicatorWrapper.setOnClickListener { onChecklistIndicatorClicked() }
        checkbox.setOnCheckedChangeListener(this)
        expandCheckboxTouchArea(checkboxHolder, checkbox)
    }

    override fun bind(newTask: Task, position: Int, displayMode: String) {
        var completed = newTask.completed
        if (newTask.isPendingApproval) {
            completed = false
        }
        this.checkbox.isChecked = completed
        if (this.shouldDisplayAsActive(newTask) && !newTask.isPendingApproval) {
            this.checkboxHolder.setBackgroundResource(newTask.lightTaskColor)
        } else {
            if (newTask.completed) {
                this.checkboxHolder.setBackgroundColor(context.getThemeColor(R.attr.colorWindowBackground))
            } else {
                this.checkboxHolder.setBackgroundColor(this.taskGray)
            }
        }
        this.checklistCompletedTextView.text = newTask.completedChecklistCount.toString()
        this.checklistAllTextView.text = newTask.checklist?.size.toString()

        this.checklistView.removeAllViews()
        this.updateChecklistDisplay()

        this.checklistIndicatorWrapper.visibility = if (newTask.checklist?.size == 0) View.GONE else View.VISIBLE
        super.bind(newTask, position, displayMode)
    }

    abstract fun shouldDisplayAsActive(newTask: Task): Boolean

    private fun updateChecklistDisplay() {
        //This needs to be a LinearLayout, as ListViews can not be inside other ListViews.
        if (this.shouldDisplayExpandedChecklist()) {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
            if (this.task?.checklist?.isValid == true) {
                checklistView.removeAllViews()
                for (item in this.task?.checklist ?: emptyList<ChecklistItem>()) {
                    val itemView = layoutInflater?.inflate(R.layout.checklist_item_row, this.checklistView, false)
                    val checkboxBackground = itemView?.findViewById<FrameLayout>(R.id.checkBoxBackground)
                    if (task?.type == Task.TYPE_TODO) {
                        checkboxBackground?.setBackgroundResource(R.drawable.round_checklist_unchecked)
                    }
                    val textView = itemView?.findViewById<HabiticaEmojiTextView>(R.id.checkedTextView)
                    // Populate the data into the template view using the data object
                    textView?.text = item.text
                    if (item.text != null) {
                        Observable.just(item.text)
                                .map { MarkdownParser.parseMarkdown(it) }
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ textView?.text = it }, RxErrorHandler.handleEmptyError())
                    }
                    val checkmark = itemView?.findViewById<ImageView>(R.id.checkmark)
                    checkmark?.drawable?.setTintMode(PorterDuff.Mode.SRC_ATOP)
                    checkmark?.visibility = if (item.completed) View.VISIBLE else View.GONE
                    val checkboxHolder = itemView?.findViewById<View>(R.id.checkBoxHolder) as? ViewGroup
                    checkboxHolder?.setOnClickListener { _ ->
                        task?.let { scoreChecklistItemFunc(it, item) }
                    }
                    val color = ContextCompat.getColor(context, if (task?.completed == true || (task?.type == Task.TYPE_DAILY && task?.isDue == false)) {
                        checkmark?.drawable?.setTint(ContextCompat.getColor(context, R.color.text_dimmed))
                        R.color.offset_background
                    } else {
                        checkmark?.drawable?.setTint(ContextCompat.getColor(context, task?.darkTaskColor ?: R.color.text_dimmed))
                        task?.extraLightTaskColor ?: R.color.offset_background
                    })
                    color.let { checkboxHolder?.setBackgroundColor(it) }
                    this.checklistView.addView(itemView)
                }
            }
            this.checklistView.visibility = View.VISIBLE
        } else {
            this.checklistView.removeAllViewsInLayout()
            this.checklistView.visibility = View.GONE
        }
    }

    protected fun setChecklistIndicatorBackgroundActive(isActive: Boolean) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.checklist_indicator_background)
        if (isActive) {
            if (context.isUsingNightModeResources()) {
                drawable?.setTint(ContextCompat.getColor(context, R.color.gray_100))
            } else {
                drawable?.setTint(ContextCompat.getColor(context, R.color.gray_200))
            }
            val textColor = if (context.isUsingNightModeResources()) {
                ContextCompat.getColor(context, R.color.gray_10)
            } else {
                ContextCompat.getColor(context, R.color.gray_500)
            }
            checklistCompletedTextView.setTextColor(textColor)
            checklistAllTextView.setTextColor(textColor)
            checklistDivider.setBackgroundColor(textColor)
        } else {
            drawable?.setTint(ContextCompat.getColor(context, R.color.offset_background))
            val textColor = ContextCompat.getColor(context, R.color.text_ternary)
            checklistCompletedTextView.setTextColor(textColor)
            checklistAllTextView.setTextColor(textColor)
            checklistDivider.setBackgroundColor(textColor)
        }
        drawable?.setTintMode(PorterDuff.Mode.MULTIPLY)
        checklistIndicatorWrapper.background = drawable
    }

    private fun onChecklistIndicatorClicked() {
        expandedChecklistRow = if (this.shouldDisplayExpandedChecklist()) null else adapterPosition
        if (this.shouldDisplayExpandedChecklist()) {
            val recyclerView = this.checklistView.parent.parent as? RecyclerView
            val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(this.adapterPosition, 15)
        }
        updateChecklistDisplay()

    }

    private fun shouldDisplayExpandedChecklist(): Boolean {
        return expandedChecklistRow != null && adapterPosition == expandedChecklistRow
    }

    private fun expandCheckboxTouchArea(expandedView: View?, checkboxView: View?) {
        expandedView?.post {
            val rect = Rect()
            expandedView.getHitRect(rect)
            expandedView.touchDelegate = TouchDelegate(rect, checkboxView)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView == checkbox) {
            if (task?.isValid != true) {
                return
            }
            if (isChecked != task?.completed) {
                task?.let { scoreTaskFunc(it, if (task?.completed == false) TaskDirection.UP else TaskDirection.DOWN) }
            }
        }
    }

    override fun setDisabled(openTaskDisabled: Boolean, taskActionsDisabled: Boolean) {
        super.setDisabled(openTaskDisabled, taskActionsDisabled)

        this.checkbox.isEnabled = !taskActionsDisabled
    }

    companion object {

        private var expandedChecklistRow: Int? = null
    }
}
