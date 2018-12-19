package com.example.chenwei.androidthingscamerademo

import android.app.ProgressDialog
import android.os.Handler
import android.os.Message
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_camera_preview.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by williamsha on 2018/11/22.
 */
internal class MessageHandler (activity: CameraPreviewActivity) : Handler() {
    private var mActivity: WeakReference<CameraPreviewActivity> = WeakReference(activity)
    private val UNKOWN_NAME = "Unknown"
    private var unknowCount: Int = 0
    private val UNKOWN_COUNT_MAX = 10

    private var myWeight: Double = 0.0
    private var myAccount: String = ""

    private val theActivity: CameraPreviewActivity?
    private var pd: ProgressDialog

    init {
        theActivity = mActivity.get()

        pd = ProgressDialog(theActivity)

    }

    private fun trySaveWeightAndAccount() {
        if (myWeight != 0.0 && myAccount != "") {
            Toast.makeText(theActivity, myAccount + "体重：" + myWeight, Toast.LENGTH_LONG).show()
            myWeight = 0.0
            myAccount = ""
        }
    }

    private fun setThreadIdle() {
        theActivity!!.mCaptureThread.detectorType = R.id.what_idle
        theActivity.tv_mode.setText(R.string.mode_idle)
        theActivity.tv_hint.setText(R.string.hint)
        theActivity.draw.clear()
    }

    override fun handleMessage(msg: Message?) {
        val state = msg!!.arg1
        val count = msg.arg2
        when (msg.what) {
            R.id.what_weight -> {
                val tempWeight: Double = msg.obj as Double
                when (state) {

                    R.id.state_start -> {
                        theActivity!!.tv_weight.text = ">>>"
                        theActivity.mCaptureThread.detectorType = R.id.what_facenet_identify
                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_weight.text = "= $tempWeight"
                        myWeight = tempWeight
                        trySaveWeightAndAccount()

                    }

                    R.id.state_progress -> {
                        theActivity!!.tv_weight.text = "~ $tempWeight"

                    }
                    R.id.state_leave -> {
                        theActivity!!.tv_weight.text = "..."
                        myWeight = 0.0
                        setThreadIdle()

                    }
                    R.id.state_leave_without_stable_value -> {
                        theActivity!!.tv_weight.text = "..."
                        myWeight = 0.0
                        setThreadIdle()
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
                        theActivity.tv_msg.text = ""
                        theActivity.tv_person.text = ""
                        theActivity.tv_mode.text = R.string.qrcode_string.toString()

                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_hint.setText(R.string.qrcode_succ)
                        theActivity.tv_msg.text = msg.obj as String
                        theActivity.mCaptureThread.detectorType = R.id.what_facenet_regadd

                    }
                    R.id.state_fail -> {
                        theActivity!!.tv_hint.setText(R.string.qrcode_fail)
                        theActivity.mCaptureThread.detectorType = R.id.what_facenet_identify
                        theActivity.tv_msg.text = ""

                    }

                    R.id.state_progress -> theActivity!!.tv_msg.text = theActivity.getString(R.string.qrcode_progress) + count + "/25"

                }

                //detectorType = R.id.what_facenet_identify

            }
            R.id.what_mtcnn -> {
                if (state == R.id.state_succ) {
                    val boxes: Vector<Box> = msg.obj as Vector<Box>
                    theActivity!!.draw.draw(boxes)
                    if (boxes.size == 0) {
                        unknowCount = 0
                        theActivity.tv_msg.text = "..."
                        theActivity.tv_person.text = "..."
                        theActivity.tv_hint.text = "近一些..."


                    }
                }

            }
            R.id.what_facenet_identify -> {
                try {
                    if (theActivity!!.mCaptureThread.detectorType == R.id.what_facenet_identify)
                        when (state) {
                            R.id.state_start -> {
                                //unknowCount = 0
                                theActivity.tv_hint.setText(R.string.facenet_identify_start)
                                theActivity.tv_mode.setText(R.string.mode_face_idtf)
                                myAccount = ""

                                //Toast.makeText(this@CameraPreviewActivity, R.string.facenet_identify_start, Toast.LENGTH_SHORT).show()


                            }
                            R.id.state_succ -> {
                                theActivity.tv_hint.setText(R.string.facenet_identify_succ)
                                val jsonPersons: JSONArray = msg.obj as JSONArray
                                var text = ""
                                for (i in 1..jsonPersons.length()) {
                                    val jsonPerson: JSONObject = jsonPersons.get(i - 1) as JSONObject
                                    val name = jsonPerson.getString("name")
                                    if (name == UNKOWN_NAME) {
                                        unknowCount++
                                    } else {
                                        unknowCount = 0
                                        myAccount = name
                                        trySaveWeightAndAccount()
                                    }

                                    text += String.format("%n %s  @ %.2f %s", name, 100 * jsonPerson.getDouble("prob"), jsonPerson.getString("emotion"))
                                }

                                theActivity.tv_person.text = text
                                if (unknowCount > 0)
                                    theActivity.tv_msg.text = "Unknow:" + unknowCount
                                else
                                    theActivity.tv_msg.text = ""
                                if (unknowCount > UNKOWN_COUNT_MAX) {
                                    unknowCount = 0
                                    theActivity.mCaptureThread.detectorType = R.id.what_qrcode
                                }
                            }

                            R.id.state_fail -> {
                                theActivity.tv_hint.setText(R.string.facenet_identify_fail)
                            }
                            R.id.state_progress -> {
                                theActivity.tv_msg.text = "..."
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
                        theActivity.tv_mode.text = "facenet_regadd"
                        theActivity.tv_msg.text = ""
                        theActivity.tv_person.text = ""

                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_hint.setText(R.string.facenet_add_succ)

                    }

                    R.id.state_fail -> {
                    }
                    R.id.state_progress -> theActivity!!.tv_msg.text = "已采集图像：" + count
                }

            }
            R.id.what_facenet_validate -> {
                when (state) {

                    R.id.state_start -> {
                        theActivity!!.mCaptureThread.detectorType = R.id.what_idle
                        theActivity.tv_mode.text = "facenet_validate"
                        theActivity.tv_msg.text = ""
                        theActivity.tv_person.text = ""
                        theActivity.pd = ProgressDialog.show(theActivity, "facenet_validate", theActivity.getString(R.string.facenet_validate_start))
                        theActivity.tv_hint.setText(R.string.facenet_validate_start)
                    }
                    R.id.state_succ -> {
                        theActivity!!.pd.dismiss()
                        theActivity.tv_hint.setText(R.string.facenet_validate_succ)
                        theActivity.mCaptureThread.detectorType = R.id.what_facenet_identify
                        Toast.makeText(theActivity, "注册成功", Toast.LENGTH_LONG).show()

                    }
                    R.id.state_fail -> {
                        pd.dismiss()
                        theActivity!!.tv_hint.setText(R.string.facenet_validate_fail)
                        theActivity.mCaptureThread.detectorType = R.id.what_facenet_identify
                        Toast.makeText(theActivity, "注册失败", Toast.LENGTH_LONG).show()

                    }
                    R.id.state_progress -> {
                        theActivity!!.tv_hint.setText(R.string.facenet_validate_progress)
                    }

                }
            }


            else -> {

            }
        //}
        }
    }
}
