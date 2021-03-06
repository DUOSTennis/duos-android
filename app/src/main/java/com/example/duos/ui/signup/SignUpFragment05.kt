package com.example.duos.ui.signup

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.duos.R
import com.example.duos.databinding.FragmentSignup05Binding
import java.io.File
import android.graphics.Matrix
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.duos.ApplicationClass
import com.example.duos.data.entities.User
import com.example.duos.data.local.UserDatabase
import com.example.duos.data.remote.login.LoginVerifyAuthNumResultData
import com.example.duos.data.remote.signUp.SignUpRequestResultData
import com.example.duos.data.remote.signUp.SignUpService
import com.example.duos.ui.main.MainActivity
import com.example.duos.utils.ViewModel
import com.example.duos.utils.saveJwt
import com.example.duos.utils.saveRefreshJwt
import com.example.duos.utils.saveUserIdx
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Part.Companion.createFormData
import okio.BufferedSink
import org.json.JSONObject


class SignUpFragment05() : Fragment(), SignUpRequestView {

    lateinit var contentUri: Uri
    lateinit var binding: FragmentSignup05Binding
    lateinit var onGoingNextListener: SignUpGoNextInterface

    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val CAMERA_PERMISSION_REQUEST = 100
    lateinit var signupNextBtnListener: SignUpNextBtnInterface
    lateinit var mContext: SignUpActivity
    lateinit var viewModel: ViewModel
    var profileBitmap: Bitmap? = null
    var savedState: Bundle? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SignUpActivity) {
            mContext = context
        }
    }

    // ??????????????? ?????????1
    val requiredPermissions1 = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
    )
    val multiplePermissionsCode1 = 200

    // ??????????????? ?????????(??????)2
    val permission_list = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )
    val multiplePermissionsCode2 = 300

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignup05Binding.inflate(inflater, container, false)
        requireActivity().findViewById<TextView>(R.id.signup_process_tv).text = "05"
        signupNextBtnListener = mContext
        onGoingNextListener = mContext
        viewModel = ViewModelProvider(requireActivity()).get(ViewModel::class.java)

        if (savedInstanceState != null && savedState == null) {
            savedState = savedInstanceState.getBundle("savedState")
        }
        if (savedState != null) {
            Log.d("??????", "??????")
        } else {
            Log.d("??????", "??????X")
            signupNextBtnListener.onNextBtnUnable()
        }
        savedState = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewmodel = viewModel

        this.viewModel.profileImg.observe(viewLifecycleOwner, {
            if (it != null) {
                this.viewModel.introduce.observe(viewLifecycleOwner, { it2 ->
                    if (it2 != null) {
                        if (it2.isNotEmpty())
                            signupNextBtnListener.onNextBtnEnable()
                        else signupNextBtnListener.onNextBtnUnable()
                    }
                })
            } else signupNextBtnListener.onNextBtnUnable()
        })


        val file_path = requireActivity().getExternalFilesDir(null).toString()

        binding.signup05ProfileImageBackgroundIv.setOnClickListener {

            val dialogBuilder = AlertDialog.Builder(activity)
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
                                    requireContext(),
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

                            1 -> {
                                Log.d("Signup_Image_Upload_Dialog", "?????? ?????? $which")
                                val rejectedPermissionList = ArrayList<String>()
                                // ????????? ??????????????? ?????? ????????? ???????????? ??????
                                for (permission in permission_list) {
                                    if (ContextCompat.checkSelfPermission(
                                            requireContext(),
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
                                    startActivityForResult(albumIntent, multiplePermissionsCode2)
                                }
                            }
                        }
                    })
            dialogBuilder.create().show()

            // ??????????????? ??????

        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d("Signup_Image_Upload_Dialog", "OnRequestPermissionsResult ?????????.")
        val file_path = requireActivity().getExternalFilesDir(null).toString()
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
                        requireContext(),
                        "????????? ????????? ?????????????????? ????????? ?????? ????????? ???????????? ?????????.",
                        Toast.LENGTH_LONG
                    ).show()

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
                                requireContext(),
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
                        albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
                        startActivityForResult(albumIntent, multiplePermissionsCode2)
                    }
                }
            }
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    // data : Intent ?????? ?????? ????????? ?????????
                    val bitmap = data?.getParcelableExtra<Bitmap>("data")
                    profileBitmap = bitmap!!
                    viewModel.profileImg.value = profileBitmap
                    binding.signup05ProfileImageBackgroundIv.setImageBitmap(bitmap)
