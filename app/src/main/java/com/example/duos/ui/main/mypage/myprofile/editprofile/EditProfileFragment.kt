package com.example.duos.ui.main.mypage.myprofile.editprofile

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
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.duos.R
import com.example.duos.data.entities.User
import com.example.duos.data.entities.duplicate.DuplicateNicknameListView
import com.example.duos.data.entities.editProfile.*
import com.example.duos.data.local.UserDatabase
import com.example.duos.data.remote.duplicate.DuplicateNicknameResponse
import com.example.duos.data.remote.duplicate.DuplicateNicknameService
import com.example.duos.data.remote.editProfile.EditProfileGetService
import com.example.duos.data.remote.editProfile.EditProfilePutPicResponse
import com.example.duos.data.remote.editProfile.EditProfilePutResponse
import com.example.duos.data.remote.editProfile.EditProfilePutService
import com.example.duos.databinding.FragmentEditProfileBinding
import com.example.duos.ui.main.mypage.myprofile.MyProfileActivity
import com.example.duos.ui.signup.localSearch.LocationDialogFragment
import com.example.duos.utils.ViewModel
import com.example.duos.utils.getUserIdx
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Part.Companion.createFormData
import okhttp3.RequestBody
import okio.BufferedSink
import java.util.regex.Pattern

