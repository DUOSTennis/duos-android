package com.example.duos.ui.main.dailyMatching

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.ImageLoader
import com.bumptech.glide.Glide
import com.example.duos.ApplicationClass
import com.example.duos.CustomDialog
import com.example.duos.R
import com.example.duos.data.entities.dailyMatching.DailyMatchingDetail
import com.example.duos.data.remote.dailyMatching.DailyMatchingWriteService
import com.example.duos.data.remote.dailyMatching.DailyWriteResult

import com.example.duos.databinding.ActivityDailyMatchingEditBinding
import com.example.duos.ui.BaseActivity
import com.example.duos.utils.GlideApp.with
import com.example.duos.utils.ViewModel
import com.example.duos.utils.getUserIdx
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import java.lang.Exception


class DailyMatchingEdit :
    BaseActivity<ActivityDailyMatchingEditBinding>(ActivityDailyMatchingEditBinding::inflate),
    DailyMatchingEditView {

    var setTime: Int = 0
    var start: Int = 0
    var end: Int = 0
    var boardIdx: Int = -1
    lateinit var matchDate: LocalDate
    lateinit var startTime: LocalDateTime
    lateinit var endTime: LocalDateTime
    lateinit var viewModel: ViewModel
    var contentBitmap: Bitmap? = null
    var isSelectedImg: Boolean = false
    lateinit var dailyMatchingInfo: DailyMatchingDetail
    lateinit var currentTime: LocalDateTime
    lateinit var dailyMatchingEditRVAdapter: DailyMatchingTimeSelectRVAdapter

    lateinit var now : LocalDate
    lateinit var tomorrow : LocalDate
    lateinit var dayAfterTomorrow : LocalDate
    lateinit var strNow : String
    lateinit var strTomorrow : String
    lateinit var strDayAfterTomorrow: String

    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val CAMERA_PERMISSION_REQUEST = 100
    lateinit var contentUri: Uri
    val multiplePermissionsCode1 = 200
    val multiplePermissionsCode2 = 300

    // ??????????????? ?????????1
    val requiredPermissions1 = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
    )

    // ??????????????? ?????????(??????)2
    val permission_list = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )

    override fun onResume() {
        super.onResume()
        viewModel.dailyMatchingDateCheck.value = true
        viewModel.dailyMatchingTimeCheck.value = true
        viewModel.dailyMatchingImgCount.value = 0

        viewModel.dailyMatchingEditTitle.value = dailyMatchingInfo.title
        viewModel.dailyMatchingEditPlace.value = dailyMatchingInfo.matchPlace
        viewModel.dailyMatchingEditContent.value = dailyMatchingInfo.content

        currentTime = LocalDateTime.now()

        binding.dailyMatchingEditTodayDateTv.text = strNow
        binding.dailyMatchingEditTomorrowDateTv.text = strTomorrow
        binding.dailyMatchingEditDayAfterTomorrowDateTv.text = strDayAfterTomorrow

        Log.d("dailyMatchinginfo",dailyMatchingInfo.toString())

        when (dailyMatchingInfo.urls.size) {
            1 -> {
                setImage01()
            }
            2 -> {
                setImage01()
                setImage02()
            }
            3 -> {
                setImage01()
                setImage02()
                setImage03()
            }
        }
        when (dailyMatchingInfo.stringForMatchDateGap) {
            "??????" -> {
                binding.dailyMatchingEditTodayTv.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.primary
                    )
                )
                binding.dailyMatchingEditTodayRadioBtn.isChecked = true
                setTime = currentTime.hour + 1
                dailyMatchingEditRVAdapter.updateStartTime(setTime)
                dailyMatchingEditRVAdapter.setTime(
                    dailyMatchingInfo.startTime.substring(0 until 2).toInt(),
                    dailyMatchingInfo.duration
                )
                matchDate = now
                start = dailyMatchingInfo.startTime.substring(0 until 2).toInt()
                end = dailyMatchingInfo.startTime.substring(0 until 2)
                    .toInt() + dailyMatchingInfo.duration

            }
            "??????" -> {
                binding.dailyMatchingEditTomorrowTv.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.primary
                    )
                )
                binding.dailyMatchingEditTomorrowRadioBtn.isChecked = true
                setTime = 0
                dailyMatchingEditRVAdapter.updateStartTime(setTime)
                dailyMatchingEditRVAdapter.setTime(
                    dailyMatchingInfo.startTime.substring(0 until 2).toInt(),
                    dailyMatchingInfo.duration
                )
                matchDate = tomorrow
                start = dailyMatchingInfo.startTime.substring(0 until 2).toInt()
                end = dailyMatchingInfo.startTime.substring(0 until 2)
                    .toInt() + dailyMatchingInfo.duration
            }
            "??????" -> {
                binding.dailyMatchingEditDayAfterTomorrowTv.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.primary
                    )
                )
                binding.dailyMatchingEditDayAfterTomorrowRadioBtn.isChecked = true
                setTime = 0
                dailyMatchingEditRVAdapter.updateStartTime(setTime)
                dailyMatchingEditRVAdapter.setTime(
                    dailyMatchingInfo.startTime.substring(0 until 2).toInt(),
                    dailyMatchingInfo.duration
                )
                matchDate = dayAfterTomorrow
                start = dailyMatchingInfo.startTime.substring(0 until 2).toInt()
                end = dailyMatchingInfo.startTime.substring(0 until 2)
                    .toInt() + dailyMatchingInfo.duration
            }
            else -> {}
        }
        binding.dailyMatchingEditSelectTimeTv.text =
            (start.toString() + ":00 - " +
                    end.toString() + ":00" + "(" + (dailyMatchingInfo.duration).toString() + "??????)")

    }

    override fun initAfterBinding() {

        // ????????? ???????????? & ???????????????
        boardIdx = intent.getIntExtra("boardIdx", -1)
        dailyMatchingInfo = intent.getSerializableExtra("dailyMatchingInfo") as DailyMatchingDetail

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ).get(ViewModel::class.java)

        binding.viewmodel = viewModel

        dailyMatchingEditRVAdapter = DailyMatchingTimeSelectRVAdapter(setTime)
        binding.dailyMatchingEditSelectTimeRecyclerviewRv.setHasFixedSize(false)
        binding.dailyMatchingEditSelectTimeRecyclerviewRv.itemAnimator = null
        binding.dailyMatchingEditSelectTimeRecyclerviewRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dailyMatchingEditSelectTimeRecyclerviewRv.adapter = dailyMatchingEditRVAdapter

        now = LocalDate.now()
        tomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS)
        dayAfterTomorrow = LocalDate.now().plus(2, ChronoUnit.DAYS)
        strNow = now.format(DateTimeFormatter.ofPattern("M??? d???(E)", Locale.KOREAN))
        strTomorrow = tomorrow.format(DateTimeFormatter.ofPattern("M??? d???(E)", Locale.KOREAN))
        strDayAfterTomorrow =
            dayAfterTomorrow.format(DateTimeFormatter.ofPattern("M??? d???(E)", Locale.KOREAN))


        setEditBtnEnable()

        this.viewModel.dailyMatchingEditTitle.observe(this, {
            Log.d("1", "??????")
            if (it!!.isNotEmpty() && this.viewModel.dailyMatchingEditPlace.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingEditContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ) {
                setEditBtnEnable()
            } else setEditBtnUnable()
        })
        this.viewModel.dailyMatchingEditPlace.observe(this, {
            Log.d("2", "??????")
            if (it!!.isNotEmpty() && this.viewModel.dailyMatchingEditTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingEditContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ) {
                setEditBtnEnable()
            } else setEditBtnUnable()
        })
        this.viewModel.dailyMatchingEditContent.observe(this, {
            Log.d("3", "??????")
            if (it!!.isNotEmpty() && this.viewModel.dailyMatchingEditTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingEditPlace.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ) {
                setEditBtnEnable()
            } else setEditBtnUnable()
        })
