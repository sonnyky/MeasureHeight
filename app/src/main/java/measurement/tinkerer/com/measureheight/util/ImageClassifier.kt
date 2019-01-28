package measurement.tinkerer.com.measureheight.util

import android.content.res.AssetManager
import android.graphics.Bitmap
import measurement.tinkerer.com.measureheight.model.Result
import measurement.tinkerer.com.measureheight.util.Keys.DIM_BATCH_SIZE
import measurement.tinkerer.com.measureheight.util.Keys.DIM_IMG_SIZE_X
import measurement.tinkerer.com.measureheight.util.Keys.DIM_IMG_SIZE_Y
import measurement.tinkerer.com.measureheight.util.Keys.DIM_PIXEL_SIZE
import measurement.tinkerer.com.measureheight.util.Keys.INPUT_SIZE
import measurement.tinkerer.com.measureheight.util.Keys.LABEL_PATH
import measurement.tinkerer.com.measureheight.util.Keys.MAX_RESULTS
import measurement.tinkerer.com.measureheight.util.Keys.MODEL_PATH
import io.reactivex.Single
import measurement.tinkerer.com.measureheight.util.Keys.IMAGE_MEAN
import measurement.tinkerer.com.measureheight.util.Keys.IMAGE_STD
import measurement.tinkerer.com.measureheight.util.Keys.NUM_CHANNELS
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class ImageClassifier constructor(private val assetManager: AssetManager) {

    private var interpreter: Interpreter? = null
    private var labelProb: Array<ByteArray>
    private val labelProbArray: Array<Array<FloatArray>>

    // Modify to accept 4 output tensors
    private var outputLocations: FloatArray? = null
    private var outputScores: FloatArray? = null
    private var outputClasses: FloatArray? = null
    private var outputNumDetections: FloatArray? = null
    private var outputNames: Array<String>? = null

    private var logStats = false

    private val labels = Vector<String>()
    private val intValues by lazy { IntArray(INPUT_SIZE * INPUT_SIZE) }
    private var imgData: ByteBuffer

    init {
        try {
            val br = BufferedReader(InputStreamReader(assetManager.open(LABEL_PATH)))
            while (true) {
                val line = br.readLine() ?: break
                labels.add(line)
            }
            br.close()
        } catch (e: IOException) {
            throw RuntimeException("Problem reading label file!", e)
        }
        labelProbArray = Array( 1){ Array(1917) { FloatArray(4) }}
        labelProb = Array(1) { ByteArray(labels.size) }
        imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE * NUM_CHANNELS)
        imgData.order(ByteOrder.nativeOrder())
        try {
            interpreter = Interpreter(loadModelFile(assetManager, MODEL_PATH))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues!![pixel++]
                imgData!!.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData!!.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData!!.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
    }

    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun recognizeImage(bitmap: Bitmap): Single<List<Result>> {
        return Single.just(bitmap).flatMap {
            convertBitmapToByteBuffer(it)
            interpreter!!.run(imgData, labelProbArray)
            val pq = PriorityQueue<Result>(3,
                Comparator<Result> { lhs, rhs ->
                    // Intentionally reversed to put high confidence at the head of the queue.
                    java.lang.Float.compare(rhs.confidence!!, lhs.confidence!!)
                })
            for (i in labels.indices) {

                pq.add(Result("" + i, if (labels.size > i) labels[i] else "unknown", labelProbArray[0][i][2], null))
            }
            val recognitions = ArrayList<Result>()
            val recognitionsSize = Math.min(pq.size, MAX_RESULTS)
            for (i in 0 until recognitionsSize) recognitions.add(pq.poll())
            return@flatMap Single.just(recognitions)
        }
    }

    fun close() {
        interpreter?.close()
    }
}
