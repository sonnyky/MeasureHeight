package measurement.tinkerer.com.measureheight.util

object Keys {
    //    const val MODEL_PATH = "tensorflow_inception_graph.pb"
    // const val MODEL_PATH = "mobilenet_quant_v1_224.tflite"
    // const val LABEL_PATH = "labels.txt"
    const val MODEL_PATH = "mobilenet_ssd.tflite"
    const val LABEL_PATH = "coco_labels_list.txt"
    const val INPUT_NAME = "input"
    const val OUTPUT_NAME = "output"
    const val IMAGE_MEAN: Int = 0
    const val IMAGE_STD: Float = 0.toFloat()
    const val INPUT_SIZE = 300
    const val MAX_RESULTS = 3
    const val DIM_BATCH_SIZE = 1
    const val DIM_PIXEL_SIZE = 3
    const val DIM_IMG_SIZE_X = 300
    const val DIM_IMG_SIZE_Y = 300

}