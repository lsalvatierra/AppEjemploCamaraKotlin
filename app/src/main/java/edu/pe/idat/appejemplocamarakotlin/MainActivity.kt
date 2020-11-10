package edu.pe.idat.appejemplocamarakotlin

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private val CAMARA_REQUEST = 1888
    var mRutaFotoActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btntomarfoto.setOnClickListener {
            if(permisoEscrituraAlmacenamiento()){
                try {
                    intencionTomarFoto()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }else{
                solicitarPermiso()
            }
        }
        this.btncompartir.setOnClickListener {
            if(mRutaFotoActual != ""){
                //val uri = Uri.parse(mRutaFotoActual)
                val contentUri = FileProvider.getUriForFile(
                        applicationContext,
                        "edu.pe.idat.appejemplocamarakotlin.provider", File(mRutaFotoActual))
                //val contentUri = Uri.fromFile(File(mRutaFotoActual))
                // Create the text message with a string
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "image/jpeg"
                }
                val chooser: Intent = Intent.createChooser(sendIntent, "Compartir Imagen")
                // Verify that the intent will resolve to an activity
                if (sendIntent.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                }
            }else{
                Toast.makeText(applicationContext, "Debe seleccionar una imagen para compartirlo",
                        Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun permisoEscrituraAlmacenamiento(): Boolean{
        val result = ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        var exito = false
        if (result == PackageManager.PERMISSION_GRANTED) exito = true
        return exito
    }

    private fun solicitarPermiso(){
        ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if(requestCode == 0){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                intencionTomarFoto()
            }else{
                Toast.makeText(applicationContext, "Permiso Denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Crear el método donde guardar la imagen
    //Este método crea una Excepción por que puede devolver NULL
    @Throws(IOException::class)
    private fun crearArchivoImagen(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File = this?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val image: File = File.createTempFile(imageFileName, ".jpg", storageDir)
        mRutaFotoActual = image.absolutePath
        return image
    }

    //Llamamos a la cámara mediante un Intent implícito.
    @Throws(IOException::class)
    private fun intencionTomarFoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //Validamos que el dispositivo tiene la aplicación de la cámara.
        if (takePictureIntent.resolveActivity(this?.packageManager!!) != null) {
            val photoFile = crearArchivoImagen()
            if (photoFile != null) {
                //creamos una URI para para el archivo
                val photoURI: Uri = FileProvider.getUriForFile(
                        applicationContext,
                        "edu.pe.idat.appejemplocamarakotlin.provider", photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMARA_REQUEST)
            }
        }
    }

    //Llamamos a la cámara utilizando Intent implícito.
    private fun mostrarFoto() {
        val ei = ExifInterface(mRutaFotoActual)
        val orientation: Int = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
        )
        if(orientation == ExifInterface.ORIENTATION_ROTATE_90){
            ivfoto.rotation = 90.0F
        }else{
            ivfoto.rotation = 0.0F
        }
        val targetW: Int = ivfoto.width
        val targetH: Int = ivfoto.height
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mRutaFotoActual, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight
        val scaleFactor = min(photoW / targetW, photoH / targetH)
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(mRutaFotoActual, bmOptions)
        ivfoto.setImageBitmap(bitmap)

    }
    //Grabar Foto en la galeria del dispositivo.
    private fun grabarFotoGaleria() {
        val mediaScanIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val nuevoarchivo = File(mRutaFotoActual)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            val contentUri = FileProvider.getUriForFile(
                    applicationContext,
                    "edu.pe.idat.appejemplocamarakotlin.provider", nuevoarchivo)
            mediaScanIntent.data = contentUri
        }else{
            val contentUri = Uri.fromFile(nuevoarchivo)
            mediaScanIntent.data = contentUri
        }
        this?.sendBroadcast(mediaScanIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CAMARA_REQUEST){
            if(resultCode == Activity.RESULT_OK){
                grabarFotoGaleria()
                mostrarFoto()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }
}