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
internal class CamHandler(activity: CameraPreviewActivity) : Handler() {
    var mActivity: WeakReference<CameraPreviewActivity>
    private val UNKOWN_NAME = "Unknown"
    private var unknowCount: Int = 0
    private val UNKOWN_COUNT_MAX = 10

    var myWeight: Double = 0.0
    var myAccount: String = ""

    var theActivity: CameraPreviewActivity?
    var pd: ProgressDialog

    init {
        mActivity = WeakReference<CameraPreviewActivity>(activity)
        theActivity = mActivity.get()

        pd = ProgressDialog(theActivity)

    }

    fun trySaveWeightAndAccount() {
        if (myWeight != 0.0 && myAccount != "") {
            Toast.makeText(theActivity, myAccount + "体重：" + myWeight, Toast.LENGTH_LONG).show()
            myWeight = 0.0
            myAccount = ""
        }
    }

    fun setThreadIdle() {
        theActivity!!.mCameraThread2.setdetectorType(R.id.what_idle)
        theActivity!!.tv_mode.setText(R.string.mode_idle)
        theActivity!!.tv_hint.setText(R.string.hint)
        theActivity!!.mDraw.clear()
    }

    override fun handleMessage(msg: Message?) {
        val state = msg!!.arg1
        val count = msg.arg2
        when (msg.what) {
            R.id.what_weight -> {
                var tmpWeight: Double = msg.obj as Double
                when (state) {

                    R.id.state_start -> {
                        theActivity!!.tv_weight.setText(">>>")
                        theActivity!!.mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_weight.setText("= " + tmpWeight)
                        myWeight = tmpWeight
                        trySaveWeightAndAccount()

                    }

                    R.id.state_progress -> {
                        theActivity!!.tv_weight.setText("~ " + tmpWeight)

                    }
                    R.id.state_leave -> {
                        theActivity!!.tv_weight.setText("...")
                        myWeight = 0.0
                        setThreadIdle()

                    }
                    R.id.state_leave_without_stable_value -> {
                        theActivity!!.tv_weight.setText("...")
                        myWeight = 0.0
                        setThreadIdle()
                    }
                }
            }
            R.id.what_bt_con -> {
                when (state) {

                    R.id.state_start -> {

                        theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.connecting))
                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.connected))

                    }
                    R.id.state_fail -> {
                        theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.connect_fail))
                    }

                    R.id.state_progress -> {
                        //theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.connect_fail))

                    }
                    R.id.state_discon -> {
                        theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.disconnected))
                    }
                    R.id.state_funcfail -> {
                        theActivity!!.tv_bt.setText(theActivity!!.getText(R.string.con_func_fail))
                    }
                }
            }
            R.id.what_ws_con -> {
                when (state) {

                    R.id.state_start -> {

                        theActivity!!.tv_ws.setText(theActivity!!.getText(R.string.connecting))
                    }
                    R.id.state_succ -> {
                        theActivity!!.tv_ws.setText(theActivity!!.getText(R.string.connected))

                    }
                    R.id.state_fail -> {
                        theActivity!!.tv_ws.setText(theActivity!!.getText(R.string.connect_fail))
                    }

                    R.id.state_progress -> {
                    }
                    R.id.state_discon -> {
                        theActivity!!.tv_ws.setText(theActivity!!.getText(R.string.disconnected))
                    }
                }
            }
            R.id.what_qrcode -> {
                when (state) {

                    R.id.state_start -> {
                        //画扫扫描框

                        theActivity!!.mDraw.drawRectQR()

                        theActivity!!.mTvHint.setText(R.string.qrcode_start)
                        theActivity!!.mTvMsg.setText("")
                        theActivity!!.mTvPerson.setText("")
                        theActivity!!.mTvMode.setText("qrcode")

                    }
                    R.id.state_succ -> {
                        theActivity!!.mTvHint.setText(R.string.qrcode_succ)
                        theActivity!!.mTvMsg.text = msg.obj as String
                        theActivity!!.mCameraThread2.setdetectorType(R.id.what_facenet_regadd)

                    }
                    R.id.state_fail -> {
                        theActivity!!.mTvHint.setText(R.string.qrcode_fail)
                        theActivity!!.mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                        theActivity!!.mTvMsg.setText("")

                    }

                    R.id.state_progress -> theActivity!!.mTvMsg.setText(theActivity!!.getString(R.string.qrcode_progress) + count + "/25")

                }

                //detectorType = R.id.what_facenet_identify

            }
            R.id.what_mtcnn -> {
                if (state == R.id.state_succ) {
                    val boxes: Vector<Box> = msg.obj as Vector<Box>
                    theActivity!!.mDraw.draw(boxes)
                    if (boxes.size == 0) {
                        unknowCount = 0
                        theActivity!!.mTvMsg.setText("...")
                        theActivity!!.mTvPerson.setText("...")
                        theActivity!!.mTvHint.setText("近一些...")


                    }
                }

            }
            R.id.what_facenet_identify -> {
                try {
                    if (theActivity!!.mCameraThread2.getDetectorType() == R.id.what_facenet_identify)
                        when (state) {
                            R.id.state_start -> {
                                //unknowCount = 0
                                theActivity!!.mTvHint.setText(R.string.facenet_identify_start)
                                theActivity!!.mTvMode.setText(R.string.mode_face_idtf)
                                myAccount = ""

                                //Toast.makeText(this@CameraPreviewActivity, R.string.facenet_identify_start, Toast.LENGTH_SHORT).show()


                            }
                            R.id.state_succ -> {
                                theActivity!!.mTvHint.setText(R.string.facenet_identify_succ)
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

                                    text = text + String.format("%n %s  @ %.2f %s", name, 100 * jsonPerson.getDouble("prob"), jsonPerson.getString("emotion"))
                                }

                                theActivity!!.mTvPerson.text = text
                                if (unknowCount > 0)
                                    theActivity!!.mTvMsg.setText("Unknow:" + unknowCount)
                                else
                                    theActivity!!.mTvMsg.setText("")
                                if (unknowCount > UNKOWN_COUNT_MAX) {
                                    unknowCount = 0
                                    theActivity!!.mCameraThread2.setdetectorType(R.id.what_qrcode)
                                }
                            }

                            R.id.state_fail -> {
                                theActivity!!.mTvHint.setText(R.string.facenet_identify_fail)
                            }
                            R.id.state_progress -> {
                                theActivity!!.mTvMsg.text = "..."
                            }
                        }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.what_facenet_regadd -> {
                when (state) {
                    R.id.state_start -> {
                        theActivity!!.mTvHint.setText(R.string.facenet_add_start)
                        theActivity!!.mTvMode.setText("facenet_regadd")
                        theActivity!!.mTvMsg.setText("")
                        theActivity!!.mTvPerson.setText("")

                    }
                    R.id.state_succ -> {
                        theActivity!!.mTvHint.setText(R.string.facenet_add_succ)

                    }

                    R.id.state_fail -> {
                    }
                    R.id.state_progress -> theActivity!!.mTvMsg.setText("已采集图像：" + count)
                }

            }
            R.id.what_facenet_validate -> {
                when (state) {

                    R.id.state_start -> {
                        theActivity!!.mCameraThread2.setdetectorType(R.id.what_idle)
                        theActivity!!.mTvMode.setText("facenet_validate")
                        theActivity!!.mTvMsg.setText("")
                        theActivity!!.mTvPerson.setText("")
                        theActivity!!.pd = ProgressDialog.show(theActivity, "facenet_validate", theActivity!!.getString(R.string.facenet_validate_start));
                        theActivity!!.mTvHint.setText(R.string.facenet_validate_start)
                    }
                    R.id.state_succ -> {
                        theActivity!!.pd.dismiss()
                        theActivity!!.mTvHint.setText(R.string.facenet_validate_succ)
                        theActivity!!.mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                        Toast.makeText(theActivity, "注册成功", Toast.LENGTH_LONG).show()

                    }
                    R.id.state_fail -> {
                        pd.dismiss()
                        theActivity!!.mTvHint.setText(R.string.facenet_validate_fail)
                        theActivity!!.mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                        Toast.makeText(theActivity, "注册失败", Toast.LENGTH_LONG).show()

                    }
                    R.id.state_progress -> {
                        theActivity!!.mTvHint.setText(R.string.facenet_validate_progress)
                    }

                }
            }


            else -> {

            }
        //}
        }
    }
};
