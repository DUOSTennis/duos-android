package com.example.duos.ui.main.mypage.myprofile.frag

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.example.duos.R
import com.example.duos.databinding.FragmentEditProfileBinding
import com.example.duos.ui.BaseFragment
import java.io.File

class EditProfileFragment : BaseFragment<FragmentEditProfileBinding>(FragmentEditProfileBinding::inflate) {

    lateinit var contentUri: Uri

    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val CAMERA_PERMISSION_REQUEST = 100

    // 멀티퍼미션 카메라1
    val requiredPermissions1 = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
    )
    val multiplePermissionsCode1 = 200

    // 멀티퍼미션 갤러리(앨범)2
    val permission_list = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )
    val multiplePermissionsCode2 = 300

    override fun initAfterBinding() {

        for (i in 1..14) {
            var btnId: Int = resources.getIdentifier("edit_profile_table_" + i.toString() + "_btn", "id", requireActivity().packageName)

            var btn: Button = requireView().findViewById(btnId)
            val num: String = i.toString()

            btn.text = resources.getString(resources.getIdentifier("signup_length_of_play_$num", "string", requireActivity().packageName))

        }
        val file_path = requireActivity().getExternalFilesDir(null).toString()

        binding.myProfileImgIv.setOnClickListener {

            val dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setTitle(R.string.upload_pic_dialog_title)
                // setItems 대신 setAdapter()를 사용하여 목록을 지정 가능
                // 이렇게 하면 동적 데이터가 있는 목록(예: 데이터베이스에서 가져온 것을 ListAdapter로 지원할 수 있다.)
                .setItems(R.array.upload_pic_dialog_title, DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        // 카메라 0 썸네일로
                        0 -> {
                            var permissionResult0 = ContextCompat.checkSelfPermission(requireContext(), CAMERA_PERMISSION[0])
                            Log.d("Signup_Image_Upload_Dialog", "checkSelfPermission$which")

                            when (permissionResult0) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    Log.d("Signup_Image_Upload_Dialog", "PERMISSION_GRANTED$which")
                                    // 카메라 권한이 이미 허용된 상태일 때 바로 카메라 액티비티 호출
                                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    startActivityForResult(intent, CAMERA_PERMISSION_REQUEST)
                                }
                                PackageManager.PERMISSION_DENIED -> {
                                    Log.d("Signup_Image_Upload_Dialog", "PERMISSION_DENIED$which")
                                    // 카메라 권한이 허용된 상태가 아닐 때
                                    // ActivityCompat.requestPermissions(requireActivity(), CAMERA_PERMISSION, CAMERA_PERMISSION_REQUEST)
                                    // Fragment에서 onRequestPermissionsResult 호출하려면 requestPermissions만 쓰기
                                    requestPermissions(CAMERA_PERMISSION, CAMERA_PERMISSION_REQUEST)
                                    // 이 떄 onRequestPermissionsResult 메소드 호출

                                }
                            }

                        }

                        // 사진 1 -> 갤러리에 저장 함 (용량 큰). 이미지의 용량이 너무 크면 서버와 송수신할 때 데이터를 너무 많이 써서 용량 줄이는 작업 ㄱ,  사진 보정 가능
                        1 -> {

                            // 거부 Or 아직 수락하지 않은 퍼미션을 저장할 String 배열리스트
                            val rejectedPermissionList = ArrayList<String>()
                            // 필요한 퍼미션들이 현재 권한을 받았는지 체크
                            for (permission in requiredPermissions1) {
                                if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                                    //만약 권한이 없다면 rejectedPermissionList에 추가
                                    rejectedPermissionList.add(permission)
                                }
                            }

                            if (rejectedPermissionList.isNotEmpty()) {   // 거절된 퍼미션 있다면?
                                val array = arrayOfNulls<String>(rejectedPermissionList.size)
                                requestPermissions(rejectedPermissionList.toArray(array), multiplePermissionsCode1)
                            } else {// 모두 허용된 퍼미션이라면
                                Log.d("Signup_Image_Upload_Dialog", "PERMISSION_GRANTED$which")
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                                // 촬영한 사진이 저장될 파일 이름
                                val file_name = "/temp_${System.currentTimeMillis()}.jpg"
                                // 경로 + 파일 이름
                                val pic_path = "$file_path/$file_name"
                                val file = File(pic_path)

                                // 사진이 저장될 위치를 관리하는 Uri 객체
                                // val contentUri = Uri(pic_path) // 예전에는 파일명을 기술하면 바로 접근 가능
                                // -> 현재 안드로이드 OS 6.0 부터는 OS에서 해당 경로를 집어 넣으면 이 경로로 접근할 수 있는지 없는지를 판단. 접근할 수 있으면 Uri 객체를 넘겨줌.
                                contentUri = FileProvider.getUriForFile(requireContext(), "com.duos.camera.file_provider", file)

                                intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
                                startActivityForResult(intent, multiplePermissionsCode1)
                            }
                        }
                        // 내 앨범에서 선택
                        2 -> {
                            Log.d("Signup_Image_Upload_Dialog", "파일 접근 $which")
                            val rejectedPermissionList = ArrayList<String>()
                            // 필요한 퍼미션들이 현재 권한을 받았는지 체크
                            for (permission in permission_list) {
                                if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                                    //만약 권한이 없다면 rejectedPermissionList에 추가
                                    rejectedPermissionList.add(permission)
                                }
                            }
                            if (rejectedPermissionList.isNotEmpty()) {   // 거절된 퍼미션 있다면?
                                val array = arrayOfNulls<String>(rejectedPermissionList.size)
                                requestPermissions(rejectedPermissionList.toArray(array), multiplePermissionsCode2)
                            } else {

                                // 앨범에서 사진을 선택할 수 있도록 Intent
                                val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                // 실행할 액티비티의 타입 설정(이미지를 선택할 수 있는 것)
                                albumIntent.type = "image/*"
                                // 선택할 파일의 타입 지정
                                val mimeType = arrayOf("image/*")
                                albumIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
                                startActivityForResult(albumIntent, multiplePermissionsCode2)
                            }
                        }
                    }
                })
            dialogBuilder.create().show()

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d("Signup_Image_Upload_Dialog", "OnRequestPermissionsResult 호출댐.")
        val file_path = requireActivity().getExternalFilesDir(null).toString()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 허용을 눌렀을 때 바로 카메라로 ㄱㄱ
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, CAMERA_PERMISSION_REQUEST)

                } else {
                    // 권한 거부 시 로그 띄우기
                    Log.d("Signup_Image_Upload_Dialog", "OnRequestPermissionsResult에서 카메라0 권한 거부.")
                    Toast.makeText(requireContext(), "프로필 사진을 업로드하려면 카메라 접근 권한을 허용해야 합니다.", Toast.LENGTH_LONG).show()

                }
            }

            multiplePermissionsCode1 -> {
                var startCam = true
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        // 권한이 없는 permission이 있다면
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d("Signup", "사용하려면 권한 체크 해야되")
                            Toast.makeText(requireContext(), "프로필 사진을 업로드하려면 카메라 접근 권한을 허용해야 합니다.", Toast.LENGTH_LONG).show()
                            startCam = false
                        }
                    }
                    if (startCam) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                        // 촬영한 사진이 저장될 파일 이름
                        val file_name = "/temp_${System.currentTimeMillis()}.jpg"
                        // 경로 + 파일 이름
                        val pic_path = "$file_path/$file_name"
                        val file = File(pic_path)

                        // 사진이 저장될 위치를 관리하는 Uri 객체
                        // val contentUri = Uri(pic_path) // 예전에는 파일명을 기술하면 바로 접근 가능
                        // -> 현재 안드로이드 OS 6.0 부터는 OS에서 해당 경로를 집어 넣으면 이 경로로 접근할 수 있는지 없는지를 판단. 접근할 수 있으면 Uri 객체를 넘겨줌.
                        contentUri = FileProvider.getUriForFile(requireContext(), "com.duos.camera.file_provider", file)

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
                        startActivityForResult(intent, 200)
                    }
                }
            }
            multiplePermissionsCode2 -> {
                var startAlb = true
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        // 권한이 없는 permission이 있다면
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d("Signup", "사용하려면 권한 체크 해야되")
                            Toast.makeText(requireContext(), "프로필 사진을 업로드하려면 카메라 접근 권한을 허용해야 합니다.", Toast.LENGTH_LONG).show()
                            startAlb = false
                        }
                    }
                    if (startAlb) {
                        // 앨범에서 사진을 선택할 수 있도록 Intent
                        val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        // 실행할 액티비티의 타입 설정(이미지를 선택할 수 있는 것)
                        albumIntent.type = "image/*"
                        // 선택할 파일의 타입 지정
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
                if (resultCode == Activity.RESULT_OK) {
                    // data : Intent 안에 사진 정보가 들어감
                    val bitmap = data?.getParcelableExtra<Bitmap>("data")
                    binding.myProfileImgIv.setImageBitmap(bitmap)
                    binding.myProfileImgIv.scaleType = ImageView.ScaleType.FIT_XY
                }
            }

            multiplePermissionsCode1 -> {
                if (resultCode == Activity.RESULT_OK) {
                    val bitmap = BitmapFactory.decodeFile(contentUri.path)
                    // 사진 조정 된것
                    val degree = getDegree(contentUri, contentUri.path!!)   // contentUri 는 안드로이드 10버전 이상, contentUri.path!! 는 9버전 이하를 위해 넣음
                    val bitmap2 = resizeBitmap(1024, bitmap)
                    val bitmap3 = rotateBitmap(bitmap2, degree)

                    binding.myProfileImgIv.setImageBitmap(bitmap3)


//                    // 사진 파일 삭제한다.
//                    val file = File(contentUri.path)
//                    file.delete()

                }
            }
            multiplePermissionsCode2 -> {
                if (resultCode == Activity.RESULT_OK) {
                    // 선택한 이미지의 경로 데이터를 관리하는 Uri 객체를 추출

                    val uri = data?.data

                    if (uri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // 안드로이드 10버전 이상
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                            val bitmap = ImageDecoder.decodeBitmap(source)
                            binding.myProfileImgIv.setImageBitmap(bitmap)
                        } else {
                            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
                            if (cursor != null) {
                                cursor.moveToNext()
                                // 이미지 경로를 가져온다.
                                val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                                val source = cursor.getString(index)
                                // 이미지 생성
                                val bitmap = BitmapFactory.decodeFile(source)
                                binding.myProfileImgIv.setImageBitmap(bitmap)
                            }
                        }
                    }
                }


            }

        }

    }

    // 사진의 사이즈를 조정하는 메서드
    fun resizeBitmap(targetWidth: Int, source: Bitmap): Bitmap {
        // 이미지 비율 계산
        val ratio = targetWidth.toDouble() / source.width.toDouble()
        // 보정될 세로 길이 구하기
        var targetHeight = (source.height * ratio).toInt()
        // 크기를 조정한 bitmap 객체를 생성
        val result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false)
        return result

    }

    // 이미지의 회전 각도값을 구하기
    // 11버전 이상부터 달라짐 (외부저장소 보안 때문에)
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
        var ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)   // 만약 회전값이 저장이 안되어 있으면 default값으로 -1 넣기 (0 넣으면 안댐)
        when (ori) {
            ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
        }
        return degree.toFloat()
    }

    // 사진 돌리기
    fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {

        // 각도값을 관리하는 객체
        val matrix = Matrix()
        matrix.postRotate(degree)
        // 회전된 이미지를 받아온다.
        val bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return bitmap2
    }


}