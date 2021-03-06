package com.example.duos.ui.main.dailyMatching

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duos.ApplicationClass.Companion.TAG
import com.example.duos.CustomDialog
import com.example.duos.R
import com.example.duos.data.remote.dailyMatching.DailyMatchingWriteService
import com.example.duos.data.remote.dailyMatching.DailyWriteResult
import com.example.duos.databinding.ActivityDailyMatchingWriteBinding
import com.example.duos.ui.BaseActivity
import com.example.duos.utils.ViewModel
import com.example.duos.utils.getUserIdx
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
import java.text.SimpleDateFormat
import java.util.*

class DailyMatchingWrite :
    BaseActivity<ActivityDailyMatchingWriteBinding>(ActivityDailyMatchingWriteBinding::inflate),
    DailyMatchingWriteView {

    var setTime: Int = 0
    var start: Int = 0
    var end: Int = 0
    lateinit var matchDate: LocalDate
    lateinit var startTime: LocalDateTime
    lateinit var endTime: LocalDateTime
    lateinit var viewModel: ViewModel
    var contentBitmap: Bitmap? = null
    var isSelectedImg : Boolean = false


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


    override fun initAfterBinding() {

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ).get(ViewModel::class.java)
        viewModel.dailyMatchingDateCheck.value = false
        viewModel.dailyMatchingTimeCheck.value = false
        viewModel.dailyMatchingImgCount.value = 0
        binding.viewmodel = viewModel

        val currentTime = LocalDateTime.now()

        var dailyMatchingWritetRVAdapter = DailyMatchingTimeSelectRVAdapter(setTime)
        binding.dailyMatchingWriteSelectTimeRecyclerviewRv.setHasFixedSize(false)
        binding.dailyMatchingWriteSelectTimeRecyclerviewRv.itemAnimator = null
        binding.dailyMatchingWriteSelectTimeRecyclerviewRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        var now = LocalDate.now()
        val tomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS)
        val dayAfterTomorrow = LocalDate.now().plus(2, ChronoUnit.DAYS)
        var strNow = now.format(DateTimeFormatter.ofPattern("M??? d???(E)", Locale.KOREAN))
        var strTomorrow = tomorrow.format(DateTimeFormatter.ofPattern("M??? d???(E)", Locale.KOREAN))
        var strDayAfterTomorrow =
            dayAfterTomorrow.format(DateTimeFormatter.ofPattern("M??? d???(E)", Locale.KOREAN))
        binding.dailyMatchingTodayDateTv.text = strNow
        binding.dailyMatchingTomorrowDateTv.text = strTomorrow
        binding.dailyMatchingDayAfterTomorrowDateTv.text = strDayAfterTomorrow

        binding.dailyMatchingWriteSelectTimeRecyclerviewRv.adapter = dailyMatchingWritetRVAdapter

        dailyMatchingWritetRVAdapter.setMyItemClickListener(object :
            DailyMatchingTimeSelectRVAdapter.MyItemClickListener {
            override fun onClickItem(startPosition: Int, endPosition: Int) {
                for (i in startPosition until endPosition + 1) {
                    val view =
                        binding.dailyMatchingWriteSelectTimeRecyclerviewRv.findViewHolderForAdapterPosition(
                            i
                        )?.itemView
                    val btn = view?.findViewById<Button>(R.id.daily_matching_time_selector_btn)
                    btn?.isSelected = true
                    start = setTime + startPosition
                    end = setTime + endPosition + 1
                    binding.dailyMatchingWriteSelectTimeTv.text =
                        ((setTime + startPosition).toString() + ":00 - " +
                                (setTime + endPosition + 1).toString() + ":00" + "(" + (endPosition - startPosition + 1).toString() + "??????)")
                    if (viewModel.dailyMatchingTimeCheck.value == false)
                        viewModel.dailyMatchingTimeCheck.value = true
                }
            }

            override fun onResetItem(startPosition: Int) {
                if (viewModel.dailyMatchingTimeCheck.value == false)
                    viewModel.dailyMatchingTimeCheck.value = true
                binding.dailyMatchingWriteSelectTimeTv.text =
                    ((setTime + startPosition).toString() + ":00 - " +
                            (setTime + startPosition + 1).toString() + ":00" + "(" + "1??????)")
                for (i in 0 until dailyMatchingWritetRVAdapter.itemCount) {
                    val view =
                        binding.dailyMatchingWriteSelectTimeRecyclerviewRv.findViewHolderForAdapterPosition(
                            i
                        )?.itemView
                    val btn = view?.findViewById<Button>(R.id.daily_matching_time_selector_btn)
                    btn?.isSelected = false
                }
                start = setTime + startPosition
                end = setTime + startPosition + 1
            }
        })

        this.viewModel.dailyMatchingTitle.observe(this, {
            Log.d("1", "??????")
            if (it!!.isNotEmpty() && this.viewModel.dailyMatchingPlace.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ) {
                setWriteBtnEnable()
            } else setWriteBtnUnable()
        })
        this.viewModel.dailyMatchingPlace.observe(this, {
            Log.d("2", "??????")
            if (it!!.isNotEmpty() && this.viewModel.dailyMatchingTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ) {
                setWriteBtnEnable()
            } else setWriteBtnUnable()
        })
        this.viewModel.dailyMatchingContent.observe(this, {
            Log.d("3", "??????")
            if (it!!.isNotEmpty() && this.viewModel.dailyMatchingTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingPlace.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ) {
                setWriteBtnEnable()
            } else setWriteBtnUnable()
        })
        this.viewModel.dailyMatchingDateCheck.observe(this, {
            Log.d("4", "??????")
            if (it!! && this.viewModel.dailyMatchingTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingPlace.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingTimeCheck.value == true
            ){
                setWriteBtnEnable()
            } else setWriteBtnUnable()
        })
        this.viewModel.dailyMatchingTimeCheck.observe(this, {
            Log.d("5", "??????")
            if (it!! && this.viewModel.dailyMatchingTitle.value?.isNotEmpty() == true
                && this.viewModel.dailyMatchingPlace.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingContent.value?.isNotEmpty() == true &&
                this.viewModel.dailyMatchingDateCheck.value == true
            ) {
                setWriteBtnEnable()
            } else setWriteBtnUnable()
        })


        binding.dailyMatchingWriteRadioGroupRg.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val radio: RadioButton = findViewById(checkedId)
            if (viewModel.dailyMatchingDateCheck.value == false)
                viewModel.dailyMatchingDateCheck.value = true
            if (viewModel.dailyMatchingTimeCheck.value == true)
                viewModel.dailyMatchingTimeCheck.value = false
            when (radio.tag) {
                "today" -> {
                    setTime = currentTime.hour + 1

                    dailyMatchingWritetRVAdapter.updateStartTime(setTime)
                    binding.dailyMatchingTodayTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.primary
                        )
                    )
                    binding.dailyMatchingTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    binding.dailyMatchingDayAfterTomorrowTv.setTextColor(
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
                    dailyMatchingWritetRVAdapter.updateStartTime(setTime)
                    binding.dailyMatchingTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.primary
                        )
                    )
                    binding.dailyMatchingDayAfterTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    binding.dailyMatchingTodayTv.setTextColor(
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
                    dailyMatchingWritetRVAdapter.updateStartTime(setTime)

                    binding.dailyMatchingDayAfterTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.primary
                        )
                    )
                    binding.dailyMatchingTomorrowTv.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_gray_A
                        )
                    )
                    binding.dailyMatchingTodayTv.setTextColor(
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
            binding.dailyMatchingWriteImageCountTv.text = it.toString()
        })

        binding.dailyMatchingWriteBackArrowIv.setOnClickListener {
            showDialog(this)
        }

        binding.dailyMatchingWriteBtn.setOnClickListener {
            startTime =
                LocalDateTime.of(matchDate.year, matchDate.month, matchDate.dayOfMonth, start, 0, 0)
            endTime =
                LocalDateTime.of(matchDate.year, matchDate.month, matchDate.dayOfMonth, end, 0, 0)


            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = JSONObject()

            json.put("writerIdx", getUserIdx()!!)
            json.put("title", viewModel.dailyMatchingTitle.value.toString())
            json.put("content", viewModel.dailyMatchingContent.value.toString())
            json.put("matchDate", matchDate)
            json.put("matchPlace", viewModel.dailyMatchingPlace.value.toString())
            json.put("startTime", startTime)
            json.put("endTime", endTime)

            Log.d("matchDate startTime endTime","$matchDate $startTime $endTime")

            val writeInfo = json.toString().toRequestBody(JSON)

            val bitmapMultipartBody : MutableList<MultipartBody.Part> = mutableListOf()
            if (viewModel.dailyMatchingImg01Bitmap.value != null){
                val bitmapRequestBody = viewModel.dailyMatchingImg01Bitmap.value.let { BitmapRequestBody(it!!) }
                bitmapMultipartBody.add(MultipartBody.Part.createFormData("mFiles", "img01", bitmapRequestBody))
                isSelectedImg = true
            }
            if (viewModel.dailyMatchingImg02Bitmap.value != null){
                val bitmapRequestBody = viewModel.dailyMatchingImg02Bitmap.value.let { BitmapRequestBody(it!!) }
                bitmapMultipartBody.add(MultipartBody.Part.createFormData("mFiles", "img02", bitmapRequestBody))
                isSelectedImg = true
            }
            if (viewModel.dailyMatchingImg03Bitmap.value != null){
                val bitmapRequestBody = viewModel.dailyMatchingImg03Bitmap.value.let { BitmapRequestBody(it!!) }
                bitmapMultipartBody.add(MultipartBody.Part.createFormData("mFiles", "img03", bitmapRequestBody))
                isSelectedImg = true
            }

            // ???????????? ???????????????
            if (isSelectedImg){
                Log.d("??????",bitmapMultipartBody.toString())
                DailyMatchingWriteService.dailyMatchingWriteWithImg(this, bitmapMultipartBody, writeInfo)
            }
            // ?????? ????????? ????????? ??????????????????
            else{
                DailyMatchingWriteService.dailyMatchingWriteWithoutImg(this, writeInfo)
            }
        }


        binding.dailyMatchingWriteSelectImageLayoutCl.setOnClickListener {
            requestImg()
        }

        binding.dailyMatchingWriteSelectErase01Iv.setOnClickListener {
            viewModel.dailyMatchingImg01.value = false
            viewModel.dailyMatchingImg01Bitmap.value = null
            viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.minus(1)
            binding.dailyMatchingWriteSelectImageLayout01Iv.visibility = View.GONE
        }

        binding.dailyMatchingWriteSelectErase02Iv.setOnClickListener {
            viewModel.dailyMatchingImg02.value = false
            viewModel.dailyMatchingImg02Bitmap.value = null
            viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.minus(1)
            binding.dailyMatchingWriteSelectImageLayout02Iv.visibility = View.GONE
        }

        binding.dailyMatchingWriteSelectErase03Iv.setOnClickListener {
            viewModel.dailyMatchingImg03.value = false
            viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.minus(1)
            viewModel.dailyMatchingImg03Bitmap.value = null
            binding.dailyMatchingWriteSelectImageLayout03Iv.visibility = View.GONE
        }
    }


    fun setWriteBtnEnable() {
        binding.dailyMatchingWriteBtn.isEnabled = true
        binding.dailyMatchingWriteBtn.background = getDrawable(R.drawable.signup_next_btn_done_rectangular)
        binding.dailyMatchingWriteBtn.setTextColor(getColor(R.color.white))
    }

    fun setWriteBtnUnable() {
        binding.dailyMatchingWriteBtn.isEnabled = false
        binding.dailyMatchingWriteBtn.background = getDrawable(R.drawable.signup_next_btn_rectangular)
        binding.dailyMatchingWriteBtn.setTextColor(getColor(R.color.dark_gray_B0))
    }

    fun showDialog(context: Context) {
        // ?????? ?????????????????? ??????????????? ?????? requireContext() ??? ?????? context??? ???????????? ???.
        val dialog = CustomDialog.Builder(context)
            .setCommentMessage(getString(R.string.daily_matching_write_cancel))// Dialog????????? ???????????? "~~~ "
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

    override fun onDailyMatchingWriteSuccess(dailyWriteResult: DailyWriteResult) {
        Log.d(TAG, dailyWriteResult.boradIdx.toString())
        finish()
    }

    override fun onDailyMatchingWriteFailure(code: Int, message: String) {
        Log.d(TAG, code.toString() + " : " + message)
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
                        viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                        viewModel.dailyMatchingImg01Bitmap.value = contentBitmap
                        viewModel.dailyMatchingImg01.value = true
                        binding.dailyMatchingWriteSelectImageLayout01Iv.visibility = View.VISIBLE
                        binding.dailyMatchingWriteSelectImage01Iv.scaleType =
                            ImageView.ScaleType.FIT_XY
                        binding.dailyMatchingWriteSelectImage01Iv.setImageBitmap(bitmap)

                    } else if (viewModel.dailyMatchingImg02.value == false) {
                        Log.d("??????2???", "false")
                        viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                        viewModel.dailyMatchingImg02Bitmap.value = contentBitmap
                        viewModel.dailyMatchingImg02.value = true
                        binding.dailyMatchingWriteSelectImageLayout02Iv.visibility = View.VISIBLE
                        binding.dailyMatchingWriteSelectImage02Iv.scaleType =
                            ImageView.ScaleType.FIT_XY
                        binding.dailyMatchingWriteSelectImage02Iv.setImageBitmap(bitmap)

                    } else if (viewModel.dailyMatchingImg03.value == false) {
                        Log.d("??????3???", "false")
                        viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                        viewModel.dailyMatchingImg03Bitmap.value = contentBitmap
                        viewModel.dailyMatchingImg03.value = true
                        binding.dailyMatchingWriteSelectImageLayout03Iv.visibility = View.VISIBLE
                        binding.dailyMatchingWriteSelectImage03Iv.scaleType =
                            ImageView.ScaleType.FIT_XY
                        binding.dailyMatchingWriteSelectImage03Iv.setImageBitmap(bitmap)

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
                                viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                                viewModel.dailyMatchingImg01Bitmap.value = contentBitmap
                                viewModel.dailyMatchingImg01.value = true
                                binding.dailyMatchingWriteSelectImageLayout01Iv.visibility =
                                    View.VISIBLE
                                binding.dailyMatchingWriteSelectImage01Iv.scaleType =
                                    ImageView.ScaleType.FIT_XY
                                binding.dailyMatchingWriteSelectImage01Iv.setImageBitmap(
                                    contentBitmap
                                )
                            } else if (viewModel.dailyMatchingImg02.value == false) {
                                viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                                viewModel.dailyMatchingImg02Bitmap.value = contentBitmap
                                viewModel.dailyMatchingImg02.value = true
                                binding.dailyMatchingWriteSelectImageLayout02Iv.visibility =
                                    View.VISIBLE
                                binding.dailyMatchingWriteSelectImage02Iv.scaleType =
                                    ImageView.ScaleType.FIT_XY
                                binding.dailyMatchingWriteSelectImage02Iv.setImageBitmap(
                                    contentBitmap
                                )
                            } else if (viewModel.dailyMatchingImg03.value == false) {
                                viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                                viewModel.dailyMatchingImg03Bitmap.value = contentBitmap
                                viewModel.dailyMatchingImg03.value = true
                                binding.dailyMatchingWriteSelectImageLayout03Iv.visibility =
                                    View.VISIBLE
                                binding.dailyMatchingWriteSelectImage03Iv.scaleType =
                                    ImageView.ScaleType.FIT_XY
                                binding.dailyMatchingWriteSelectImage03Iv.setImageBitmap(
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
                                    viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                                    viewModel.dailyMatchingImg01Bitmap.value = contentBitmap
                                    viewModel.dailyMatchingImg01.value = true
                                    binding.dailyMatchingWriteSelectImageLayout01Iv.visibility =
                                        View.VISIBLE
                                    binding.dailyMatchingWriteSelectImage01Iv.scaleType =
                                        ImageView.ScaleType.FIT_XY
                                    binding.dailyMatchingWriteSelectImage01Iv.setImageBitmap(
                                        contentBitmap
                                    )
                                } else if (viewModel.dailyMatchingImg02.value == false) {
                                    viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                                    viewModel.dailyMatchingImg02Bitmap.value = contentBitmap
                                    viewModel.dailyMatchingImg02.value = true
                                    binding.dailyMatchingWriteSelectImageLayout02Iv.visibility =
                                        View.VISIBLE
                                    binding.dailyMatchingWriteSelectImage02Iv.scaleType =
                                        ImageView.ScaleType.FIT_XY
                                    binding.dailyMatchingWriteSelectImage02Iv.setImageBitmap(
                                        contentBitmap
                                    )
                                } else if (viewModel.dailyMatchingImg03.value == false) {
                                    viewModel.dailyMatchingImgCount.value = viewModel.dailyMatchingImgCount.value?.plus(1)
                                    viewModel.dailyMatchingImg03Bitmap.value = contentBitmap
                                    viewModel.dailyMatchingImg03.value = true
                                    binding.dailyMatchingWriteSelectImageLayout03Iv.visibility =
                                        View.VISIBLE
                                    binding.dailyMatchingWriteSelectImage03Iv.scaleType =
                                        ImageView.ScaleType.FIT_XY
                                    binding.dailyMatchingWriteSelectImage03Iv.setImageBitmap(
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
}