class EditProfileFragment : Fragment(), EditProfileListView,
    EditProfilePutListView, DuplicateNicknameListView, EditProfilePicPutListView {
    val TAG = "EditProfileFragment"
    lateinit var binding: FragmentEditProfileBinding

    lateinit var mContext: EditProfileActivity

    lateinit var viewModel: ViewModel
    var savedState: Bundle? = null
    private val myUserIdx = getUserIdx()
    var locationText: TextView? = null
    var checkStore: Boolean = false
    var inputIntroduction: String = ""
    var originExperience: Int? = null
    var profileBitmap: Bitmap? = null
    private lateinit var expBtn : RadioButton

    lateinit var user: User
    var putSuccess: Boolean = false

    private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    private val CAMERA_PERMISSION_REQUEST = 100

    // ??????????????? ?????????(??????)
    @RequiresApi(Build.VERSION_CODES.Q)
    val permission_list = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )
    private val multiplePermissionsCode2 = 300

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is EditProfileActivity) {
            mContext = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, " onCreateView")
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(ViewModel::class.java)
        return binding.root
    }


    @SuppressLint("SetTextI18n", "ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        onApplyDisable()    // ???????????? ??????
        EditProfileGetService.getEditProfile(this, myUserIdx)   // API ??? ??? ????????? ????????????
        // ??? ????????? ????????? ???????????? (Room)
        val db = UserDatabase.getInstance(requireContext())
        val myProfileDB = db!!.userDao().getUser(myUserIdx)
        originExperience = myProfileDB.experience
        viewModel = ViewModelProvider(requireActivity()).get(ViewModel::class.java)
        binding.viewmodel = viewModel

        if (savedInstanceState != null && savedState == null) {
            savedState = savedInstanceState.getBundle("savedState")
        }
        if (savedInstanceState != null) {
            //  ??????
            checkStore = true
            binding.btnCheckDuplicationTv.isEnabled = false
            binding.btnCheckDuplicationTv.setBackgroundResource(R.drawable.signup_phone_verifying_rectangular)
            binding.btnCheckDuplicationTv.setTextColor(ContextCompat.getColor(mContext, R.color.dark_gray_A))

        } else {
            //  ?????? X
            viewModel.editProfileNickname.value = myProfileDB.nickName
            viewModel.editProfileLocationDialogShowing.value = false
            viewModel.editProfileExperience.value = myProfileDB.experience!!.toInt()
            viewModel.editProfileLocation.value = myProfileDB.location
            viewModel.setEditProfileNickName.value = false
            viewModel.setEditProfileImgUrl.value = false
            viewModel.setEditProfileLocation.value = false
            viewModel.setEditProfileIntroduction.value = false
            viewModel.setEditProfileExperience.value = false
            viewModel.setEditProfileIsDuplicated.value = false
            viewModel.isChangedNickname.value = false
            viewModel.setEditProfileNonPic.value = false

        }
        savedState = null


        // ????????? ?????? ?????? ?????? ?????????
        binding.btnClearIntroductionTv.setOnClickListener {
            binding.contentIntroductionEt.text.clear()
        }

        // ?????? ??????
        binding.myProfileImgIv.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setTitle(R.string.upload_pic_dialog_title)
                .setItems(R.array.upload_pic_dialog_title, DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        // ????????? 0 ????????????
                        0 -> {
                            val permissionResult0 = ContextCompat.checkSelfPermission(
                                requireContext(),
                                CAMERA_PERMISSION[0]
                            )

                            when (permissionResult0) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    // ????????? ????????? ?????? ????????? -> ????????? ???????????? ??????
                                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    startActivityForResult(intent, CAMERA_PERMISSION_REQUEST)
                                }
                                PackageManager.PERMISSION_DENIED -> {
                                    // ????????? ????????? ????????? ????????? ??????
                                    requestPermissions(
                                        CAMERA_PERMISSION,
                                        CAMERA_PERMISSION_REQUEST
                                    )
                                    // ??? ??? onRequestPermissionsResult ????????? ??????
                                }
                            }

                        }

                        // ??? ???????????? ??????
                        1 -> {
                            val rejectedPermissionList = ArrayList<String>()
                            // ????????? ??????????????? ?????? ????????? ???????????? ??????
                            for (permission in permission_list) {
                                if (ContextCompat.checkSelfPermission(
                                        requireContext(),
                                        permission
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    //?????? ????????? ????????? ?????? ????????? rejectedPermissionList ??? ??????
                                    rejectedPermissionList.add(permission)
                                }
                            }
                            // ????????? ????????? ??????????
                            if (rejectedPermissionList.isNotEmpty()) {
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
                                val mimeType = arrayOf("image/*")   // ????????? ????????? ?????? ??????
                                albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
                                startActivityForResult(albumIntent, multiplePermissionsCode2)
                            }
                        }
                    }
                })
            dialogBuilder.create().show()

        }

        // ????????? ??????
        viewModel.editProfileNickname.observe(viewLifecycleOwner) {
            val pattern = "^[0-9|a-z|A-Z|???-???|???-???|???-???]*$"
            if (!checkStore) {  /* ????????? ??? ?????? ???????????????? */
                if (it!!.isNotEmpty()) {
                    binding.nicknameEtField.isEndIconVisible = true
                    if (!Pattern.matches(pattern, it.toString()) or (it.length < 2)) {  /* ???????????? */
                        binding.nickNameErrorTv.visibility = View.VISIBLE
                        binding.nicknameCheckIconIv.visibility = View.VISIBLE
                        binding.nicknameCheckIconIv.setImageResource(R.drawable.ic_signup_nickname_unable)
                        binding.btnCheckDuplicationTv.isEnabled = false
                        binding.btnCheckDuplicationTv.setBackgroundResource(R.drawable.signup_phone_verifying_rectangular)
                        binding.btnCheckDuplicationTv.setTextColor(
                            ContextCompat.getColor(
                                mContext,
                                R.color.dark_gray_A
                            )
                        )
                    } else {            /* ????????? */
                        binding.nickNameErrorTv.visibility = View.INVISIBLE
                        binding.nicknameCheckIconIv.visibility = View.VISIBLE
                        binding.nicknameCheckIconIv.setImageResource(R.drawable.ic_signup_phone_verifying_check_done)
                        binding.btnCheckDuplicationTv.isEnabled = true
                        binding.btnCheckDuplicationTv.setBackgroundResource(R.drawable.signup_phone_verifying_done_rectangular)
                        binding.btnCheckDuplicationTv.setTextColor(
                            ContextCompat.getColor(
                                mContext,
                                R.color.primary
                            )
                        )
                    }
                }
            }
            checkStore = false
        }

        val nicknameEt = binding.nicknameEt
        nicknameEt.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.guideNicknameTv.setTextColor(ContextCompat.getColor(mContext, R.color.nero))
                binding.nicknameEt.setTextColor(ContextCompat.getColor(mContext, R.color.nero))
            } else {
                binding.guideNicknameTv.setTextColor(ContextCompat.getColor(mContext, R.color.grey))
                binding.nicknameEt.setTextColor(ContextCompat.getColor(mContext, R.color.grey))
            }
        }

        nicknameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == myProfileDB.nickName) {
                    viewModel.isChangedNickname.value = false
                    viewModel.setEditProfileNickName.value = false
                    viewModel.editProfileNickname.value = s.toString()
                } else {
                    viewModel.isChangedNickname.value = true
                    viewModel.setEditProfileNickName.value = true
                    viewModel.editProfileNickname.value = s.toString()
                }
            }
        })

        // ?????? ?????? ?????? ?????????
        binding.btnCheckDuplicationTv.setOnClickListener {
            DuplicateNicknameService.getDuplicateNickname(this, myUserIdx, binding.nicknameEt.text.toString())
        }

        locationText = binding.locationInfoTv

        // ?????? ?????? ??????
            // ??????????????? ?????????
        binding.locationInfoTv.setOnClickListener {
            val dialog = LocationDialogFragment()
            activity?.supportFragmentManager?.let { fragmentManager ->
                dialog.show(
                    fragmentManager, "?????? ??????"
                )
            }
        }
            // ?????????????????? ??? observe ?????? ????????? ??? ?????????
        viewModel.editProfileLocationDialogShowing.observe(viewLifecycleOwner,
            Observer {
                if (it) {
                    binding.locationInfoTv.text =
                        viewModel.editProfileLocationCateName.value + " " + viewModel.editProfileLocationName.value
                    viewModel.setEditProfileLocation.value = viewModel.editProfileLocation.value != myProfileDB.location
                }
            })


        // ?????? ??????
        for (i in 1..14) {
            val btnId: Int = resources.getIdentifier(
                "edit_profile_table_" + i.toString() + "_btn",
                "id",
                requireActivity().packageName
            )
            val btn: Button = requireView().findViewById(btnId)
            val num: String = i.toString()
            btn.text = resources.getString(
                resources.getIdentifier(
                    "signup_length_of_play_$num", "string",
                    requireActivity().packageName
                )
            )
            btn.tag = i.toString()
        }

        // ????????? ??????
            // ?????? focus
        binding.contentIntroductionEt.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    binding.guideIntroductionTv.setTextColor(ContextCompat.getColor(mContext, R.color.nero))
                    binding.contentIntroductionEt.setTextColor(ContextCompat.getColor(mContext, R.color.nero))
                    binding.dimensionIntroductionIv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.ic_rectangle_introduction_on
                        )
                    )
                } else {
                    binding.guideIntroductionTv.setTextColor(ContextCompat.getColor(mContext, R.color.grey))
                    binding.contentIntroductionEt.setTextColor(ContextCompat.getColor(mContext, R.color.grey))
                    binding.dimensionIntroductionIv.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.ic_rectangle_introduction_off
                        )
                    )
                }
            }

            // ????????? ????????? ?????? ????????? ????????????
        val introductionEt = binding.contentIntroductionEt
        introductionEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.setEditProfileIntroduction.value = false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == myProfileDB.introduce) {
                    viewModel.setEditProfileIntroduction.value = false
                } else {
                    when {
                        s.toString().length in 1..300 -> { /* EditText ??? ?????? 1 ~ 300 ?????? ?????? ???????????? ??????*/
                            viewModel.setEditProfileIntroduction.value = true
                        }
                        s.toString().length >= 301 -> {    /* EditText ??? ?????? 400 ????????? ?????? */
                            viewModel.setEditProfileIntroduction.value = false
                            Toast.makeText(context, "??????????????? ?????? 300????????? ?????? ???????????????.", Toast.LENGTH_LONG).show()
                        }
                        s.toString().isEmpty() -> { /* EditText ??? ?????? ????????? ?????? ???????????? ??????*/
                            viewModel.setEditProfileIntroduction.value = false
                        }
                    }
                }
            }

        })

        viewModel.editProfileNickname.observe(viewLifecycleOwner) {
            if (it != myProfileDB.nickName) {
                viewModel.setEditProfileIsDuplicated.observe(viewLifecycleOwner) { it2 ->
                    if (it2) {
                        onApplyEnable()
                        viewModel.setEditProfileNonPic.value = true
                    } else {
                        onApplyDisable()
                    }
                }
            }
        }
        viewModel.editProfileNickname.observe(viewLifecycleOwner) {
            if (it == myProfileDB.nickName) {
                viewModel.setEditProfileLocation.observe(viewLifecycleOwner) { it3 ->
                    if (it3 && (viewModel.editProfileNickname.value == myProfileDB.nickName || viewModel.setEditProfileIsDuplicated.value == true)) {    /* ?????? ?????? */
                        onApplyEnable()
                        viewModel.setEditProfileNonPic.value = true
                    }
                }
                viewModel.setEditProfileExperience.observe(viewLifecycleOwner) { it4 ->
                    if (it4 && (viewModel.editProfileNickname.value == myProfileDB.nickName || viewModel.setEditProfileIsDuplicated.value == true)) {
                        onApplyEnable()
                        viewModel.setEditProfileNonPic.value = true
                    }
                }
                viewModel.setEditProfileIntroduction.observe(viewLifecycleOwner) { it5 ->
                    if (it5 && (viewModel.editProfileNickname.value == myProfileDB.nickName || viewModel.setEditProfileIsDuplicated.value == true)) {
                        onApplyEnable()
                        viewModel.setEditProfileNonPic.value = true
                    }
                }
                viewModel.setEditProfileImgUrl.observe(viewLifecycleOwner) { it6 ->
                    if (it6 && (viewModel.editProfileNickname.value == myProfileDB.nickName || viewModel.setEditProfileIsDuplicated.value == true)) {
                        onApplyEnable()
                    }
                }
            } else {
                onApplyDisable()
            }
        }


        binding.activatingApplyBtn.setOnClickListener {
            val phoneNumber = myProfileDB.phoneNumber.toString()
            val nickname = binding.nicknameEt.text.toString()    ////
            val birth = myProfileDB.birth.toString()
            val gender = myProfileDB.gender!!.toInt()
            val locationIdx = viewModel.editProfileLocation.value!!.toInt()     ////
            val experienceIdx = viewModel.editProfileExperience.value!!.toInt()     ////
            val introduction = binding.contentIntroductionEt.text.toString()            ////
            val bitmapRequestBody = profileBitmap?.let { BitmapRequestBody(it) }
            val bitmapMultipartBody: MultipartBody.Part? =
                if (bitmapRequestBody == null) null
                else createFormData("mFile", "mFile", bitmapRequestBody)

            // ????????? ???????????? ?????????
            if (viewModel.setEditProfileImgUrl.value == true && viewModel.setEditProfileNonPic.value == false) {
                EditProfilePutService.putPicEditProfile(this, bitmapMultipartBody, myUserIdx)
            }
            //????????? ????????? ?????? X ????????? ?????????.
            else if (viewModel.setEditProfileImgUrl.value == false && viewModel.setEditProfileNonPic.value == true) {
                EditProfilePutService.putEditNonPicProfile(this, phoneNumber, nickname, birth, gender,
                    locationIdx, experienceIdx, introduction, myUserIdx)
                val intent = Intent(activity, MyProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            // ????????? ????????? ???????????? ???????????? ?????????
            else {
                EditProfilePutService.putEditNonPicProfile(this, phoneNumber, nickname, birth, gender,
                    locationIdx, experienceIdx, introduction, myUserIdx)
                EditProfilePutService.putPicEditProfile(this, bitmapMultipartBody, myUserIdx)
            }
        }

        val fragmentTransaction: FragmentManager = requireActivity().supportFragmentManager
        (context as EditProfileActivity).findViewById<ImageView>(R.id.edit_top_left_arrow_iv).setOnClickListener {
            requireActivity().finish()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("$TAG _ onDestroyView", "onDestroyView")
        savedState = saveState()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ????????? ????????? ??? ?????? ???????????? ??????
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, CAMERA_PERMISSION_REQUEST)

                } else {
                    // ?????? ?????? ??? ?????? ?????????
                    Toast.makeText(requireContext(), "????????? ????????? ?????????????????? ????????? ?????? ????????? ???????????? ?????????.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            multiplePermissionsCode2 -> {
                var startAlb = true
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        // ????????? ?????? permission ??? ?????????
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(requireContext(),
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

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    // data : Intent ?????? ?????? ????????? ?????????
                    val bitmap = data?.getParcelableExtra<Bitmap>("data")

                    profileBitmap = bitmap!!
                    viewModel.editProfileImg.value = profileBitmap
                    viewModel.setEditProfileImgUrl.value = true
                    binding.myProfileImgIv.setImageBitmap(bitmap)
                    binding.myProfileImgIv.scaleType = ImageView.ScaleType.FIT_XY
                }
            }

            multiplePermissionsCode2 -> {
                if (resultCode == Activity.RESULT_OK) {
                    // ????????? ???????????? ?????? ???????????? ???????????? Uri ????????? ??????
                    val uri = data?.data
                    if (uri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // ??????????????? 10?????? ??????
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                            var bitmap = ImageDecoder.decodeBitmap(source)
                            bitmap = resizeBitmap(1024, bitmap)
                            profileBitmap = bitmap
                            viewModel.editProfileImg.value = profileBitmap
                            viewModel.setEditProfileImgUrl.value = true
                            binding.myProfileImgIv.setImageBitmap(bitmap)
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
                                viewModel.editProfileImg.value = profileBitmap
                                viewModel.setEditProfileImgUrl.value = true
                                binding.myProfileImgIv.setImageBitmap(bitmap)

                            }
                        }
                    }
                }
            }
        }
    }


    override fun onGetEditProfileItemSuccess(getEditProfileResDto: GetEditProfileResDto) {
        val db = UserDatabase.getInstance(requireContext())
        val myProfileDB = db!!.userDao().getUser(myUserIdx) /* ?????? ??? idx??? ?????? ????????? ????????? ????????????... */
        // ?????????, ??????, ??????, ????????? ?????????, ??????
        binding.nicknameEt.hint = getEditProfileResDto.existingProfileInfo.nickname
        viewModel.editProfileNickname.value = getEditProfileResDto.existingProfileInfo.nickname
        binding.locationInfoTv.text = toLocationStr(myProfileDB.location!!)
        // ????????? API ??? ??? ????????????, Editable ????????? ??????
        inputIntroduction = getEditProfileResDto.existingProfileInfo.introduction
        binding.contentIntroductionEt.text = Editable.Factory.getInstance().newEditable(inputIntroduction)
        Glide.with(binding.myProfileImgIv.context)
            .load(getEditProfileResDto.existingProfileInfo.profileImgUrl)
            .into(binding.myProfileImgIv)
        val btnId: Int = resources.getIdentifier(
            "edit_profile_table_" + getEditProfileResDto.existingProfileInfo.experienceIdx.toString() + "_btn",
            "id",
            requireActivity().packageName
        )
        expBtn = requireView().findViewById(btnId)
        expBtn.isChecked = true

    }

    override fun onGetEditItemFailure(code: Int, message: String) {
        val db = UserDatabase.getInstance(requireContext())
        val myProfileDB = db!!.userDao().getUser(myUserIdx) /* ?????? ??? idx??? ?????? ????????? ????????? ????????????... */

        binding.nicknameEt.hint = myProfileDB.nickName
        binding.locationInfoTv.hint = toLocationStr(myProfileDB.location!!)
        inputIntroduction = myProfileDB.introduce.toString()
        binding.contentIntroductionEt.text = Editable.Factory.getInstance().newEditable(inputIntroduction)
        binding.editProfileTableLayoutTl.checkedRadioButtonId = myProfileDB.experience!!

        Toast.makeText(context, "???????????? ?????? ?????? ??? ?????? ??????????????????.", Toast.LENGTH_LONG).show()
    }


    override fun onPutEditNonPicProfileItemSuccess(
        editPutProfileResponse: EditProfilePutResponse,
        message: String
    ) {
        // DB ????????????
        val db = UserDatabase.getInstance(requireContext())
        val myProfileDB = db!!.userDao().getUser(myUserIdx)

        user = User(
            phoneNumber = myProfileDB.phoneNumber,
            nickName = viewModel.editProfileNickname.value!!,
            gender = myProfileDB.gender,
            birth = myProfileDB.birth,
            location = viewModel.editProfileLocation.value,
            experience = viewModel.editProfileExperience.value,
            profileImg = myProfileDB.profileImg,
            introduce = viewModel.editProfileIntroduce.value,
            fcmToken = myProfileDB.fcmToken,
            myUserIdx
        )
        db.userDao().update(user)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        putSuccess = true
    }

    override fun onPutEditNonPicProfileItemFailure(code: Int, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        Toast.makeText(context, "???????????? ????????? ?????? ??? ?????? ??????????????????.", Toast.LENGTH_LONG)
    }

    // ????????? ???????????? ???????????? ?????????
    private fun resizeBitmap(targetWidth: Int, source: Bitmap): Bitmap {
        // ????????? ?????? ??????
        val ratio = targetWidth.toDouble() / source.width.toDouble()
        // ????????? ?????? ?????? ?????????
        val targetHeight = (source.height * ratio).toInt()
        // ????????? ????????? bitmap ????????? ??????
        val result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false)
        return result

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(
            "savedState",
            if (savedState != null) savedState else saveState()
        )
    }

    private fun saveState(): Bundle { /* called either from onDestroyView() or onSaveInstanceState() */
        val state = Bundle()
        state.putCharSequence("save", "true")

        return state
    }

    private fun toLocationStr(index: Int): String? {
        val array = resources.getStringArray(R.array.location_full_name)
        return array[index]

    }

    @SuppressLint("ResourceAsColor", "UseCompatLoadingForDrawables")
    private fun onApplyEnable() {
        binding.activatingApplyBtn.apply {
            setTextColor(ContextCompat.getColor(mContext, R.color.white))
            background =
                requireActivity().getDrawable(R.drawable.signup_next_btn_done_rectangular)
            isEnabled = true
        }

    }

    @SuppressLint("ResourceAsColor", "UseCompatLoadingForDrawables")
    private fun onApplyDisable() {
        binding.activatingApplyBtn.apply {
            setTextColor(ContextCompat.getColor(mContext, R.color.dark_gray_B0))
            isEnabled = false
            background =
                requireActivity().getDrawable(R.drawable.signup_next_btn_rectangular)
        }
    }


    // ?????? ?????? ???!!
    override fun onGetDuplicateNicknameSuccess(duplicateNicknameResponse: DuplicateNicknameResponse) {
        viewModel.setEditProfileNickName.value = true
        viewModel.isValidNicknameEditCondition.value = true
        viewModel.setEditProfileIsDuplicated.value = true
        binding.btnCheckDuplicationTv.setBackgroundResource(R.drawable.signup_phone_verifying_rectangular)
        binding.btnCheckDuplicationTv.setTextColor(ContextCompat.getColor(mContext, R.color.dark_gray_A))
        binding.btnCheckDuplicationTv.isEnabled = false
        binding.nicknameEtField.isEndIconVisible = false
        binding.nicknameEt.isEnabled = false
        Toast.makeText(context, "????????? ?????? ?????? ??????!", Toast.LENGTH_LONG).show()
    }

    override fun onGetDuplicateNicknameFailure(code: Int, message: String) {
        viewModel.setEditProfileIsDuplicated.value = false
        binding.nickNameErrorTv.visibility = View.VISIBLE
        binding.nickNameErrorTv.text = message
        binding.nicknameCheckIconIv.visibility = View.VISIBLE
        binding.nicknameCheckIconIv.setImageResource(R.drawable.ic_signup_nickname_unable)
        binding.btnCheckDuplicationTv.setBackgroundResource(R.drawable.signup_phone_verifying_done_rectangular)
        binding.btnCheckDuplicationTv.setTextColor(ContextCompat.getColor(mContext, R.color.dark_gray_A))
        Toast.makeText(context, "???????????? ?????? ?????? ??? ?????? ??????????????????.", Toast.LENGTH_LONG).show()

    }

    fun setRadioButton(tag: Int) {
        if (tag == 0){
            expBtn.isChecked = false
        } else {
            viewModel.editProfileExperience.value = tag
            viewModel.setEditProfileExperience.value = viewModel.editProfileExperience.value != originExperience
        }
    }

    override fun onPutPicEditProfileItemSuccess(editProfilePutPicResponse: EditProfilePutPicResponse) {
//        DB??? ?????? Uri ???????????? -> RESULT ????????? uri ??? ????????????
        val db = UserDatabase.getInstance(requireContext())
        db!!.userDao().update(myUserIdx, editProfilePutPicResponse.result.profilePhotoUrl)
        val myProfileDB = db.userDao().getUser(myUserIdx) /* ?????? ??? idx??? ?????? ????????? ????????? ????????????... */
        Toast.makeText(context, editProfilePutPicResponse.message, Toast.LENGTH_LONG).show()
        putSuccess = true
        val intent = Intent(activity, MyProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)

    }

    override fun onPutPicEditProfileItemFailure(code: Int, message: String) {
        Log.d(TAG, "onPutPicEditProfileItemFailure : $message")
        Toast.makeText(context, "???????????? ?????? ?????? ??? ?????? ??????????????????.", Toast.LENGTH_LONG).show()
    }

    inner class BitmapRequestBody(private val bitmap: Bitmap) : RequestBody() {
        override fun contentType(): MediaType = "image/jpeg".toMediaType()
        override fun writeTo(sink: BufferedSink) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, sink.outputStream())
        }
    }


}