//                    binding.signup05ProfileImageBackgroundIv.scaleType = ImageView.ScaleType.FIT_XY
                    binding.signup05ProfileImageBackgroundIv.scaleType =
                        ImageView.ScaleType.CENTER_CROP
                }
            }

            multiplePermissionsCode2 -> {
                if (resultCode == RESULT_OK) {
                    // ????????? ???????????? ?????? ???????????? ???????????? Uri ????????? ??????
                    val uri = data?.data
                    if (uri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // ??????????????? 10?????? ??????
                            val source =
                                ImageDecoder.createSource(requireActivity().contentResolver, uri)
                            var bitmap = ImageDecoder.decodeBitmap(source)
                            bitmap = resizeBitmap(1024, bitmap)
                            profileBitmap = bitmap
                            viewModel.profileImg.value = profileBitmap
                            binding.signup05ProfileImageBackgroundIv.setImageBitmap(bitmap)
                            binding.signup05ProfileImageBackgroundIv.scaleType =
                                ImageView.ScaleType.CENTER_CROP

                        } else {
                            val cursor =
                                requireActivity().contentResolver.query(uri, null, null, null, null)
                            if (cursor != null) {
                                cursor.moveToNext()
                                // ????????? ????????? ????????????.
                                val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                                val source = cursor.getString(index)
                                // ????????? ??????
                                var bitmap = BitmapFactory.decodeFile(source)
                                bitmap = resizeBitmap(1024, bitmap)
                                profileBitmap = bitmap
                                viewModel.profileImg.value = profileBitmap
                                binding.signup05ProfileImageBackgroundIv.setImageBitmap(bitmap)
                                binding.signup05ProfileImageBackgroundIv.scaleType =
                                    ImageView.ScaleType.CENTER_CROP
                            }
                        }
                    }
                }
            }
        }
    }

    // ????????? ???????????? ???????????? ?????????
    fun resizeBitmap(targetWidth: Int, source: Bitmap): Bitmap {
        // ????????? ?????? ??????
        val ratio = targetWidth.toDouble() / source.width.toDouble()
        // ????????? ?????? ?????? ?????????
        val targetHeight = (source.height * ratio).toInt()
        // ????????? ????????? bitmap ????????? ??????
        val result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false)
        return result

    }

    // ???????????? ?????? ???????????? ?????????
// 11?????? ???????????? ????????? (??????????????? ?????? ?????????)
    fun getDegree(uri: Uri, source: String): Float {
        var exif: ExifInterface? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val photoUri = MediaStore.setRequireOriginal(uri)
            val stream = requireActivity().contentResolver.openInputStream(photoUri)
            exif = ExifInterface(source)
        } else {
            exif = ExifInterface(source)
        }
        var degree = 0
        var ori = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            -1
        )   // ?????? ???????????? ????????? ????????? ????????? default????????? -1 ?????? (0 ????????? ??????)
        when (ori) {
            ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
        }
        return degree.toFloat()
    }

    // ?????? ?????????
    fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {

        // ???????????? ???????????? ??????
        val matrix = Matrix()
        matrix.postRotate(degree)
        // ????????? ???????????? ????????????.
        val bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return bitmap2
    }

    fun signUpPost() {
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        val mFile = profileBitmap?.let { bitmapToFile(it,"mFile.jpg") }
//        val requestBody: RequestBody =
//            byteArrayOutputStream.toByteArray()
//                .toRequestBody("image/jpg".toMediaTypeOrNull())
//        val uploadFile: MultipartBody.Part = createFormData("mFile", mFile?.name, requestBody)

        val bitmapRequestBody = profileBitmap?.let { BitmapRequestBody(it) }
        val bitmapMultipartBody: MultipartBody.Part? =
            if (bitmapRequestBody == null) null
            else createFormData("mFile", "mFile", bitmapRequestBody)

        FirebaseApp.initializeApp(requireContext())
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    ApplicationClass.TAG,
                    "Fetching FCM registration token failed",
                    task.exception
                )
                return@OnCompleteListener
            }

            // Get new FCM registration token // FCM ?????? ?????? get
            val token = task.result

            Log.d("token", token!!)
