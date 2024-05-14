package com.example.chronometer

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class MainActivityAndroidViewModel(val app:Application):AndroidViewModel(app) {

    @Volatile private var label = 0
    private val blockingQueue = LinkedBlockingQueue<Int>()
    private var toastMessage = ""

    private val _labelLiveData = MutableLiveData(label)
    val labelLiveData: LiveData<Int> = _labelLiveData

    private val executorService = Executors.newFixedThreadPool(3)

    init{
        startCounting()
    }

    private fun startCounting(){

        class Counter: Thread(){
            override fun run() {
                try {
                    while (true) {
                        val newLabel = label + 1
                        blockingQueue.put(newLabel)
                        Thread.sleep(1000)
                    }
                } catch(e: InterruptedException){
                    Thread.currentThread().interrupt()
                }
            }
        }
        val counter = Counter()

        class ToastShowing: Thread(){
            override fun run() {
                try {
                    while (true) {
                        if ((label != 0) && (label % 10 == 0)) {
                            showToastMessage()
                            Thread.sleep(2000) // Wait another label's iteration
                        }
                    }
                } catch(e: InterruptedException){
                    Thread.currentThread().interrupt()
                }
            }
        }
        val toastShowing = ToastShowing()

        class ToastVerification: Thread(){
            override fun run() {
                try {
                    while (true) {
                        if (blockingQueue.remainingCapacity() > 0) {

                            val element = blockingQueue.take()
                            verifyTextToastMessage(element)
                            label = element
                            _labelLiveData.postValue(label)

                        }
                    }
                } catch (e: InterruptedException){
                    Thread.currentThread().interrupt()
                }
            }
        }
        val toastVerification = ToastVerification()

        executorService.execute(counter)
        executorService.execute(toastVerification)
        executorService.execute(toastShowing)
    }

    private fun showToastMessage(){
        Handler(Looper.getMainLooper()).post(Runnable {
            Toast.makeText(app, toastMessage, Toast.LENGTH_SHORT).show()
        })
    }

    private fun verifyTextToastMessage(value: Int){
        toastMessage = if(value % 40 != 0)
            value.toString()
        else
            app.getString(R.string.surprise)
    }

    override fun onCleared() {
        super.onCleared()
        executorService.shutdown()
    }
}



