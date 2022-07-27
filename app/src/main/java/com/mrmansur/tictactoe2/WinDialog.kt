package com.mrmansur.tictactoe2

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import com.mrmansur.tictactoe2.databinding.WinDialogLayoutBinding

class WinDialog constructor(context: Context, var message : String, ) : Dialog(context) {

    private var mainActivity : MainActivity
    init {
        this.mainActivity = context as MainActivity
    }

    private lateinit var binding: WinDialogLayoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WinDialogLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        binding.messageTV.text = message

        binding.startNewBtn.setOnClickListener {
            dismiss()
            context.startActivity(Intent(context, PlayerName::class.java))
            mainActivity.finish()
        }
    }
}

