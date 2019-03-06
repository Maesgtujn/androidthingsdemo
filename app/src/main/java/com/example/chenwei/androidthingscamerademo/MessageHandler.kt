package com.example.chenwei.androidthingscamerademo

import android.app.ProgressDialog
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_camera_preview.*

import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*




/**
 * Created by williamSha on 2018/11/22.
 */
internal class MessageHandler(activity: CameraPreviewActivity) : Handler() {
    private var mActivity: WeakReference<CameraPreviewActivity> = WeakReference(activity)
    private var unknownCount: Int = 0

    private var weight: Double = EMPTY_WEIGHT
    private var employeeId: String = EMPTY

    private val theActivity: CameraPreviewActivity?
    private var pd: ProgressDialog

    init {
        theActivity = mActivity.get()
        pd = ProgressDialog(theActivity)

    }

    /**
     * When both [weight] and [employeeId] is not null, post the Weight, the Employee ID and
     * Timestamp to the cloud server. Then this two variables reset to empty value, and set
     * detector type as idle state.
     */
    private fun trySaveWeightAndId() {
        if (weight != EMPTY_WEIGHT && employeeId != EMPTY) {
            idleThread()
            theActivity!!.postWeightTest(weight, employeeId)
            Toast.makeText(theActivity, employeeId + "体重：" + weight, Toast.LENGTH_LONG).show()

            weight = EMPTY_WEIGHT
            employeeId = EMPTY


        }
    }

    private fun idleThread() {
        theActivity!!.draw.clear()
        theActivity.captureThread.detectorType = R.id.what_idle

    }

