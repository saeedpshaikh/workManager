package com.example.workmangerdemopoc


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.work.*
import com.example.workmangerdemopoc.ui.theme.WorkMangerDemoPocTheme
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    companion object {
        const val KEY_COUNT_VALUE = "key_count"
    }
    private val text = mutableStateOf("")
    private lateinit var workManagerOneTime: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManagerOneTime = WorkManager.getInstance(applicationContext)

        setContent {
            WorkMangerDemoPocTheme {
                Column(Modifier.fillMaxSize()) {
                    addButton("OneTime Request")
                    addButton("Periodic Request")
                    addButton("Chaining Request")
                    addButton("Parallel Request")
                    addButton("Cancel Request")
                    AddTextView()

                }
            }
        }
    }


    @Composable
    fun addButton(nameOfAction: String) {
        Button(onClick = {

            when (nameOfAction) {
                "OneTime Request" -> {
                    setOneTimeWorkRequest()
                }
                "Periodic Request" -> {
                    setPeriodicWorkRequest()
                }
                "Chaining Request" -> {
                    setChainingWorkRequest()
                }
                "Parallel Request" -> {
                    setChainingAndParallelWorkRequest()
                }
                "Cancel Request" -> {
                    cancelWorkManager()
                }
            }
        },
            Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(text = nameOfAction)
        }
    }

    @Composable
    fun AddTextView() {
        Text(text = "", Modifier.fillMaxSize(), textAlign = TextAlign.Center, )
    }

    /**
     * Work Manager OneTime Request
     * */

    private fun setOneTimeWorkRequest() {

        val workManager = WorkManager.getInstance(applicationContext)

        val data: Data = Data.Builder()
            .putInt(KEY_COUNT_VALUE, 5)
            .build()
        val constraints = Constraints.Builder()
          //  .setRequiresCharging(true)
            // .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()



        val uploadRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
          /*  .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS)*/
            .setConstraints(constraints)
            .setInputData(data)
            .build()



        workManagerOneTime.enqueue(uploadRequest)

      /*  workManagerOneTime.enqueueUniqueWork(
            "OneTimeRequest",
            ExistingWorkPolicy.REPLACE,
            uploadRequest
        )
*/


        workManagerOneTime.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this, Observer {
                text.value = it.state.toString()
                Log.d("====one", it.state.toString())

                if (it.state.isFinished) {
                    text.value = it.state.toString()

                    val data = it.outputData
                    val message: String? = data.getString(UploadWorker.KEY_WORKER)
                 //   Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            })
        //workManagerOneTime.cancelWorkById(uploadRequest.id)


    }


    /**
     * Work Manager Periodic Request
     * */

    private fun setPeriodicWorkRequest() {

        val workManager = WorkManager.getInstance(applicationContext)

        val constraints = Constraints.Builder()
            //  .setRequiresCharging(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest
            .Builder(DownloadingWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this, Observer {
                text.value = it.state.toString()
                Log.d("====", it.state.toString())
                if (it.state.isFinished) {
                    text.value = it.state.toString()

                    val data = it.outputData
                    val message: String? = data.getString(UploadWorker.KEY_WORKER)
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            })
    }


    private fun setChainingAndParallelWorkRequest() {

        val workManager = WorkManager.getInstance(applicationContext)

        val data: Data = Data.Builder()
            .putInt(KEY_COUNT_VALUE, 5)
            .build()
        val constraints = Constraints.Builder()
            //  .setRequiresCharging(true)
            //  .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        val filteringRequest = OneTimeWorkRequest.Builder(FilteringWorker::class.java)
            .build()
        val compressingRequest = OneTimeWorkRequest.Builder(CompressingWorker::class.java)
            .build()
        val downloadingWorker = OneTimeWorkRequest.Builder(DownloadingWorker::class.java)
            .build()

        //ParallelWorks Request.

        val parallelWorks = mutableListOf<OneTimeWorkRequest>()
        parallelWorks.add(downloadingWorker)
        parallelWorks.add(filteringRequest)
        workManager
            .beginWith(parallelWorks)
            .then(compressingRequest)
            .then(uploadRequest)
            .enqueue()

        WorkManager.getInstance().enqueue(parallelWorks)


        workManager.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this, Observer {
                Log.d("====Chaining", it.state.toString())

                if (it.state.isFinished) {
                    val data = it.outputData
                    val message: String? = data.getString(UploadWorker.KEY_WORKER)
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            })
    }



    private fun setChainingWorkRequest() {

        val workManager = WorkManager.getInstance(applicationContext)

        val data: Data = Data.Builder()
            .putInt(KEY_COUNT_VALUE, 5)
            .build()
        val constraints = Constraints.Builder()
            //  .setRequiresCharging(true)
            //  .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        val filteringRequest = OneTimeWorkRequest.Builder(FilteringWorker::class.java)
            .build()
        val compressingRequest = OneTimeWorkRequest.Builder(CompressingWorker::class.java)
            .build()
        val downloadingWorker = OneTimeWorkRequest.Builder(DownloadingWorker::class.java)
            .build()

        //ParallelWorks Request.

        val parallelWorks = mutableListOf<OneTimeWorkRequest>()
        parallelWorks.add(downloadingWorker)
        workManager
            .beginWith(parallelWorks)
            .then(filteringRequest)
            .then(compressingRequest)
            .then(uploadRequest)
            .enqueue()

        WorkManager.getInstance().enqueue(parallelWorks)


        workManager.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this, Observer {
                Log.d("====Chaining", it.state.toString())

                if (it.state.isFinished) {
                    val data = it.outputData
                    val message: String? = data.getString(UploadWorker.KEY_WORKER)
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun cancelWorkManager() {

        val data: Data = Data.Builder()
            .putInt(KEY_COUNT_VALUE, 10000)
            .build()
        val constraints = Constraints.Builder()
            .build()

        val uploadRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .setConstraints(constraints)
            .setInputData(data)
            .build()



        workManagerOneTime.enqueue(uploadRequest)


        workManagerOneTime.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this, Observer {
                text.value = it.state.toString()
                Log.d("====one", it.state.toString())

                if (it.state.isFinished) {
                    text.value = it.state.toString()

                    val data = it.outputData
                    val message: String? = data.getString(UploadWorker.KEY_WORKER)
                    //   Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                }
            })
        workManagerOneTime.cancelWorkById(uploadRequest.id)


    }
}