//
//            val userInfo = SignUpRequestInfo(
//                viewModel.phoneNumber.value.toString(),
//                viewModel.nickName.value.toString(),
//                viewModel.birthYear.value.toString() + "-" + viewModel.birthMonth.value.toString().padStart(2,'0')
//                        + "-" + viewModel.birthDay.value.toString().padStart(2, '0'),
//                viewModel.gender.value!!,
//                viewModel.location.value!!,
//                viewModel.experience.value!!,
//                binding.signup05IntroduceEt.text.toString(),
//                token!!
//            )

            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = JSONObject()
            val regex = Regex("[^A-Za-z0-9]")//Regex??? ???????????? ???????????? ????????????
            val phoneNumber = regex.replace(viewModel.phoneNumber.value.toString(), "")

            json.put("phoneNumber", phoneNumber)
            json.put("nickname", viewModel.nickName.value.toString())
            json.put(
                "birthDate",
                viewModel.birthYear.value.toString() + "-" + viewModel.birthMonth.value.toString()
                    .padStart(2, '0')
                        + "-" + viewModel.birthDay.value.toString().padStart(2, '0')
            )
            json.put("gender", viewModel.gender.value)
            json.put("locationIdx", viewModel.location.value)
            json.put("experienceIdx", viewModel.experience.value)
            json.put("introduction", binding.signup05IntroduceEt.text.toString())
            json.put("fcmToken", token)

            Log.d(
                "??????", phoneNumber + " " + viewModel.nickName.value.toString() + " " +
                        viewModel.birthYear.value.toString() + "-" + viewModel.birthMonth.value.toString()
                    .padStart(2, '0')
                        + "-" + viewModel.birthDay.value.toString()
                    .padStart(2, '0') + " " + viewModel.gender.value
                        + " " + viewModel.location.value + " " + viewModel.experience.value + " " + binding.signup05IntroduceEt.text.toString() +
                        " " + token
            )

            val userInfo = json.toString().toRequestBody(JSON)
            SignUpService.signUpReqeust(this, bitmapMultipartBody, userInfo)
        })


    }

    inner class BitmapRequestBody(private val bitmap: Bitmap) : RequestBody() {
        override fun contentType(): MediaType = "image/jpeg".toMediaType()
        override fun writeTo(sink: BufferedSink) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, sink.outputStream())
        }
    }

    fun setUser(result: SignUpRequestResultData): User {
        var user = User(
            result.phoneNumber,
            result.nickname,
            result.gender,
            result.birthDate,
            result.locationIdx,
            result.experienceIdx,
            result.profilePhotoUrl,
            result.introduction,
            result.fcmToken,
            result.userIdx
        )
        return user
    }

    override fun onSignUpRequestSuccess(result: SignUpRequestResultData) {
        Log.d("success", "hi")
        //         roomDB ??? ???????????? ?????? ?????? ??????
        // ???????????? ?????? ??????!
        // ????????? ?????? roomDB ??? ?????? or ????????????
        // jwt?????? sharedpreference ??? ??????
        Log.d("user", result.toString())

        val db = UserDatabase.getInstance(requireContext())
        var user = db!!.userDao().getUser(result.userIdx)
        if (user != null) {
            user = setUser(result)
            db!!.userDao().update(user)
        } else {
            user = setUser(result)
            db!!.userDao().insert(user)
        }

        saveUserIdx(result.userIdx)
        saveJwt(result.jwtAccessToken)
        saveRefreshJwt(result.jwtRefreshToken)

        onGoingNextListener.onGoingNext()
    }

    override fun onSignUpRequestFailure(code: Int, message: String) {
        when (code) {
            5025 -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

    }

}