    override fun handleMessage(msg: Message?) {
        val state = msg!!.arg1
        val count = msg.arg2
        when (msg.what) {
            R.id.what_postWeight -> {
                Toast.makeText(theActivity, "post:" + (msg.obj as String), Toast.LENGTH_SHORT).show()

            }

            R.id.what_got_uname -> {
                theActivity!!.tv_person.text = msg.obj as String

                theActivity.delayClearText()

            }
            R.id.what_weight -> {
                val tempWeight: Double = msg.obj as Double
                when (state) {

                    R.id.state_start -> {
                        Log.d(TAG, "what_weight: start")

                        theActivity!!.captureThread.detectorType = R.id.what_facenet_identify

                    }
                    R.id.state_succ -> {
                        Log.d(TAG, "what_weight: success")

                        weight = tempWeight
                        trySaveWeightAndId()

                        theActivity!!.tv_weight.text = "= $tempWeight"
//                        theActivity!!.tv_weight.visibility = View.VISIBLE

                    }

                    R.id.state_progress -> {
                        Log.d(TAG, "what_weight: progress")
                        theActivity!!.tv_weight.text = "~ $tempWeight"
//                        theActivity!!.tv_weight.visibility = View.VISIBLE
                    }
                    R.id.state_leave -> {
                        Log.d(TAG, "what_weight: leave")
                        theActivity!!.tv_hint.setText(R.string.hint)
                        theActivity.tv_weight.text = EMPTY
                        weight = EMPTY_WEIGHT

                        idleThread()


                    }
                    R.id.state_leave_without_stable_value -> {
                        Log.d(TAG, "what_weight: leave without stable value")
                        theActivity!!.tv_hint.setText(R.string.hint)
                        theActivity.tv_weight.text = EMPTY
                        weight = EMPTY_WEIGHT

                        idleThread()

                    }
                }
            }
            R.id.what_bt_con -> {
                when (state) {

                    R.id.state_start -> {

                        theActivity!!.tv_bt.text = theActivity.getText(R.string.connecting)
                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_bt.text = theActivity.getText(R.string.connected)

                    }
                    R.id.state_fail -> {
                        theActivity!!.tv_bt.text = theActivity.getText(R.string.connect_fail)
                    }

                    R.id.state_progress -> {
                        //theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.connect_fail))

                    }
                    R.id.state_discon -> {
                        theActivity!!.tv_bt.text = theActivity.getText(R.string.disconnected)
                    }
                    R.id.state_funcfail -> {
                        theActivity!!.tv_bt.text = theActivity.getText(R.string.con_func_fail)
                    }
                }
            }
            R.id.what_ws_con -> {
                when (state) {

                    R.id.state_start -> {

                        theActivity!!.tv_ws.text = theActivity.getText(R.string.connecting)
                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_ws.text = theActivity.getText(R.string.connected)

                    }
                    R.id.state_fail -> {
                        theActivity!!.tv_ws.text = theActivity.getText(R.string.connect_fail)
                    }

                    R.id.state_progress -> {
                    }
                    R.id.state_discon -> {
                        theActivity!!.tv_ws.text = theActivity.getText(R.string.disconnected)
                    }
                }
            }
            R.id.what_qrcode -> {
                when (state) {

                    R.id.state_start -> {
                        //画扫扫描框

                        theActivity!!.draw.drawRectQR()

                        theActivity.tv_hint.setText(R.string.qrcode_start)
                        theActivity.tv_msg.visibility = View.INVISIBLE
                        theActivity.tv_person.visibility = View.INVISIBLE
//                        theActivity.tv_mode.text = R.string.qrcode_string.toString()

                    }
                    R.id.state_succ -> {
                        //@todo 工号翻译成人名

                        theActivity!!.tv_hint.setText(R.string.qrcode_succ)
                        theActivity.tv_msg.text = msg.obj as String
//                        theActivity.reqName(msg.obj as String)

                        theActivity.tv_msg.visibility = View.VISIBLE
                        theActivity.captureThread.detectorType = R.id.what_facenet_regadd

                    }
                    R.id.state_fail -> {
                        theActivity!!.tv_hint.setText(R.string.qrcode_fail)
                        theActivity.captureThread.detectorType = R.id.what_facenet_identify
                        theActivity.tv_msg.visibility = View.INVISIBLE

                    }

                    R.id.state_progress -> {
                        theActivity!!.tv_msg.text = theActivity.getString(R.string.qrcode_progress) + count + "/25"
                        theActivity.tv_msg.visibility = View.VISIBLE
                    }

                }

                //detectorType = R.id.what_facenet_identify

            }
            R.id.what_mtcnn -> {
                if (state == R.id.state_succ) {
                    val boxes: Vector<Box> = msg.obj as Vector<Box>
                    theActivity!!.draw.draw(boxes)
                    if (boxes.size == 0) {
                        unknownCount = 0
                        theActivity.tv_hint.text = "近一些..."


                    }
                }

            }
            R.id.what_facenet_identify -> {
                try {
                    if (theActivity!!.captureThread.detectorType == R.id.what_facenet_identify)
                        when (state) {
                            R.id.state_start -> {
                                //unknownCount = 0
                                theActivity.tv_hint.setText(R.string.facenet_identify_start)
//                                theActivity.tv_mode.setText(R.string.mode_face_idtf)
                                employeeId = EMPTY

                                //Toast.makeText(this@CameraPreviewActivity, R.string.facenet_identify_start, Toast.LENGTH_SHORT).show()


                            }
                            R.id.state_succ -> {
                                //@todo 工号翻译成人名
                                theActivity.tv_hint.setText(R.string.facenet_identify_succ)
                                val jsonPersons: JSONArray = msg.obj as JSONArray

                                for (i in 1..jsonPersons.length()) {
                                    val jsonPerson: JSONObject = jsonPersons.get(i - 1) as JSONObject
                                    val name = jsonPerson.getString("name")
                                    if (name == UNKNOWN_NAME) {
                                        unknownCount++
                                    } else {
                                        unknownCount = 0
                                        employeeId = name
                                        theActivity.reqName(name, 100 * jsonPerson.getDouble("prob"),jsonPerson.getString("emotion"))
                                        trySaveWeightAndId()

                                    }

//                                    text += String.format("%n %s  @ %.2f %s", name, 100 * jsonPerson.getDouble("prob"), jsonPerson.getString("emotion"))
                                }
//                                  theActivity.tv_person.text = text
//                                theActivity.tv_person.text = text
//                                theActivity.tv_person.visibility = View.VISIBLE
                                if (unknownCount > 0) {
                                    theActivity.tv_msg.text = "Unknown:$unknownCount"
                                    theActivity.tv_msg.visibility = View.VISIBLE
                                } else
                                    theActivity.tv_msg.visibility = View.INVISIBLE
                                if (unknownCount > MAX_UNKNOWN_COUNT) {
                                    unknownCount = 0
                                    theActivity.captureThread.detectorType = R.id.what_qrcode
                                }
                            }

                            R.id.state_fail -> {
                                theActivity.tv_hint.setText(R.string.facenet_identify_fail)
                            }
                            R.id.state_progress -> {
                                theActivity.tv_msg.visibility = View.INVISIBLE
                            }
                        }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.what_facenet_regadd -> {
                when (state) {
                    R.id.state_start -> {
                        theActivity!!.tv_hint.setText(R.string.facenet_add_start)
//                        theActivity.tv_mode.text = "facenet_regadd"
                        theActivity.tv_msg.visibility = View.INVISIBLE
                        theActivity.tv_person.visibility = View.INVISIBLE

                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_hint.setText(R.string.facenet_add_succ)

                    }

                    R.id.state_fail -> {
                    }
                    R.id.state_progress -> {
                        theActivity!!.tv_msg.text = "已采集图像：$count"
                        theActivity!!.tv_msg.visibility = View.VISIBLE
                    }
                }

            }
            R.id.what_facenet_validate -> {
                when (state) {

                    R.id.state_start -> {
                        theActivity!!.captureThread.detectorType = R.id.what_idle
//                        theActivity.tv_mode.text = "facenet_validate"
                        theActivity.tv_msg.visibility = View.INVISIBLE
                        theActivity.tv_person.visibility = View.INVISIBLE
                        theActivity.pd = ProgressDialog.show(theActivity, "facenet_validate", theActivity.getString(R.string.facenet_validate_start))
                        theActivity.tv_hint.setText(R.string.facenet_validate_start)
                    }
                    R.id.state_succ -> {
                        theActivity!!.pd.dismiss()
                        theActivity.tv_hint.setText(R.string.facenet_validate_succ)
                        theActivity.captureThread.detectorType = R.id.what_facenet_identify
                        Toast.makeText(theActivity, "注册成功", Toast.LENGTH_LONG).show()

                    }
                    R.id.state_fail -> {
                        pd.dismiss()
                        theActivity!!.tv_hint.setText(R.string.facenet_validate_fail)
                        theActivity.captureThread.detectorType = R.id.what_facenet_identify
                        Toast.makeText(theActivity, "注册失败", Toast.LENGTH_LONG).show()

                    }
                    R.id.state_progress -> {
                        theActivity!!.tv_hint.setText(R.string.facenet_validate_progress)
                    }

                }
            }
            R.id.what_clear_text -> {
                theActivity!!.tv_person.text = EMPTY
                theActivity.tv_hint.setText(R.string.hint)
            }


            else -> {

            }
        }
    }

    companion object {
        private val TAG = MessageHandler::class.java.simpleName

        private const val MAX_UNKNOWN_COUNT = 10
        private const val UNKNOWN_NAME = "Unknown"
        private const val EMPTY_WEIGHT = 0.0
        private const val EMPTY = ""


    }
}