//        this.viewModel.dailyMatchingDateCheck.observe(this, {
//            Log.d("4", "??????")
//            if (it!! && this.viewModel.dailyMatchingEditTitle.value?.isNotEmpty() == true
//                && this.viewModel.dailyMatchingEditPlace.value?.isNotEmpty() == true &&
//                this.viewModel.dailyMatchingEditContent.value?.isNotEmpty() == true &&
//                this.viewModel.dailyMatchingTimeCheck.value == true
//            ) {
//                setEditBtnEnable()
//            } else setEditBtnUnable()
//        })
        this.viewModel.dailyMatchingTimeCheck.observe(this, {
            Log.d("5", "??????")
            if (it!! && this.viewModel.dailyMatchingEditTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingEditPlace.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingEditContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true
            ) {
                setEditBtnEnable()
            } else setEditBtnUnable()
        })




        dailyMatchingEditRVAdapter.setMyItemClickListener(object :
            DailyMatchingTimeSelectRVAdapter.MyItemClickListener {
            override fun onClickItem(startPosition: Int, endPosition: Int) {
                for (i in startPosition until endPosition + 1) {
                    val view =
                        binding.dailyMatchingEditSelectTimeRecyclerviewRv.findViewHolderForAdapterPosition(
                            i
                        )?.itemView
                    val btn = view?.findViewById<Button>(R.id.daily_matching_time_selector_btn)
                    btn?.isSelected = true
                    start = setTime + startPosition
                    end = setTime + endPosition + 1
                    binding.dailyMatchingEditSelectTimeTv.text =
                        ((setTime + startPosition).toString() + ":00 - " +
                                (setTime + endPosition + 1).toString() + ":00" + "(" + (endPosition - startPosition + 1).toString() + "??????)")
                    if (viewModel.dailyMatchingTimeCheck.value == false)
                        viewModel.dailyMatchingTimeCheck.value = true
                }
            }

            override fun onResetItem(startPosition: Int) {
                if (viewModel.dailyMatchingTimeCheck.value == false)
                    viewModel.dailyMatchingTimeCheck.value = true
                binding.dailyMatchingEditSelectTimeTv.text =
                    ((setTime + startPosition).toString() + ":00 - " +
                            (setTime + startPosition + 1).toString() + ":00" + "(" + "1??????)")
                for (i in 0 until dailyMatchingEditRVAdapter.itemCount) {
                    val view =
                        binding.dailyMatchingEditSelectTimeRecyclerviewRv.findViewHolderForAdapterPosition(
                            i
                        )?.itemView
                    val btn = view?.findViewById<Button>(R.id.daily_matching_time_selector_btn)
                    btn?.isSelected = false
                }
                start = setTime + startPosition
                end = setTime + startPosition + 1
            }
        })

        binding.dailyMatchingEditRadioGroupRg.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val radio: RadioButton = findViewById(checkedId)
            when (radio.tag) {
                "today" -> {
                    setTime = currentTime.hour + 1
                    dailyMatchingEditRVAdapter.updateStartTime(setTime)
                    binding.dailyMatchingEditTodayTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.primary
                        )
                    )
                    binding.dailyMatchingEditTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    binding.dailyMatchingEditDayAfterTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    matchDate = now
                    viewModel.dailyMatchingDate.value = "??????"
                }
                "tomorrow" -> {
                    setTime = 0
                    dailyMatchingEditRVAdapter.updateStartTime(setTime)

                    binding.dailyMatchingEditTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.primary
                        )
                    )
                    binding.dailyMatchingEditDayAfterTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    binding.dailyMatchingEditTodayTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    matchDate = tomorrow
                    viewModel.dailyMatchingDate.value = "??????"
                }
                "dayaftertomorrow" -> {
                    setTime = 0
                    dailyMatchingEditRVAdapter.updateStartTime(setTime)

                    binding.dailyMatchingEditDayAfterTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.primary
                        )
                    )
                    binding.dailyMatchingEditTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    binding.dailyMatchingEditTodayTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    matchDate = dayAfterTomorrow
                    viewModel.dailyMatchingDate.value = "??????"

                }
            }
        })

        viewModel.dailyMatchingImgCount.observe(this, {
            binding.dailyMatchingEditImageCountTv.text = it.toString()
        })

        binding.dailyMatchingEditBackArrowIv.setOnClickListener {
            showDialog(this)
        }

        binding.dailyMatchingEditBtn.setOnClickListener {
            Log.d("??????", "??????")
            startTime =
                LocalDateTime.of(matchDate.year, matchDate.month, matchDate.dayOfMonth, start, 0, 0)
            if (end == 24) {
                endTime =
                    LocalDateTime.of(
                        matchDate.plus(1, ChronoUnit.DAYS).year,
                        matchDate.plus(1, ChronoUnit.DAYS).month,
                        matchDate.plus(1, ChronoUnit.DAYS).dayOfMonth,
                        0,
                        0,
                        0
                    )
            } else {
                endTime =
                    LocalDateTime.of(
                        matchDate.year,
                        matchDate.month,
                        matchDate.dayOfMonth,
                        end,
                        0,
                        0
                    )
            }


            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = JSONObject()


            json.put("boardIdx", boardIdx)
            json.put("writerIdx", getUserIdx()!!)
            json.put("title", viewModel.dailyMatchingEditTitle.value.toString())
            json.put("content", viewModel.dailyMatchingEditContent.value.toString())
            json.put("matchDate", matchDate)
            json.put("matchPlace", viewModel.dailyMatchingEditPlace.value.toString())
            json.put("startTime", startTime)
            json.put("endTime", endTime)

            Log.d("boardIdx", boardIdx.toString())
            Log.d("writerIdx", getUserIdx()!!.toString())
            Log.d("title", viewModel.dailyMatchingEditTitle.value.toString())
            Log.d("content", viewModel.dailyMatchingEditContent.value.toString())
            Log.d("matchDate", matchDate.toString())
            Log.d("matchPlace", viewModel.dailyMatchingEditPlace.value.toString())
            Log.d("startTime", startTime.toString())
            Log.d("endTime", endTime.toString())

            val editInfo = json.toString().toRequestBody(JSON)

            Log.d("json", editInfo.toString())

            val bitmapMultipartBody: MutableList<MultipartBody.Part> = mutableListOf()
            if (viewModel.dailyMatchingImg01Bitmap.value != null) {
                Log.d("??????1", viewModel.dailyMatchingImg01Bitmap.value.toString())
                val bitmapRequestBody =
                    viewModel.dailyMatchingImg01Bitmap.value.let { BitmapRequestBody(it!!) }
                bitmapMultipartBody.add(
                    MultipartBody.Part.createFormData(
                        "mFiles",
                        "img01",
                        bitmapRequestBody
                    )
                )
                isSelectedImg = true
            }
            if (viewModel.dailyMatchingImg02Bitmap.value != null) {
                Log.d("??????", "??????2")
                val bitmapRequestBody =
                    viewModel.dailyMatchingImg02Bitmap.value.let { BitmapRequestBody(it!!) }
                bitmapMultipartBody.add(
                    MultipartBody.Part.createFormData(
                        "mFiles",
                        "img02",
                        bitmapRequestBody
                    )
                )
                isSelectedImg = true
            }
            if (viewModel.dailyMatchingImg03Bitmap.value != null) {
                Log.d("??????", "??????3")
                val bitmapRequestBody =
                    viewModel.dailyMatchingImg03Bitmap.value.let { BitmapRequestBody(it!!) }
                bitmapMultipartBody.add(
                    MultipartBody.Part.createFormData(
                        "mFiles",
                        "img03",
                        bitmapRequestBody
                    )
                )
                isSelectedImg = true
            }

            // ???????????? ???????????????
            if (isSelectedImg) {
                Log.d("??????", bitmapMultipartBody.toString())
                DailyMatchingWriteService.dailyMatchingEditWithImg(
                    this,
                    bitmapMultipartBody,
                    editInfo
                )
            }
            // ?????? ????????? ????????? ??????????????????
            else {
                DailyMatchingWriteService.dailyMatchingEditWithoutImg(this, editInfo)
            }
        }


        binding.dailyMatchingEditSelectImageLayoutCl.setOnClickListener {
            requestImg()
        }

        binding.dailyMatchingEditSelectErase01Iv.setOnClickListener {
            viewModel.dailyMatchingImg01.value = false
            viewModel.dailyMatchingImg01Bitmap.value = null
            viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.minus(1)
            binding.dailyMatchingEditSelectImageLayout01Iv.visibility = View.GONE
            isSelectedImg = false
        }

        binding.dailyMatchingEditSelectErase02Iv.setOnClickListener {
            viewModel.dailyMatchingImg02.value = false
            viewModel.dailyMatchingImg02Bitmap.value = null
            viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.minus(1)
            binding.dailyMatchingEditSelectImageLayout02Iv.visibility = View.GONE
        }

        binding.dailyMatchingEditSelectErase03Iv.setOnClickListener {
            viewModel.dailyMatchingImg03.value = false
            viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.minus(1)
            viewModel.dailyMatchingImg03Bitmap.value = null
            binding.dailyMatchingEditSelectImageLayout03Iv.visibility = View.GONE
        }
    }


    fun setEditBtnEnable() {
        binding.dailyMatchingEditBtn.isEnabled = true
        binding.dailyMatchingEditBtn.background =
            getDrawable(R.drawable.signup_next_btn_done_rectangular)
        binding.dailyMatchingEditBtn.setTextColor(getColor(R.color.white))
    }

    fun setEditBtnUnable() {
        binding.dailyMatchingEditBtn.isEnabled = false
        binding.dailyMatchingEditBtn.background =
            getDrawable(R.drawable.signup_next_btn_rectangular)
        binding.dailyMatchingEditBtn.setTextColor(getColor(R.color.dark_gray_B0))
    }

    fun showDialog(context: Context) {
        // ?????? ?????????????????? ??????????????? ?????? requireContext() ??? ?????? context??? ???????????? ???.
        val dialog = CustomDialog.Builder(context)
            .setCommentMessage(getString(R.string.daily_matching_edit_cancel))// Dialog????????? ???????????? "~~~ "
            .setRightButton(
                getString(R.string.daily_matching_write_delete),
                object : CustomDialog.CustomDialogCallback {
                    override fun onClick(
                        dialog: CustomDialog,
                        message: String
                    ) {//????????? ?????? ????????? ????????? ????????????
                        finish()
                        dialog.dismiss()
                    }
                })
            .setLeftButton(
                getString(R.string.daily_matching_write_continue),
                object : CustomDialog.CustomDialogCallback {
                    override fun onClick(
                        dialog: CustomDialog,
                        message: String
                    ) {//?????? ?????? ????????? ????????? ????????????
                        dialog.dismiss()
                    }
                })
        dialog.show()

    }


    override fun onBackPressed() {
        showDialog(this)
    }


    fun requestImg() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.upload_pic_dialog_title)
            // setItems ?????? setAdapter()??? ???????????? ????????? ?????? ??????
            // ????????? ?????? ?????? ???????????? ?????? ??????(???: ???????????????????????? ????????? ?????? ListAdapter??? ????????? ??? ??????.)
            .setItems(
                R.array.upload_pic_dialog_title,
                DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        // ????????? 0 ????????????
                        0 -> {
                            var permissionResult0 = ContextCompat.checkSelfPermission(
                                this,
                                CAMERA_PERMISSION[0]
                            )
                            Log.d("Signup_Image_Upload_Dialog", "checkSelfPermission$which")

                            when (permissionResult0) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    Log.d(
                                        "Signup_Image_Upload_Dialog",
                                        "PERMISSION_GRANTED$which"
                                    )
                                    // ????????? ????????? ?????? ????????? ????????? ??? ?????? ????????? ???????????? ??????
                                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    startActivityForResult(intent, CAMERA_PERMISSION_REQUEST)
                                }
                                PackageManager.PERMISSION_DENIED -> {
                                    Log.d(
                                        "Signup_Image_Upload_Dialog",
                                        "PERMISSION_DENIED$which"
                                    )
                                    // ????????? ????????? ????????? ????????? ?????? ???
                                    // ActivityCompat.requestPermissions(requireActivity(), CAMERA_PERMISSION, CAMERA_PERMISSION_REQUEST)
                                    // Fragment?????? onRequestPermissionsResult ??????????????? requestPermissions??? ??????
                                    requestPermissions(
                                        CAMERA_PERMISSION,
                                        CAMERA_PERMISSION_REQUEST
                                    )
                                    // ??? ??? onRequestPermissionsResult ????????? ??????
                                }
                            }
                        }

                        // ?????? ??????
                        1 -> {
                            Log.d("Signup_Image_Upload_Dialog", "?????? ?????? $which")
                            val rejectedPermissionList = ArrayList<String>()
                            // ????????? ??????????????? ?????? ????????? ???????????? ??????
                            for (permission in permission_list) {
                                if (ContextCompat.checkSelfPermission(
                                        this,
                                        permission
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    //?????? ????????? ????????? rejectedPermissionList??? ??????
                                    rejectedPermissionList.add(permission)
                                }
                            }
                            if (rejectedPermissionList.isNotEmpty()) {   // ????????? ????????? ??????????
                                val array = arrayOfNulls<String>(rejectedPermissionList.size)
                                requestPermissions(
                                    rejectedPermissionList.toArray(array),
                                    multiplePermissionsCode2
                                )
                            } else {
                                // ???????????? ????????? ????????? ??? ????????? Intent
                                val albumIntent = Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                                // ????????? ??????????????? ?????? ??????(???????????? ????????? ??? ?????? ???)
                                albumIntent.type = "image/*"
                                // ????????? ????????? ?????? ??????
                                val mimeType = arrayOf("image/*")
                                albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
                                albumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                startActivityForResult(albumIntent, multiplePermissionsCode2)
                            }
                        }
                    }
                })
        dialogBuilder.create().show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d("Signup_Image_Upload_Dialog", "OnRequestPermissionsResult ?????????.")
        val file_path = this.getExternalFilesDir(null).toString()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ????????? ????????? ??? ?????? ???????????? ??????
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, CAMERA_PERMISSION_REQUEST)

                } else {
                    // ?????? ?????? ??? ?????? ?????????
                    Log.d("Signup_Image_Upload_Dialog", "OnRequestPermissionsResult?????? ?????????0 ?????? ??????.")
                    Toast.makeText(
                        this,
                        "????????? ????????? ?????????????????? ????????? ?????? ????????? ???????????? ?????????.",
                        Toast.LENGTH_LONG
                    ).show()

                }
            }

            multiplePermissionsCode1 -> {
                var startCam = true
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        // ????????? ?????? permission??? ?????????
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d("Signup", "??????????????? ?????? ?????? ?????????")
                            Toast.makeText(
                                this,
                                "????????? ????????? ?????????????????? ????????? ?????? ????????? ???????????? ?????????.",
                                Toast.LENGTH_LONG
                            ).show()
                            startCam = false
                        }
                    }
                    if (startCam) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                        // ????????? ????????? ????????? ?????? ??????
                        val file_name = "/temp_${System.currentTimeMillis()}.jpg"
                        // ?????? + ?????? ??????
                        val pic_path = "$file_path/$file_name"
                        val file = File(pic_path)

                        // ????????? ????????? ????????? ???????????? Uri ??????
                        // val contentUri = Uri(pic_path) // ???????????? ???????????? ???????????? ?????? ?????? ??????
                        // -> ?????? ??????????????? OS 6.0 ????????? OS?????? ?????? ????????? ?????? ????????? ??? ????????? ????????? ??? ????????? ???????????? ??????. ????????? ??? ????????? Uri ????????? ?????????.
                        contentUri = FileProvider.getUriForFile(
                            this,
                            "com.duos.camera.file_provider",
                            file
                        )

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
                        startActivityForResult(intent, 200)
                    }
                }
            }
            multiplePermissionsCode2 -> {
                var startAlb = true
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        // ????????? ?????? permission??? ?????????
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d("Signup", "??????????????? ?????? ?????? ?????????")
                            Toast.makeText(
                                this,
                                "????????? ????????? ?????????????????? ????????? ?????? ????????? ???????????? ?????????.",
                                Toast.LENGTH_LONG
                            ).show()
                            startAlb = false
                        }
                    }
                    if (startAlb) {
                        // ???????????? ????????? ????????? ??? ????????? Intent
                        val albumIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        // ????????? ??????????????? ?????? ??????(???????????? ????????? ??? ?????? ???)
                        albumIntent.type = "image/*"
                        // ????????? ????????? ?????? ??????
                        val mimeType = arrayOf("image/*")
                        albumIntent.action = Intent.ACTION_GET_CONTENT
                        albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
                        albumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        startActivityForResult(albumIntent, multiplePermissionsCode2)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    // data : Intent ?????? ?????? ????????? ?????????
                    val bitmap = data?.getParcelableExtra<Bitmap>("data")
                    contentBitmap = bitmap!!
                    if (viewModel.dailyMatchingImg01.value == false) {
                        Log.d("??????1???", "false")
                        viewModel.dailyMatchingImgCount.value =
                            viewModel.dailyMatchingImgCount.value?.plus(1)
                        viewModel.dailyMatchingImg01Bitmap.value = contentBitmap
                        viewModel.dailyMatchingImg01.value = true
                        binding.dailyMatchingEditSelectImageLayout01Iv.visibility = View.VISIBLE
                        binding.dailyMatchingEditSelectImage01Iv.scaleType =
                            ImageView.ScaleType.FIT_XY
                        binding.dailyMatchingEditSelectImage01Iv.setImageBitmap(bitmap)

                    } else if (viewModel.dailyMatchingImg02.value == false) {
                        Log.d("??????2???", "false")
                        viewModel.dailyMatchingImgCount.value =
                            viewModel.dailyMatchingImgCount.value?.plus(1)
                        viewModel.dailyMatchingImg02Bitmap.value = contentBitmap
                        viewModel.dailyMatchingImg02.value = true
                        binding.dailyMatchingEditSelectImageLayout02Iv.visibility = View.VISIBLE
                        binding.dailyMatchingEditSelectImage02Iv.scaleType =
                            ImageView.ScaleType.FIT_XY
                        binding.dailyMatchingEditSelectImage02Iv.setImageBitmap(bitmap)

                    } else if (viewModel.dailyMatchingImg03.value == false) {
                        Log.d("??????3???", "false")
                        viewModel.dailyMatchingImgCount.value =
                            viewModel.dailyMatchingImgCount.value?.plus(1)
                        viewModel.dailyMatchingImg03Bitmap.value = contentBitmap
                        viewModel.dailyMatchingImg03.value = true
                        binding.dailyMatchingEditSelectImageLayout03Iv.visibility = View.VISIBLE
                        binding.dailyMatchingEditSelectImage03Iv.scaleType =
                            ImageView.ScaleType.FIT_XY
                        binding.dailyMatchingEditSelectImage03Iv.setImageBitmap(bitmap)

                    } else {
                        showToast("????????? 3????????? ?????? ???????????????.")
                    }
                }
            }

            multiplePermissionsCode2 -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("??????2", resultCode.toString())
                    // ????????? ???????????? ?????? ???????????? ???????????? Uri ????????? ??????
                    if (data != null) {

                        if (data?.clipData?.getItemCount()!! > 3) {
                            showToast("????????? 3????????? ?????? ???????????????.")
                            return
                        }

                        // ???????????? ????????? ????????? ??????
                        if (data.clipData == null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Log.d("??????", "??????1")
                                val uri = data?.data!!

                                val source =
                                    ImageDecoder.createSource(this.contentResolver, uri)
                                val bitmap = ImageDecoder.decodeBitmap(source)
                                contentBitmap = bitmap

                            } else {
                                Log.d("??????", "??????2")
                                val uri = data?.data!!
                                val cursor =
                                    this.contentResolver.query(uri, null, null, null, null)
                                if (cursor != null) {
                                    cursor.moveToNext()
                                    // ????????? ????????? ????????????.
                                    val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                                    val source = cursor.getString(index)
                                    // ????????? ??????
                                    val bitmap = BitmapFactory.decodeFile(source)
                                    contentBitmap = bitmap
                                }
                            }
                            if (viewModel.dailyMatchingImg01.value == false) {
                                viewModel.dailyMatchingImgCount.value =
                                    viewModel.dailyMatchingImgCount.value?.plus(1)
                                viewModel.dailyMatchingImg01Bitmap.value = contentBitmap
                                viewModel.dailyMatchingImg01.value = true
                                binding.dailyMatchingEditSelectImageLayout01Iv.visibility =
                                    View.VISIBLE
                                binding.dailyMatchingEditSelectImage01Iv.scaleType =
                                    ImageView.ScaleType.FIT_XY
                                binding.dailyMatchingEditSelectImage01Iv.setImageBitmap(
                                    contentBitmap
                                )
                            } else if (viewModel.dailyMatchingImg02.value == false) {
                                viewModel.dailyMatchingImgCount.value =
                                    viewModel.dailyMatchingImgCount.value?.plus(1)
                                viewModel.dailyMatchingImg02Bitmap.value = contentBitmap
                                viewModel.dailyMatchingImg02.value = true
                                binding.dailyMatchingEditSelectImageLayout02Iv.visibility =
                                    View.VISIBLE
                                binding.dailyMatchingEditSelectImage02Iv.scaleType =
                                    ImageView.ScaleType.FIT_XY
                                binding.dailyMatchingEditSelectImage02Iv.setImageBitmap(
                                    contentBitmap
                                )
                            } else if (viewModel.dailyMatchingImg03.value == false) {
                                viewModel.dailyMatchingImgCount.value =
                                    viewModel.dailyMatchingImgCount.value?.plus(1)
                                viewModel.dailyMatchingImg03Bitmap.value = contentBitmap
                                viewModel.dailyMatchingImg03.value = true
                                binding.dailyMatchingEditSelectImageLayout03Iv.visibility =
                                    View.VISIBLE
                                binding.dailyMatchingEditSelectImage03Iv.scaleType =
                                    ImageView.ScaleType.FIT_XY
                                binding.dailyMatchingEditSelectImage03Iv.setImageBitmap(
                                    contentBitmap
                                )
                            } else {
                                showToast("????????? 3????????? ?????? ???????????????.")
                            }
                        } else if (data.clipData!!.itemCount >= 1 && data.clipData!!.itemCount <= 3) {
                            for (i in 0 until data.clipData!!.getItemCount()) {
                                val uri = data.clipData!!.getItemAt(i).uri
                                val source =
                                    ImageDecoder.createSource(this.contentResolver, uri)
                                val bitmap = ImageDecoder.decodeBitmap(source)
                                contentBitmap = bitmap

                                if (viewModel.dailyMatchingImg01.value == false) {
                                    viewModel.dailyMatchingImgCount.value =
                                        viewModel.dailyMatchingImgCount.value?.plus(1)
                                    viewModel.dailyMatchingImg01Bitmap.value = contentBitmap
                                    viewModel.dailyMatchingImg01.value = true
                                    binding.dailyMatchingEditSelectImageLayout01Iv.visibility =
                                        View.VISIBLE
                                    binding.dailyMatchingEditSelectImage01Iv.scaleType =
                                        ImageView.ScaleType.FIT_XY
                                    binding.dailyMatchingEditSelectImage01Iv.setImageBitmap(
                                        contentBitmap
                                    )
                                } else if (viewModel.dailyMatchingImg02.value == false) {
                                    viewModel.dailyMatchingImgCount.value =
                                        viewModel.dailyMatchingImgCount.value?.plus(1)
                                    viewModel.dailyMatchingImg02Bitmap.value = contentBitmap
                                    viewModel.dailyMatchingImg02.value = true
                                    binding.dailyMatchingEditSelectImageLayout02Iv.visibility =
                                        View.VISIBLE
                                    binding.dailyMatchingEditSelectImage02Iv.scaleType =
                                        ImageView.ScaleType.FIT_XY
                                    binding.dailyMatchingEditSelectImage02Iv.setImageBitmap(
                                        contentBitmap
                                    )
                                } else if (viewModel.dailyMatchingImg03.value == false) {
                                    viewModel.dailyMatchingImgCount.value =
                                        viewModel.dailyMatchingImgCount.value?.plus(1)
                                    viewModel.dailyMatchingImg03Bitmap.value = contentBitmap
                                    viewModel.dailyMatchingImg03.value = true
                                    binding.dailyMatchingEditSelectImageLayout03Iv.visibility =
                                        View.VISIBLE
                                    binding.dailyMatchingEditSelectImage03Iv.scaleType =
                                        ImageView.ScaleType.FIT_XY
                                    binding.dailyMatchingEditSelectImage03Iv.setImageBitmap(
                                        contentBitmap
                                    )
                                } else {
                                    showToast("????????? 3????????? ?????? ???????????????.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inner class BitmapRequestBody(private val bitmap: Bitmap) : RequestBody() {
        override fun contentType(): MediaType = "image/png".toMediaType()
        override fun writeTo(sink: BufferedSink) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, sink.outputStream())
        }
    }

    override fun onDailyMatchingEditSuccess(dailyWriteResult: DailyWriteResult) {
        Log.d(ApplicationClass.TAG, dailyWriteResult.boradIdx.toString())
        finish()
    }

    override fun onDailyMatchingEditFailure(code: Int, message: String) {
        showToast("???????????? ?????? ?????? ??? ?????? ??????????????????.")
    }

    private fun setImage01() {

        Picasso.get().load(dailyMatchingInfo.urls[0]).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                binding.dailyMatchingEditSelectImage01Iv.setImageBitmap(bitmap)
                viewModel.dailyMatchingImg01Bitmap.value = bitmap
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

        })

        viewModel.dailyMatchingImgCount.value =
            viewModel.dailyMatchingImgCount.value?.plus(1)
        binding.dailyMatchingEditSelectImageLayout01Iv.visibility = View.VISIBLE

        binding.dailyMatchingEditSelectImage01Iv.scaleType = ImageView.ScaleType.FIT_XY
        viewModel.dailyMatchingImg01.value = true
    }

    private fun setImage02() {

        Picasso.get().load(dailyMatchingInfo.urls[1]).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                binding.dailyMatchingEditSelectImage02Iv.setImageBitmap(bitmap)
                viewModel.dailyMatchingImg02Bitmap.value = bitmap
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

        })

        viewModel.dailyMatchingImgCount.value =
            viewModel.dailyMatchingImgCount.value?.plus(1)
        binding.dailyMatchingEditSelectImageLayout02Iv.visibility = View.VISIBLE
        binding.dailyMatchingEditSelectImage02Iv.scaleType = ImageView.ScaleType.FIT_XY
        viewModel.dailyMatchingImg02.value = true
    }


    private fun setImage03() {

        Picasso.get().load(dailyMatchingInfo.urls[2]).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                binding.dailyMatchingEditSelectImage03Iv.setImageBitmap(bitmap)
                viewModel.dailyMatchingImg03Bitmap.value = bitmap
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

        })

        viewModel.dailyMatchingImgCount.value =
            viewModel.dailyMatchingImgCount.value?.plus(1)

        binding.dailyMatchingEditSelectImageLayout03Iv.visibility = View.VISIBLE
        binding.dailyMatchingEditSelectImage03Iv.scaleType = ImageView.ScaleType.FIT_XY
        viewModel.dailyMatchingImg03.value = true
    }

}